package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.yixi_xun.affix_core.affix.AffixContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 修改伤害操作
 */
public class ModifyDamageOperation extends BaseOperation {

    private final String amountExpression;
    private final MathOperation operation;
    private final Map<LivingEntity, Double> damageModifiers = new WeakHashMap<>();

    public ModifyDamageOperation(String amountExpression, String operation) {
        this.amountExpression = amountExpression != null ? amountExpression : "damage";
        this.operation = MathOperation.fromString(operation);
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }
        
        // 在攻击事件中立即生效
        List<String> trigger = Arrays.asList(context.getAffix().trigger().split(","));
        if ((trigger.contains("on_attack") || trigger.contains("on_hurt"))) {
           if ((context.getEvent() instanceof LivingHurtEvent event)) {
               float originalAmount = event.getAmount();
               float calculatedValue = (float) evaluateOrDefaultValue(amountExpression, context.getVariables(), originalAmount);
               float newValue = operation.apply(originalAmount, calculatedValue);
               event.setAmount(newValue);
            }
        } else {
            // 非攻击事件中添加伤害修改器
            damageModifiers.put(context.getOwner(), evaluateOrDefaultValue(amountExpression, context.getVariables(), 0));
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        Double modifier = damageModifiers.get(entity);
        if (modifier != null) {
            float originalAmount = event.getAmount();
            float calculatedValue = (float) modifier.doubleValue();
            float newValue = operation.apply(originalAmount, calculatedValue);
            event.setAmount(newValue);
        }
        damageModifiers.remove(entity);
    }

    @Override
    public void remove(AffixContext context) {
        // 不需要移除
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("AmountExpression", amountExpression);
        nbt.putString("Operation", operation.getName());
        return nbt;
    }

    @Override
    public String getType() {
        return "modify_damage";
    }

    /**
     * 工厂方法，从NBT创建DamageOperation
     */
    public static ModifyDamageOperation fromNBT(CompoundTag nbt) {
        String amountExpression = nbt.contains("AmountExpression") ? nbt.getString("AmountExpression") : "damage";
        String operation = nbt.contains("Operation") ? nbt.getString("Operation") : "set";

        return new ModifyDamageOperation(amountExpression, operation);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("modify_damage", ModifyDamageOperation::fromNBT);
    }
}
