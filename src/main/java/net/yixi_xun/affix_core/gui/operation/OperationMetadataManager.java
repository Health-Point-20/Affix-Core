package net.yixi_xun.affix_core.gui.operation;

import net.minecraft.nbt.CompoundTag;
import net.yixi_xun.affix_core.affix.operation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 操作元数据管理器，用于注册和获取操作类型的GUI元数据
 */
public class OperationMetadataManager {
    private static final Map<String, OperationMetadata<?>> METADATA_MAP = new HashMap<>();

    static {
        registerMetadata(new ModifyDamageOperationMetadata());
        registerMetadata(new ExtraDamageOperationMetadata());
        registerMetadata(new PotionOperationMetadata());
        registerMetadata(new AttributeOperationMetadata());
        registerMetadata(new ModifyEffectOperationMetadata());
        registerMetadata(new NBTOperationMetadata());
        registerMetadata(new CommandOperationMetadata());
    }

    public static void registerMetadata(OperationMetadata<?> metadata) {
        METADATA_MAP.put(metadata.getOperationType(), metadata);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IOperation> OperationMetadata<T> getMetadata(String operationType) {
        return (OperationMetadata<T>) METADATA_MAP.get(operationType);
    }

    public static boolean hasMetadata(String operationType) {
        return METADATA_MAP.containsKey(operationType);
    }

    public static String[] getAllOperationTypes() {
        return METADATA_MAP.keySet().toArray(new String[0]);
    }
}