
package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.yixi_xun.affix_core.affix.AffixContext;

import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluate;

/**
 * 伤害操作，修改造成的伤害
 */
public class ModifyDamageOperation implements IOperation {

    private final String amountExpression;

    public ModifyDamageOperation(String amount) {
        this.amountExpression = amount;
    }

    @Override
    public void apply(AffixContext context) {
        // 只在攻击事件中生效
        if (!context.getTrigger().contains("on_attack") || !(context.getEvent() instanceof LivingDamageEvent event)
            || !(context.getTrigger().contains("on_hurt"))) {
            return;
        }
            float finalAmount = (float) evaluate(amountExpression, context.getVariables());
            event.setAmount(finalAmount);
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("Amount", amountExpression);
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

        return new ModifyDamageOperation(amountExpression);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("modify_damage", ModifyDamageOperation::fromNBT);
    }
}
