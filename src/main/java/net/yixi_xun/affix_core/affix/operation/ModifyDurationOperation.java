package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.yixi_xun.affix_core.affix.AffixContext;

import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluate;

public class ModifyDurationOperation implements IOperation {

    private final String amountExpression;
    private final String operation;

    public ModifyDurationOperation(String amount, String operation) {
        this.amountExpression = amount;
        this.operation = operation;
    }

    @Override
    public void apply(AffixContext context) {
        var itemStack = context.getItemStack();
        context.addVariable("duration", itemStack.getMaxDamage() - itemStack.getDamageValue());
        if (itemStack.getOrCreateTag().contains("Affix_Durability")) {
            context.addVariable("max_duration", itemStack.getOrCreateTag().getInt("Affix_Durability"));
       } else {
           context.addVariable("max_duration", itemStack.getMaxDamage());
       }

        switch (operation) {
            case "duration" -> itemStack.setDamageValue(itemStack.getMaxDamage() - (int) evaluate(amountExpression, context.getVariables()));
            case "max_duration" -> itemStack.getOrCreateTag().putInt("Affix_Durability", (int) evaluate(amountExpression, context.getVariables()));
        }
    }

    @Override
    public void remove(AffixContext context) {
        var itemStack = context.getItemStack();
        itemStack.getOrCreateTag().remove("Affix_Durability");
        if (itemStack.getOrCreateTag().isEmpty()) {
            itemStack.setTag(null);
        }

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
        return "modify_duration";
    }

    /**
     * 工厂方法，从NBT创建ModifyDurationOperation
     */
    public static ModifyDurationOperation fromNBT(CompoundTag nbt) {
        String amountExpression = nbt.contains("Amount") ? nbt.getString("Amount") : "duration";
        String operation = nbt.contains("Operation") ? nbt.getString("Operation") : "duration";

        return new ModifyDurationOperation(amountExpression, operation);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("modify_duration", ModifyDurationOperation::fromNBT);
    }
}
