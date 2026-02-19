package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.yixi_xun.affix_core.AffixCoreMod;
import net.yixi_xun.affix_core.affix.AffixContext;

/**
 * 修改药水效果操作，在药水效果应用事件中修改效果参数
 */
public class ModifyEffectOperation extends BaseOperation {

    private final String durationExpression;
    private final String amplifierExpression;

    public ModifyEffectOperation(String durationExpression, String amplifierExpression) {
        this.durationExpression = durationExpression != null ? durationExpression : "100";
        this.amplifierExpression = amplifierExpression != null ? amplifierExpression : "0";
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }
        
        if (!(context.getEvent() instanceof MobEffectEvent.Applicable event)) {
            return;
        }
        
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.getPersistentData().getBoolean("affix_effect_modifying")) {
            return;
        }
        
        MobEffectInstance oldEffect = event.getEffectInstance();

        try {
            // 计算新效果参数
            int newDuration = Math.max(0, (int) evaluateOrDefaultValue(durationExpression, context.getVariables(), oldEffect.getDuration()));
            int newAmplifier = Math.max(0, (int) evaluateOrDefaultValue(amplifierExpression, context.getVariables(), oldEffect.getAmplifier()));

            if (newDuration == 0) {
                event.setResult(Event.Result.DENY);
                return;
            }

            // 创建并应用新效果
            event.setResult(Event.Result.DENY);
            MobEffectInstance newEffect = new MobEffectInstance(
                oldEffect.getEffect(),
                newDuration,
                newAmplifier
            );
            
            entity.getPersistentData().putBoolean("affix_effect_modifying", true);
            entity.addEffect(newEffect);
            entity.getPersistentData().remove("affix_effect_modifying");
        } catch (Exception e) {
            AffixCoreMod.LOGGER.error("修改药水效果时发生错误", e);
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
        nbt.putString("DurationExpression", durationExpression);
        nbt.putString("AmplifierExpression", amplifierExpression);
        return nbt;
    }

    @Override
    public String getType() {
        return "effect_modify";
    }

    /**
     * 工厂方法，从NBT创建PotionOperation
     */
    public static ModifyEffectOperation fromNBT(CompoundTag nbt) {
        String durationExpression = getString(nbt, "DurationExpression", "100");
        String amplifierExpression = getString(nbt, "AmplifierExpression", "0");

        return new ModifyEffectOperation(durationExpression, amplifierExpression);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("effect_modify", ModifyEffectOperation::fromNBT);
    }
}