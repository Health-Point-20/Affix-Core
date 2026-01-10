package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import net.yixi_xun.affix_core.affix.AffixContext;
import net.yixi_xun.affix_core.api.ExpressionHelper;

/**
 * 药水效果操作，修改添加的药水效果
 */
public class ModifyEffectOperation implements IOperation {

    private final String durationExpression;
    private final String amplifierExpression;

    public ModifyEffectOperation(String durationExpression, String amplifierExpression) {
        this.durationExpression = durationExpression;
        this.amplifierExpression = amplifierExpression;
    }

    @Override
    public void apply(AffixContext context) {
        if (!(context.getEvent() instanceof MobEffectEvent.Applicable event)) return;
        if (event.getEntity().getPersistentData().getBoolean("affix_effect_modifying")) return;
        event.setResult(Event.Result.DENY);
        MobEffectInstance oldEffect = event.getEffectInstance();

        // 计算药水效果的持续时间和等级
        int computedDuration = (int) ExpressionHelper.evaluate(durationExpression, context.getVariables());
        int computedAmplifier = (int) ExpressionHelper.evaluate(amplifierExpression, context.getVariables());

        if (computedDuration <= 0 || computedAmplifier < 0) return;

        // 创建药水效果实例
        MobEffectInstance effectInstance = new MobEffectInstance(
            oldEffect.getEffect(),
            computedDuration, 
            computedAmplifier
        );
        event.getEntity().getPersistentData().putBoolean("affix_effect_modifying", true);
        event.getEntity().addEffect(effectInstance);
        event.getEntity().getPersistentData().remove("affix_effect_modifying");
    }

    @Override
    public void remove(AffixContext context) {
        // 不需要移除
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("DurationExpression", durationExpression);
        nbt.putString("AmplifierExpression", amplifierExpression);
        return nbt;
    }

    @Override
    public String getType() {
        return "on_effect_add";
    }

    /**
     * 工厂方法，从NBT创建PotionOperation
     */
    public static ModifyEffectOperation fromNBT(CompoundTag nbt) {
        String durationExpression = nbt.contains("DurationExpression") ? nbt.getString("DurationExpression") : "100";
        String amplifierExpression = nbt.contains("AmplifierExpression") ? nbt.getString("AmplifierExpression") : "0";

        return new ModifyEffectOperation(durationExpression, amplifierExpression);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("on_effect_add", ModifyEffectOperation::fromNBT);
    }
}