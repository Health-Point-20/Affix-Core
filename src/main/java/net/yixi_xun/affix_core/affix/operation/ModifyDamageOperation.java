package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.yixi_xun.affix_core.affix.AffixContext;

/**
 * 修改伤害操作，在攻击或受伤事件中修改伤害值
 */
public class ModifyDamageOperation extends BaseOperation {

    private final String amountExpression;
    private final MathOperation operation;

    public ModifyDamageOperation(String amountExpression, String operation) {
        this.amountExpression = amountExpression != null ? amountExpression : "damage";
        this.operation = MathOperation.fromString(operation);
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }
        
        // 只在攻击事件中生效
        String trigger = context.getTrigger() != null ? context.getTrigger().toString() : "";
        if (trigger == null || (!trigger.contains("on_attack") && !trigger.contains("on_hurt"))) {
            return;
        }

        if (!(context.getEvent() instanceof LivingHurtEvent event)) {
            return;
        }

        // 移除 try-catch 块，因为 ExpressionHelper 已经处理了表达式异常
        float originalAmount = event.getAmount();
        float calculatedValue = (float) evaluateOrDefaultValue(amountExpression, context.getVariables(), originalAmount);
        float newValue = operation.apply(originalAmount, calculatedValue);
        event.setAmount(newValue);
        // 移除 catch 块
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
