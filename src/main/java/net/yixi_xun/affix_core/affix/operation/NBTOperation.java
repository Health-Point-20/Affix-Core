package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.yixi_xun.affix_core.affix.AffixContext;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;

/**
 * NBT操作，用于修改实体或物品的NBT数据
 */
public class NBTOperation extends BaseOperation {
    private final String nbtPath;
    private final String valueExpression;
    private final ValueType valueType;
    private final OperationMode operationMode;
    /** 
     * 目标物品路径，支持：
     * - "self" - 触发词缀的物品
     * - "owner.mainhand" - 所有者主手物品
     * - "owner.inventory.0" - 所有者物品栏第 0 格
     * - "target" - 目标实体（获取 PersistentData）
     * - "target.mainhand" - 目标主手物品
     * 等任意 getTargetItem 支持的路径
     */
    private final String targetExpression;

    public enum ValueType {
        NUMBER("number"), STRING("string"), BOOLEAN("boolean");
        
        private final String name;
        ValueType(String name) { this.name = name; }
        public String getName() { return name; }
        
        public static ValueType fromString(String name) {
            if (name == null || name.isEmpty()) return NUMBER;
            for (ValueType type : values()) {
                if (type.name.equalsIgnoreCase(name)) return type;
            }
            return NUMBER;
        }
    }

    public enum OperationMode {
        ADD("add"), MODIFY("modify"), DELETE("delete");
        
        private final String name;
        OperationMode(String name) { this.name = name; }
        public String getName() { return name; }
        
        public static OperationMode fromString(String name) {
            if (name == null || name.isEmpty()) return ADD;
            for (OperationMode mode : values()) {
                if (mode.name.equalsIgnoreCase(name)) return mode;
            }
            return ADD;
        }
    }


    public NBTOperation(String nbtPath, String valueExpression, String valueType, 
                       String operationMode, String targetExpression) {
        this.nbtPath = nbtPath != null ? nbtPath.trim() : "";
        this.valueExpression = valueExpression != null ? valueExpression : "0";
        this.valueType = ValueType.fromString(valueType);
        this.operationMode = OperationMode.fromString(operationMode);
        this.targetExpression = targetExpression != null ? targetExpression.trim() : "self";
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null || nbtPath.isEmpty()) {
            LOGGER.warn("NBT操作失败：上下文为空或 NBT 路径为空");
            return;
        }
            
        try {
            CompoundTag targetNBT = getTargetNBT(context);
            if (targetNBT == null) {
                LOGGER.debug("无法获取目标 NBT 容器：{}", targetExpression);
                return;
            }
    
            switch (operationMode) {
                case ADD, MODIFY -> modifyNBTValue(context, targetNBT);
                case DELETE -> deleteNBTValue(targetNBT);
            }
        } catch (Exception e) {
            LOGGER.error("执行 NBT操作时发生错误：path={}", nbtPath, e);
        }
    }

    private void modifyNBTValue(AffixContext context, CompoundTag targetNBT) {
        // 计算 NBT 值
        Object computedValue = computeValue(context);

        // 修改 NBT，根据操作模式决定是否添加 Affixes 前缀
        String fullPath = nbtPath;
        if ("add".equalsIgnoreCase(operationMode.getName()) && !fullPath.startsWith("Affixes")) {
            // 只有在添加模式下才确保 NBT 路径在 Affixes 下
            fullPath = ensureAffixesPrefix(fullPath);
        }
        
        // 解析路径并设置 NBT 值
        setNBTValueByPath(targetNBT, fullPath, computedValue);
    }

    /**
     * 根据 ValueType 计算表达式的值
     */
    private Object computeValue(AffixContext context) {
        return switch (valueType) {
            case NUMBER -> {
                double result = evaluateOrDefaultValue(valueExpression, context.getVariables(), 0.0);
                // 判断是整数还是浮点数
                yield (result == (long) result) ? (long) result : result;
            }
            case STRING -> String.valueOf(evaluateOrDefaultValue(valueExpression, context.getVariables(), 0.0));
            case BOOLEAN -> {
                double result = evaluateOrDefaultValue(valueExpression, context.getVariables(), 0.0);
                yield Math.abs(result) > 1e-10; // 非零视为真
            }
        };
    }

    /**
     * 确保路径以 AffixNBT 开头
     */
    private String ensureAffixesPrefix(String path) {
        if (path.startsWith(".")) {
            return "AffixNBT" + path;
        } else {
            return "AffixNBT." + path;
        }
    }

    private void deleteNBTValue(CompoundTag targetNBT) {
        String fullPath = nbtPath;
        // 删除模式下也需要处理路径前缀
        if (!fullPath.startsWith("Affixes")) {
            fullPath = ensureAffixesPrefix(fullPath);
        }
            
        // 解析路径并删除 NBT 值
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
     * 根据 targetPath 参数获取相应的 NBT 容器
     * 全面使用 getTargetItem() 方法统一物品获取逻辑
     */
    private CompoundTag getTargetNBT(AffixContext context) {
        // 判断是否是实体目标（只有 "target" 或 UUID 格式）
        if ("target".equalsIgnoreCase(targetExpression.trim())) {
            LivingEntity targetEntity = getTargetEntity(context, "target");
            if (isInValidEntity(targetEntity)) {
                return null;
            }
            return targetEntity.getPersistentData();
        }
        
        // 其他情况都作为物品路径处理
        ItemStack targetItem = getTargetItem(context, targetExpression);
        if (targetItem == null || targetItem.isEmpty()) {
            return null;
        }
        return targetItem.getOrCreateTag();
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
        nbt.putString("ValueType", valueType.getName());
        nbt.putString("OperationMode", operationMode.getName());
        nbt.putString("TargetExpression", targetExpression);
        return nbt;
    }

    @Override
    public String getType() {
        return "nbt_modify";
    }

    /**
     * 工厂方法，从 NBT 创建 NBTOperation
     */
    public static NBTOperation fromNBT(CompoundTag nbt) {
        String nbtPath = getStringOrDefaultValue(nbt, "NBTPath", "");
        String valueExpression = getStringOrDefaultValue(nbt, "ValueExpression", "0");
        String valueType = getStringOrDefaultValue(nbt, "ValueType", "number");
        String operationMode = getStringOrDefaultValue(nbt, "OperationMode", "add");
        String targetPath = getStringOrDefaultValue(nbt, "TargetExpression", "owner");
    
        return new NBTOperation(nbtPath, valueExpression, valueType, operationMode, targetPath);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("nbt_modify", NBTOperation::fromNBT);
    }
}