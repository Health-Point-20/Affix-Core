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
    private final boolean ambient;
    private final boolean showParticles;
    private final boolean showIcon;
    private final String targetString;

    public PotionOperation(ResourceLocation effectId, String durationExpression, String amplifierExpression, boolean ambient, 
                          boolean showParticles, boolean showIcon, String target) {
        this.effectId = effectId;
        this.durationExpression = durationExpression;
        this.amplifierExpression = amplifierExpression;
        this.ambient = ambient;
        this.showParticles = showParticles;
        this.showIcon = showIcon;
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
        int computedDuration = (int) Math.max(0, ExpressionHelper.evaluate(durationExpression, context.getVariables()));
        int computedAmplifier = (int) Math.max(0, ExpressionHelper.evaluate(amplifierExpression, context.getVariables()));

        // 创建药水效果实例
        MobEffectInstance effectInstance = new MobEffectInstance(
            effect, 
            computedDuration, 
            computedAmplifier, 
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
        String target = nbt.contains("Target") ? nbt.getString("Target") : "target";

        return new PotionOperation(effectId, durationExpression, amplifierExpression, ambient, showParticles, showIcon, target);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("add_potion", PotionOperation::fromNBT);
    }
}