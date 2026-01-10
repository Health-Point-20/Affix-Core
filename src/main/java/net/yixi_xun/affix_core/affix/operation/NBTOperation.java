package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.yixi_xun.affix_core.affix.AffixContext;

import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluate;

public class NBTOperation implements IOperation {
    private final String nbtPath;  // NBT路径，例如 "Affixes.custom_value"
    private final String valueExpression;  // NBT值表达式
    private final String valueType;  // NBT值类型：number, string, boolean等
    private final String operationMode; // 操作模式：add(添加)/modify(修改)/delete(删除)
    private final String target; // 写入目标：item(物品)/owner(持有者)/attacker(攻击者)/victim(受害者)

    public NBTOperation(String nbtPath, String valueExpression, String valueType, String operationMode, String target) {
        this.nbtPath = nbtPath != null ? nbtPath : "";
        this.valueExpression = valueExpression != null ? valueExpression : "0";
        this.valueType = valueType != null ? valueType : "number";
        this.operationMode = operationMode != null ? operationMode : "add";
        this.target = target != null ? target : "item"; // 默认写入物品
    }

    @Override
    public void apply(AffixContext context) {
        // 根据target获取相应的NBT容器
        CompoundTag targetNBT = getTargetNBT(context);
        if (targetNBT == null) {
            return;
        }

        switch (operationMode) {
            case "add", "modify" -> modifyNBTValue(context, targetNBT);
            case "delete" -> deleteNBTValue(targetNBT);
            default -> modifyNBTValue(context, targetNBT); // 默认为添加/修改
        }
    }

    private void modifyNBTValue(AffixContext context, CompoundTag targetNBT) {
        // 计算NBT值
        Object computedValue;
        if ("number".equalsIgnoreCase(valueType)) {
            double result = evaluate(valueExpression, context.getVariables());
            // 判断是整数还是浮点数
            if (result == (long) result) {
                computedValue = (long) result;
            } else {
                computedValue = result;
            }
        } else if ("string".equalsIgnoreCase(valueType)) {
            // 将表达式结果转换为字符串
            computedValue = evaluate(valueExpression, context.getVariables());
        } else if ("boolean".equalsIgnoreCase(valueType)) {
            double result = evaluate(valueExpression, context.getVariables());
            computedValue = Math.abs(result) > 1e-10; // 非零视为真
        } else {
            // 默认按数字处理
            computedValue = evaluate(valueExpression, context.getVariables());
        }

        // 修改NBT，根据操作模式决定是否添加Affixes前缀
        String fullPath = nbtPath;
        if ("add".equalsIgnoreCase(operationMode) && !fullPath.startsWith("Affixes")) {
            // 只有在添加模式下才确保NBT路径在Affixes下
            if (fullPath.startsWith(".")) {
                fullPath = "Affixes" + fullPath;
            } else {
                fullPath = "Affixes." + fullPath;
            }
        }
        
        // 解析路径并设置NBT值
        setNBTValueByPath(targetNBT, fullPath, computedValue);
    }

    private void deleteNBTValue(CompoundTag targetNBT) {
        String fullPath = nbtPath;
        // 删除模式下也需要处理路径前缀
        if (!fullPath.startsWith("Affixes")) {
            if (fullPath.startsWith(".")) {
                fullPath = "Affixes" + fullPath;
            } else {
                fullPath = "Affixes." + fullPath;
            }
        }
        
        // 解析路径并删除NBT值
        deleteNBTValueByPath(targetNBT, fullPath);
    }

    /**
     * 通过路径删除NBT值
     */
    private void deleteNBTValueByPath(CompoundTag rootNBT, String path) {
        String[] parts = path.split("\\.");

        // 遍历路径直到倒数第二个部分
        CompoundTag current = rootNBT;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (current.contains(part, Tag.TAG_COMPOUND)) {
                current = current.getCompound(part);
            } else {
                // 如果路径不存在，则无需删除
                return;
            }
        }

        // 删除最后的部分
        String lastPart = parts[parts.length - 1];
        current.remove(lastPart);
    }

    /**
     * 通过路径设置NBT值
     */
    private void setNBTValueByPath(CompoundTag rootNBT, String path, Object value) {
        String[] parts = path.split("\\.");

        CompoundTag current = getCurrent(rootNBT, parts);

        // 设置最终值
        String lastPart = parts[parts.length - 1];
        if (value instanceof Long longValue) {
            current.putLong(lastPart, longValue);
        } else if (value instanceof Double doubleValue) {
            current.putDouble(lastPart, doubleValue);
        } else if (value instanceof Float floatValue) {
            current.putFloat(lastPart, floatValue);
        } else if (value instanceof Integer intValue) {
            current.putInt(lastPart, intValue);
        } else if (value instanceof Short shortValue) {
            current.putShort(lastPart, shortValue);
        } else if (value instanceof Byte byteValue) {
            current.putByte(lastPart, byteValue);
        } else if (value instanceof Boolean boolValue) {
            current.putBoolean(lastPart, boolValue);
        } else {
            // 默认作为字符串处理
            current.putString(lastPart, value.toString());
        }
    }

    private static CompoundTag getCurrent(CompoundTag rootNBT, String[] parts) {
        CompoundTag current = rootNBT;

        // 遍历路径的每一部分，除了最后一个
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];

            // 检查当前部分是否已存在且为CompoundTag
            if (current.contains(part, Tag.TAG_COMPOUND)) {
                current = current.getCompound(part);
            } else {
                // 创建新的CompoundTag
                CompoundTag newCompound = new CompoundTag();
                current.put(part, newCompound);
                current = newCompound;
            }
        }
        return current;
    }

    /**
     * 根据target参数获取相应的NBT容器
     */
    private CompoundTag getTargetNBT(AffixContext context) {
        return switch (target.toLowerCase()) {
            case "item" -> {
                var itemStack = context.getItemStack();
                if (itemStack == null || itemStack.isEmpty()) {
                    yield null;
                }
                yield itemStack.getOrCreateTag();
            }
            case "self" -> context.getOwner().getPersistentData();
            case "target" -> {
                LivingEntity target = context.getTarget();
                if (target != null) {
                    yield target.getPersistentData();
                }
                yield null;
            }
            default -> context.getItemStack().getOrCreateTag(); // 默认写入物品
        };
    }

    @Override
    public void remove(AffixContext context) {
        // NBT操作不需要特殊移除逻辑
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("NBTPath", nbtPath);
        nbt.putString("ValueExpression", valueExpression);
        nbt.putString("ValueType", valueType);
        nbt.putString("OperationMode", operationMode);
        nbt.putString("Target", target);
        return nbt;
    }

    @Override
    public String getType() {
        return "nbt_modify";
    }

    /**
     * 工厂方法，从NBT创建NBTOperation
     */
    public static NBTOperation fromNBT(CompoundTag nbt) {
        String nbtPath = nbt.contains("NBTPath") ? nbt.getString("NBTPath") : "";
        String valueExpression = nbt.contains("ValueExpression") ? nbt.getString("ValueExpression") : "0";
        String valueType = nbt.contains("ValueType") ? nbt.getString("ValueType") : "number";
        String operationMode = nbt.contains("OperationMode") ? nbt.getString("OperationMode") : "add";
        String target = nbt.contains("Target") ? nbt.getString("Target") : "item";

        return new NBTOperation(nbtPath, valueExpression, valueType, operationMode, target);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("nbt_modify", NBTOperation::fromNBT);
    }
}