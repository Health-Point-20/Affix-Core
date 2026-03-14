package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.yixi_xun.affix_core.affix.AffixContext;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 自定义消息操作，用于发送自定义消息事件
 */
public class VariableOperation extends BaseOperation {
    private final String target;
    private final String key;
    private final String value;

    private static final Map<LivingEntity, Map<String, Object>> entityVariables = new WeakHashMap<>();

    public VariableOperation(String key, String value, String target) {
        this.target = target != null ? target : "self";
        this.key = key;
        this.value = value;
    }

    public static Map<String, Object> getEntityVariables(LivingEntity entity) {
        return entityVariables.computeIfAbsent(entity, k -> new HashMap<>());
    }

    public static Map<String, Object> getItemVariables(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        Map<String, Object> variables = new HashMap<>();
        if (tag.contains("AffixData")) {
            tag.getCompound("AffixData").getAllKeys().forEach(key -> variables.put(key, tag.get(key)));
        }
        return variables;
    }

    public static void removeEntityVariables(LivingEntity entity) {
        entityVariables.remove(entity);
    }

    /**
     * 处理物品变量（NBT 存储）
     * 数据存储在物品 NBT 的 AffixData 标签下
     */
    private void handleItemVariable(AffixContext context) {
        var itemStack = context.getItemStack();
        if (itemStack == null || itemStack.isEmpty()) {
            return;
        }

        CompoundTag nbt = itemStack.getOrCreateTag();
        CompoundTag affixData = nbt.getCompound("AffixData");

        if (value.startsWith("\"")) {
            // 字符串值
            affixData.putString(key, value.substring(1, value.length() - 1));
        } else {
            // 数值或表达式
            var variables = context.getVariables();
            if (!variables.containsKey(key)) {
                variables.put(key, 0);
            }
            double result = evaluateOrDefaultValue(value, variables, 0);
            // 判断是整数还是浮点数
            if (result == (long) result) {
                affixData.putLong(key, (long) result);
            } else {
                affixData.putDouble(key, result);
            }
        }

        // 将 AffixData 写回 NBT
        nbt.put("AffixData", affixData);
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }

        LivingEntity target = getTargetEntity(context, this.target);
        Map<String, Object> current = getEntityVariables(target);

        if (value.startsWith("\"")) {
            current.put(key, value.substring(1, value.length() - 1));
        } else {
            var variables = context.getVariables();
            if (!variables.containsKey(key)) {
                variables.put(key, 0);
            }
            current.put(key, evaluateOrDefaultValue(value, variables, 0));
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
        nbt.putString("Target", target);
        nbt.putString("Key", key);
        nbt.putString("Value", value);

        return nbt;
    }

    @Override
    public String getType() {
        return "variable";
    }

    /**
     * 工厂方法，从NBT创建CustomMessageOperation
     */
    public static VariableOperation fromNBT(CompoundTag nbt) {
        String target = nbt.contains("Target") ? nbt.getString("Target") : "self";
        String key = nbt.contains("Key") ? nbt.getString("Key") : "default_key";
        String value = nbt.contains("Value") ? nbt.getString("Value") : "0";

        return new VariableOperation(key, value, target);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("variable", VariableOperation::fromNBT);
    }
}
