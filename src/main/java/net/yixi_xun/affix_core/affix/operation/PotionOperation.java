package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.yixi_xun.affix_core.AffixCoreMod;
import net.yixi_xun.affix_core.affix.AffixContext;

/**
 * 药水效果操作，给目标或自己添加药水效果
 */
public class PotionOperation extends BaseOperation {

    private final ResourceLocation effectId;
    private final String durationExpression;
    private final String amplifierExpression;
    private final boolean overlayEffect;
    private final String maxAmplifierExpression;
    private final String maxDurationExpression;
    private final boolean ambient;
    private final boolean showParticles;
    private final boolean showIcon;
    private final String targetString;

    public PotionOperation(ResourceLocation effectId, String durationExpression, String amplifierExpression, 
                          String maxAmplifierExpression, String maxDurationExpression, boolean ambient, boolean showParticles,
                          boolean showIcon, boolean overrideExisting, String target) {
        this.effectId = effectId != null ? effectId : ResourceLocation.tryParse("minecraft.speed");
        this.durationExpression = durationExpression != null ? durationExpression : "100";
        this.amplifierExpression = amplifierExpression != null ? amplifierExpression : "0";
        this.maxAmplifierExpression = maxAmplifierExpression != null ? maxAmplifierExpression : "4";
        this.maxDurationExpression = maxDurationExpression != null ? maxDurationExpression : "100";
        this.ambient = ambient;
        this.showParticles = showParticles;
        this.showIcon = showIcon;
        this.overlayEffect = overrideExisting;
        this.targetString = target != null ? target : "target";
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }
        
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        if (effect == null) {
            AffixCoreMod.LOGGER.warn("无效的药水效果ID: {}", effectId);
            return;
        }

        LivingEntity target = getTargetEntity(context, targetString);
        if (isInValidEntity(target)) {
            return;
        }

        // 计算药水效果参数
        int amplifier = calculateAmplifier(context, effect, target);
        int duration = calculateDuration(context, effect, target);

        // 创建并应用药水效果
        MobEffectInstance effectInstance = new MobEffectInstance(
            effect, duration, amplifier, ambient, showParticles, showIcon
        );
        target.addEffect(effectInstance);
    }

    /**
     * 计算药水效果等级（考虑叠加逻辑）
     */
    private int calculateAmplifier(AffixContext context, MobEffect effect, LivingEntity target) {
        int baseAmplifier = Math.max(0, (int) evaluateOrDefaultValue(amplifierExpression, context.getVariables(), 0));
        int maxAmplifier = Math.max(0, (int) evaluateOrDefaultValue(maxAmplifierExpression, context.getVariables(), 4));

        if (overlayEffect) {
            MobEffectInstance existingEffect = target.getEffect(effect);
            if (existingEffect != null) {
                int existingLevel = existingEffect.getAmplifier() + 1;
                int newLevel = baseAmplifier + 1;
                int finalAmplifier = existingLevel + newLevel - 1; // 还原回amplifier
                return Math.min(finalAmplifier, maxAmplifier);
            }
        }
        
        return Math.min(baseAmplifier, maxAmplifier);
    }

    private int calculateDuration(AffixContext context, MobEffect effect, LivingEntity target) {
        int baseDuration = Math.max(0, (int) evaluateOrDefaultValue(durationExpression, context.getVariables(), 100));
        int maxDuration = Math.max(0, (int) evaluateOrDefaultValue(maxDurationExpression, context.getVariables(), 100000));

        if (overlayEffect) {
            MobEffectInstance existingEffect = target.getEffect(effect);
            if (existingEffect != null) {
                int existingDuration = existingEffect.getDuration();
                int finalDuration = existingDuration + baseDuration;
                return Math.min(finalDuration, maxDuration);
            }
        }

        return Math.min(baseDuration, maxDuration);
    }

    @Override
    public void remove(AffixContext context) {
       // 不需要移除
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("Effect", effectId.toString());
        nbt.putString("DurationExpression", durationExpression);
        nbt.putString("AmplifierExpression", amplifierExpression);
        nbt.putBoolean("Ambient", ambient);
        nbt.putBoolean("ShowParticles", showParticles);
        nbt.putBoolean("ShowIcon", showIcon);
        nbt.putBoolean("OverlayEffect", overlayEffect);
        nbt.putString("MaxAmplifierExpression", maxAmplifierExpression);
        nbt.putString("MaxDurationExpression", maxDurationExpression);
        nbt.putString("Target", targetString);
        return nbt;
    }

    @Override
    public String getType() {
        return "add_potion";
    }

    /**
     * 工厂方法，从NBT创建PotionOperation
     */
    public static PotionOperation fromNBT(CompoundTag nbt) {
        String effectStr = nbt.contains("Effect") ? nbt.getString("Effect") : "minecraft:speed";
        ResourceLocation effectId = ResourceLocation.tryParse(effectStr);

        String durationExpression = nbt.contains("DurationExpression") ? nbt.getString("DurationExpression") : "100";
        String amplifierExpression = nbt.contains("AmplifierExpression") ? nbt.getString("AmplifierExpression") : "0";
        boolean ambient = nbt.contains("Ambient") && nbt.getBoolean("Ambient");
        boolean showParticles = !nbt.contains("ShowParticles") || nbt.getBoolean("ShowParticles");
        boolean showIcon = !nbt.contains("ShowIcon") || nbt.getBoolean("ShowIcon");
        boolean overlayEffect = nbt.contains("OverlayEffect") && nbt.getBoolean("OverlayEffect");
        String maxAmplifierExpression = nbt.contains("MaxAmplifierExpression") ? nbt.getString("MaxAmplifierExpression") : "4";
        String maxDurationExpression = nbt.contains("MaxDurationExpression") ? nbt.getString("MaxDurationExpression") : "3600";
        String target = nbt.contains("Target") ? nbt.getString("Target") : "target";

        return new PotionOperation(effectId, durationExpression, amplifierExpression, maxAmplifierExpression, maxDurationExpression, ambient, showParticles, showIcon, overlayEffect, target);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("add_potion", PotionOperation::fromNBT);
    }
}