package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.yixi_xun.affix_core.AFConfig;
import net.yixi_xun.affix_core.affix.AffixContext;

import java.util.Comparator;
import java.util.List;

import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluate;
import static net.yixi_xun.affix_core.api.HurtManager.extraHurt;

/**
 * 伤害操作，对目标造成伤害
 */
public class DealDamageOperation implements IOperation {

    private final String amountExpression;       // 伤害值表达式
    private final String damageTypeId;           // 伤害类型
    private final String isExtraDamage;          // 是否为额外伤害(不递归，不影响受击无敌时间)
    private final String target;                 // 目标实体
    private final String sourceEntity;           // 伤害源实体
    private final String isAreaDamage;
    private final String maxEntitiesExpression;  // 范围伤害最大实体数表达式
    private final String rangeExpression;        // 范围伤害半径表达式

    public DealDamageOperation(String amountExpression, String damageTypeId, String isExtraDamage, String targetString, String sourceEntity, String isAreaDamage, String maxEntitiesExpression, String rangeExpression) {
        this.amountExpression = amountExpression;
        this.damageTypeId = damageTypeId;
        this.isExtraDamage = isExtraDamage;
        this.sourceEntity = sourceEntity;
        this.isAreaDamage = isAreaDamage;
        this.target = targetString;
        this.maxEntitiesExpression = maxEntitiesExpression;
        this.rangeExpression = rangeExpression;
    }

    @Override
    public void apply(AffixContext context) {
        LivingEntity target = this.target.equals("self") ? context.getOwner() : context.getTarget();
        LivingEntity attacker = sourceEntity.equals("self") ? context.getOwner() : context.getTarget();
        if (target == null) {
            return;
        }

        // 创建伤害源 - 支持任意注册的伤害类型
        ResourceKey<DamageType> type = getDamageTypeKey(damageTypeId);

        DamageSource source;
        if (context.getEvent() instanceof LivingHurtEvent event && damageTypeId.equals("damage_type")) {
            source = event.getSource();
        } else {
            source = new DamageSource(target.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(type),
                    attacker);
        }

        float finalAmount = (float) evaluate(amountExpression, context.getVariables());

        // 应用主目标伤害
        if (isExtraDamage.equals("true")) {
            extraHurt(target, source, finalAmount);
        } else {
            target.hurt(source, finalAmount);
        }

        // 处理范围伤害
        if (isAreaDamage.equals("true") && maxEntitiesExpression != null && !maxEntitiesExpression.isEmpty() &&
            rangeExpression != null && !rangeExpression.isEmpty()) {
            applyAreaDamage(context, target, source, finalAmount);
        }
    }

    @Override
    public void remove(AffixContext context) {
        // 不需要移除
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("Target", target);
        nbt.putString("SourceEntity", sourceEntity);
        nbt.putString("AmountExpression", amountExpression);
        nbt.putString("DamageTypeId", damageTypeId);
        nbt.putString("IsExtraDamage", isExtraDamage);
        // 范围伤害字段
        nbt.putString("IsAreaDamage", isAreaDamage);
        if (maxEntitiesExpression != null) {
            nbt.putString("MaxEntitiesExpression", maxEntitiesExpression);
        }
        if (rangeExpression != null) {
            nbt.putString("RangeExpression", rangeExpression);
        }

        
        return nbt;
    }

    @Override
    public String getType() {
        return "deal_damage";
    }

    /**
     * 工厂方法，从NBT创建DamageOperation
     */
    public static DealDamageOperation fromNBT(CompoundTag nbt) {
        String amountExpression = nbt.contains("AmountExpression") ? nbt.getString("AmountExpression") : "1";
        String damageTypeId = nbt.contains("DamageTypeId") ? nbt.getString("DamageTypeId") : "minecraft:magic";
        String target = nbt.contains("Target") ? nbt.getString("Target") : "target";
        String isExtraDamage = nbt.contains("IsExtraDamage") ? nbt.getString("IsExtraDamage") : "true";
        String sourceEntity = nbt.contains("SourceEntity") ? nbt.getString("SourceEntity") : "self";
        
        // 范围伤害字段（可选）
        String isAreaDamage = nbt.contains("IsAreaDamage") ? nbt.getString("IsAreaDamage") : "false";
        String maxEntitiesExpression = nbt.contains("MaxEntitiesExpression") ? nbt.getString("MaxEntitiesExpression") : "0";
        String rangeExpression = nbt.contains("RangeExpression") ? nbt.getString("RangeExpression") : "0";

        return new DealDamageOperation(amountExpression, damageTypeId, isExtraDamage, target, sourceEntity, isAreaDamage, maxEntitiesExpression, rangeExpression);
    }

    /**
     * 应用范围伤害
     * @param context 词缀上下文
     * @param centerEntity 中心实体（范围伤害的中心点）
     * @param source 伤害源
     * @param amount 伤害值
     */
private void applyAreaDamage(AffixContext context, LivingEntity centerEntity, DamageSource source, float amount) {
    // 计算范围伤害参数
    int maxEntities = (int) evaluate(maxEntitiesExpression, context.getVariables());
    double range = evaluate(rangeExpression, context.getVariables());
    
    // 为0视为无限制，但使用配置中的最大值来避免性能问题
    if (maxEntities < 0 || range < 0) {
        return; // 无效的范围参数
    }
    
    // 应用配置限制：当范围为0或负数时，使用配置的最大范围
    double effectiveRange = range > 0 ? range : AFConfig.MAX_AREA_DAMAGE_RANGE.get();
    int effectiveMaxEntities = maxEntities > 0 ? maxEntities : AFConfig.MAX_AREA_DAMAGE_ENTITIES.get();
    
    // 获取范围内的实体并按距离排序
    List<LivingEntity> nearbyEntities = centerEntity.level().getEntitiesOfClass(
        LivingEntity.class,
        centerEntity.getBoundingBox().inflate(effectiveRange),
        entity -> {
            // 排除中心实体本身和已经死亡的实体
            boolean inRange = entity.distanceToSqr(centerEntity) <= effectiveRange * effectiveRange;
            return entity != centerEntity && entity.isAlive() && inRange;
        }
    ).stream()
     .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(centerEntity)))
     .limit(effectiveMaxEntities)
     .toList();
    
    // 对范围内的实体造成额外伤害
    for (LivingEntity entity : nearbyEntities) {
        extraHurt(entity, source, amount);
    }
}
    
    /**
     * 获取伤害类型ResourceKey
     */
    private static ResourceKey<DamageType> getDamageTypeKey(String damageTypeId) {
        if (damageTypeId.isEmpty()) {
            return DamageTypes.MAGIC;
        }
        // 尝试解析为ResourceLocation
        ResourceLocation location = ResourceLocation.tryParse(damageTypeId);
        if (location != null) {
            return ResourceKey.create(Registries.DAMAGE_TYPE, location);
        }
        
        // 如果解析失败，使用默认的magic伤害
        return DamageTypes.MAGIC;
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("deal_damage", DealDamageOperation::fromNBT);
    }
}