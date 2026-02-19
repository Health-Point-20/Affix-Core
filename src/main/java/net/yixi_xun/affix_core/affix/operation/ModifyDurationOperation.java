package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.yixi_xun.affix_core.affix.AffixContext;

/**
 * 修改耐久度操作，用于修改物品的当前耐久度或最大耐久度
 */
public class ModifyDurationOperation extends BaseOperation {

    private final String amountExpression;
    private final OperationType operation;

    public enum OperationType {
        DURATION("duration"),
        MAX_DURATION("max_duration");
        
        private final String name;
        OperationType(String name) { this.name = name; }
        public String getName() { return name; }
        
        public static OperationType fromString(String name) {
            if (name == null || name.isEmpty()) return DURATION;
            for (OperationType type : values()) {
                if (type.name.equalsIgnoreCase(name)) return type;
            }
            return DURATION;
        }
    }

    public ModifyDurationOperation(String amountExpression, String operation) {
        this.amountExpression = amountExpression != null ? amountExpression : "duration";
        this.operation = OperationType.fromString(operation);
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }
        
        ItemStack itemStack = context.getItemStack();
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        // 移除 try-catch 块，因为 ExpressionHelper 已经处理了表达式异常
        setupDurationVariables(context, itemStack);
        
        switch (operation) {
            case DURATION -> {
                int newDamage = itemStack.getMaxDamage() - (int) evaluateOrDefaultValue(amountExpression, context.getVariables(), 0.0);
                itemStack.setDamageValue(Math.max(0, Math.min(newDamage, itemStack.getMaxDamage())));
            }
            case MAX_DURATION -> {
                int newMaxDurability = (int) evaluateOrDefaultValue(amountExpression, context.getVariables(), itemStack.getMaxDamage());
                itemStack.getOrCreateTag().putInt("Affix_Durability", Math.max(1, newMaxDurability));
            }
        }
    }

    /**
     * 设置耐久度相关变量
     */
    private void setupDurationVariables(AffixContext context, ItemStack itemStack) {
        int currentDurability = itemStack.getMaxDamage() - itemStack.getDamageValue();
        context.addVariable("duration", currentDurability);
        
        int maxDurability = itemStack.getOrCreateTag().contains("Affix_Durability") ? 
            itemStack.getOrCreateTag().getInt("Affix_Durability") : itemStack.getMaxDamage();
        context.addVariable("max_duration", maxDurability);
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
        nbt.putString("AmountExpression", amountExpression);
        nbt.putString("Operation", operation.getName());
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
        String amountExpression = nbt.contains("AmountExpression") ? nbt.getString("AmountExpression") : "duration";
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
