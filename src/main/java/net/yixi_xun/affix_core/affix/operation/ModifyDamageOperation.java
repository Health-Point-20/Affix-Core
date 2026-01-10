package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.yixi_xun.affix_core.affix.AffixContext;

import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluate;

/**
 * 伤害操作，修改造成的伤害
 */
public class ModifyDamageOperation implements IOperation {

    private final String amountExpression;
    private final String operation;

    public ModifyDamageOperation(String amount, String operation) {
        this.amountExpression = amount;
        this.operation = operation;
    }

    @Override
    public void apply(AffixContext context) {
        // 只在攻击事件中生效
        if (!context.getTrigger().contains("on_attack") && !context.getTrigger().contains("on_hurt")) {
            return;
        }

        if (!(context.getEvent() instanceof LivingHurtEvent event)) {
            return;
        }

        float originalAmount = event.getAmount();
        float calculatedValue = (float) evaluate(amountExpression, context.getVariables());

        switch (operation) {
            case "add" -> event.setAmount(originalAmount + calculatedValue);
            case "multiply" -> event.setAmount(originalAmount * calculatedValue);
            case "set" -> event.setAmount(calculatedValue);
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
        nbt.putString("Amount", amountExpression);
        nbt.putString("Operation", operation);
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
        String amountExpression = nbt.contains("Amount") ? nbt.getString("Amount") : "damage";
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
