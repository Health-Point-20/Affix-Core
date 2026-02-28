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
import net.yixi_xun.affix_core.ACConfig;
import net.yixi_xun.affix_core.affix.AffixContext;

import java.util.Comparator;
import java.util.List;

import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluate;
import static net.yixi_xun.affix_core.api.HurtManager.extraHurt;

/**
 * 伤害操作，对目标造成伤害
 */
public class DealDamageOperation extends BaseOperation {

    private final String amountExpression;
    private final String damageTypeId;
    private final boolean isExtraDamage;
    private final String target;
    private final String sourceEntity;
    private final boolean isAreaDamage;
    private final String maxEntitiesExpression;
    private final String rangeExpression;

    public DealDamageOperation(String amountExpression, String damageTypeId, String isExtraDamage, 
                              String targetString, String sourceEntity, String isAreaDamage, 
                              String maxEntitiesExpression, String rangeExpression) {
        this.amountExpression = amountExpression != null ? amountExpression : "1";
        this.damageTypeId = damageTypeId != null ? damageTypeId : "minecraft:magic";
        this.isExtraDamage = Boolean.parseBoolean(isExtraDamage);
        this.sourceEntity = sourceEntity != null ? sourceEntity : "";
        this.isAreaDamage = Boolean.parseBoolean(isAreaDamage);
        this.target = targetString != null ? targetString : "target";
        this.maxEntitiesExpression = maxEntitiesExpression != null ? maxEntitiesExpression : "0";
        this.rangeExpression = rangeExpression != null ? rangeExpression : "0";
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }
        
        LivingEntity target = getTargetEntity(context, this.target);
        LivingEntity attacker;
        if (sourceEntity.isEmpty() || sourceEntity.equals("null")) {
            attacker = null;    //无来源伤害
        } else {
            attacker = getTargetEntity(context, sourceEntity);
        }
        
        if (isInValidEntity(target)) {
            return;
        }

        ResourceKey<DamageType> type = getDamageTypeKey(damageTypeId);
        DamageSource source = createDamageSource(context, attacker, type);
        float finalAmount = (float) evaluateOrDefaultValue(amountExpression, context.getVariables(), 1.0);

        // 应用主目标伤害
        if (isExtraDamage) {
            extraHurt(target, source, finalAmount);
        } else {
            target.hurt(source, finalAmount);
        }

        // 处理范围伤害
        if (isAreaDamage && isValidAreaDamageParams()) {
            applyAreaDamage(context, target, source, finalAmount);
        }
    }

    /**
     * 验证范围伤害参数是否有效
     */
    private boolean isValidAreaDamageParams() {
        return maxEntitiesExpression != null && !maxEntitiesExpression.isEmpty() &&
               rangeExpression != null && !rangeExpression.isEmpty();
    }

    /**
     * 创建伤害源
     */
    private DamageSource createDamageSource(AffixContext context,
                                          LivingEntity attacker, ResourceKey<DamageType> type) {
        if (context.getEvent() instanceof LivingHurtEvent event && "damage_type".equals(damageTypeId)) {
            return event.getSource();
        } else {
            return new DamageSource(attacker.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(type),
                    attacker);
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
        nbt.putString("IsExtraDamage", String.valueOf(isExtraDamage));
        // 范围伤害字段
        nbt.putString("IsAreaDamage", String.valueOf(isAreaDamage));
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
        String amountExpression = getString(nbt, "AmountExpression", "1");
        String damageTypeId = getString(nbt, "DamageTypeId", "minecraft:magic");
        String target = getString(nbt, "Target", "target");
        String isExtraDamage = getString(nbt, "IsExtraDamage", "true");
        String sourceEntity = getString(nbt, "SourceEntity", "");
        
        // 范围伤害字段
        String isAreaDamage = getString(nbt, "IsAreaDamage", "false");
        String maxEntitiesExpression = getString(nbt, "MaxEntitiesExpression", "0");
        String rangeExpression = getString(nbt, "RangeExpression", "0");

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
    
    // 为0则使用配置中的最大值
    if (maxEntities < 0 || range < 0) {
        return; // 无效的范围参数
    }
    
    // 应用配置限制：当范围为0时，使用配置的最大范围
    double effectiveRange = range > 0 ? range : ACConfig.MAX_AREA_DAMAGE_RANGE.get();
    int effectiveMaxEntities = maxEntities > 0 ? maxEntities : ACConfig.MAX_AREA_DAMAGE_ENTITIES.get();
    
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
        if (location != null && !location.getPath().isEmpty()) {
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