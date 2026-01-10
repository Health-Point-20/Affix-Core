package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.MinecraftForge;
import net.yixi_xun.affix_core.AffixCoreMod;
import net.yixi_xun.affix_core.api.AffixEvent;

import java.util.HashMap;
import java.util.Map;

public class OperationManager {

    // 用于快速查找的映射
    private static final Map<String, OperationFactory> FACTORY_MAP = new HashMap<>();

    /**
     * 注册操作工厂
     */
    public static void registerFactory(String type, OperationFactory factory) {
        // 触发操作注册事件
        AffixEvent.OperationRegisterEvent event = new AffixEvent.OperationRegisterEvent(type, factory);
        MinecraftForge.EVENT_BUS.post(event);
        
        // 如果事件中已经设置了工厂（例如被其他模组处理），则使用设置的工厂
        if (event.isRegistered() && event.getFactory() != null) {
            FACTORY_MAP.put(type, event.getFactory());
        } else {
            // 否则使用原始的工厂
            FACTORY_MAP.put(type, factory);
        }
    }

    public static Map<String, OperationFactory> getFactoryMap() {
        return new HashMap<>(FACTORY_MAP);
    }

    /**
     * 从NBT创建操作
     */
    public static IOperation createOperation(CompoundTag nbt) {
        if (!nbt.contains("Type")) {
            AffixCoreMod.LOGGER.error("Operation NBT missing Type field");
            return null;
        }

        String type = nbt.getString("Type");
        OperationFactory factory = FACTORY_MAP.get(type);

        if (factory == null) {
            AffixCoreMod.LOGGER.error("Unknown operation type: {}", type);
            return null;
        }

        try {
            return factory.create(nbt);
        } catch (Exception e) {
            AffixCoreMod.LOGGER.error("Failed to create operation of type: {}", type, e);
            return null;
        }
    }

    /**
     * 操作工厂接口
     */
    public interface OperationFactory {
        /**
         * 从NBT创建操作实例
         */
        IOperation create(CompoundTag nbt);
    }
}