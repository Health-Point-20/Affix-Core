package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.yixi_xun.affix_core.affix.AffixContext;

import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluate;
import static net.yixi_xun.affix_core.api.HurtManager.extraHurt;

/**
 * 伤害操作，对目标造成伤害
 */
public class DealDamageOperation implements IOperation {

    private final String amountExpression;
    private final String damageType;
    private final String isExtraDamage;
    private final String target;
    private final String sourceEntity;


    public DealDamageOperation(String amountExpression, String damageType, String isExtraDamage, String targetString, String sourceEntity) {
        this.amountExpression = amountExpression;
        this.damageType = damageType;
        this.isExtraDamage = isExtraDamage;
        this.sourceEntity = sourceEntity;
        this.target = targetString;
    }

    @Override
    public void apply(AffixContext context) {
        LivingEntity target = this.target.equals("self") ? context.getOwner() : context.getTarget();
        LivingEntity attacker = sourceEntity.equals("self") ? context.getOwner() : context.getTarget();
        if (target == null) {
            return;
        }

        // 创建伤害源
        ResourceKey<DamageType> type = switch (damageType) {
            case "magic" -> DamageTypes.MAGIC;
            case "fire" -> DamageTypes.IN_FIRE;
            case "freeze" -> DamageTypes.FREEZE;
            case "lightning" -> DamageTypes.LIGHTNING_BOLT;
            case "fall" -> DamageTypes.FALL;
            case "drown" -> DamageTypes.DROWN;
            case "starve" -> DamageTypes.STARVE;
            case "wither" -> DamageTypes.WITHER;
            case "out_of_world" -> DamageTypes.FALLING_BLOCK;
            default -> DamageTypes.GENERIC;
        };

        DamageSource source;
        if (context.getEvent() instanceof LivingHurtEvent event && damageType.equals("damage_type")) {
            source = event.getSource();
        }else {
            source = new DamageSource(target.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(type),
                    attacker);
        }

        float finalAmount = (float) evaluate(amountExpression, context.getVariables());

        // 应用伤害
        if (isExtraDamage.equals("true")) {
            extraHurt(target, source, finalAmount);
        } else {
            target.hurt(source, finalAmount);
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
        nbt.putString("DamageType", damageType);
        nbt.putString("IsExtraDamage", isExtraDamage);
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
        String damageType = nbt.contains("DamageType") ? nbt.getString("DamageType") : "magic";
        String target = nbt.contains("Target") ? nbt.getString("Target") : "target";
        String isExtraDamage = nbt.contains("IsExtraDamage") ? nbt.getString("IsExtraDamage") : "true";
        String sourceEntity = nbt.contains("SourceEntity") ? nbt.getString("SourceEntity") : "self";

        return new DealDamageOperation(amountExpression, damageType, isExtraDamage, target, sourceEntity);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("deal_damage", DealDamageOperation::fromNBT);
    }
}