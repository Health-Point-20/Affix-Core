package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.yixi_xun.affix_core.affix.AffixContext;
import net.yixi_xun.affix_core.api.ExpressionHelper;

/**
 * 药水效果操作，给目标或自己添加药水效果
 */
public class PotionOperation implements IOperation {

    private final ResourceLocation effectId;
    private final String durationExpression;
    private final String amplifierExpression;
    private final String maxAmplifierExpression;
    private final boolean ambient;
    private final boolean showParticles;
    private final boolean showIcon;
    private final boolean overlayEffect;
    private final String targetString;

    public PotionOperation(ResourceLocation effectId, String durationExpression, String amplifierExpression, String maxAmplifierExpression, boolean ambient, 
                          boolean showParticles, boolean showIcon, boolean overrideExisting, String target) {
        this.effectId = effectId;
        this.durationExpression = durationExpression;
        this.amplifierExpression = amplifierExpression;
        this.maxAmplifierExpression = maxAmplifierExpression;
        this.ambient = ambient;
        this.showParticles = showParticles;
        this.showIcon = showIcon;
        this.overlayEffect = overrideExisting;
        this.targetString = target;
    }

    @Override
    public void apply(AffixContext context) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        if (effect == null) {
            return;
        }

        // 确定目标实体
        LivingEntity target = targetString.equals("self") ? context.getOwner() : context.getTarget();
        if (target == null) {
            // 如果无法获取目标实体，则使用持有者作为默认目标
            target = context.getOwner();
        }
        if (target == null) {
            return; // 如果仍然为空，则返回
        }

        // 计算药水效果的持续时间和等级
        int amplifier = (int) Math.max(0, ExpressionHelper.evaluate(amplifierExpression, context.getVariables()));
        int duration = (int) Math.max(0, ExpressionHelper.evaluate(durationExpression, context.getVariables()));
        int maxAmplifier = (int) Math.max(0, ExpressionHelper.evaluate(maxAmplifierExpression, context.getVariables()));

        // 处理效果叠加逻辑
        MobEffectInstance existingEffect = target.getEffect(effect);
        if (existingEffect != null && overlayEffect) {
            // 将amplifier转换为等级
            int existingLevel = existingEffect.getAmplifier() + 1;
            int level = amplifier + 1; // 新增效果也转换为等级
            
            // 进行等级叠加
            int finalLevel = existingLevel + level - 1; // 减1是因为等级1对应放大器0
            
            // 转换回amplifier并应用最大限制
            amplifier = Math.min(finalLevel - 1, maxAmplifier);
        } else {
            // 直接应用
            amplifier = Math.min(amplifier, maxAmplifier);
        }

        // 创建药水效果实例
        MobEffectInstance effectInstance = new MobEffectInstance(
            effect, 
            duration,
            amplifier,
            ambient, 
            showParticles, 
            showIcon
        );

        // 应用药水效果
        target.addEffect(effectInstance);
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
        String target = nbt.contains("Target") ? nbt.getString("Target") : "target";

        return new PotionOperation(effectId, durationExpression, amplifierExpression, maxAmplifierExpression, ambient, showParticles, showIcon, overlayEffect, target);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("add_potion", PotionOperation::fromNBT);
    }
}