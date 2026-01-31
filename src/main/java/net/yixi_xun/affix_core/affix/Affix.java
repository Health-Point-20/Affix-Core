package net.yixi_xun.affix_core.affix;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.common.MinecraftForge;
import net.yixi_xun.affix_core.affix.operation.IOperation;
import net.yixi_xun.affix_core.affix.operation.OperationManager;
import net.yixi_xun.affix_core.api.AffixEvent;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;
import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluateCondition;

/**
 * 表示一个词缀，包含触发器、条件、操作、冷却时间和槽位限制等信息
 *
 * @param index 在物品NBT列表中的索引，用于冷却追踪
 */
public record Affix(String trigger, String condition, IOperation operation, Long cooldown, int triggerCount, EquipmentSlot slot,
                    int index) {

    /**
     * 从物品NBT中读取词缀信息
     */
    public static Affix fromNBT(CompoundTag nbt, int index) {
        // 读取触发器，默认为空
        String trigger = nbt.contains("Trigger") ? nbt.getString("Trigger") : "";

        // 读取条件，默认为空字符串（视为true）
        String condition = nbt.contains("Condition") ? nbt.getString("Condition") : "";

        // 读取操作配置
        IOperation operation;
        if (nbt.contains("Operation")) {
            CompoundTag operationTag = nbt.getCompound("Operation");
            operation = OperationManager.createOperation(operationTag);
            // 如果OperationManager无法创建操作，抛出异常
            if (operation == null) {
                LOGGER.warn("Invalid operation in Affix NBT");
                return null;
            }
        } else {
            LOGGER.warn("Operation is required for Affix");
            return null;
        }

        // 读取冷却时间，默认为0（无冷却）
        Long cooldown = nbt.contains("Cooldown") ? nbt.getLong("Cooldown") : 0L;

        // 读取触发次数，默认为0
        int triggerCount = nbt.contains("TriggerCount") ? nbt.getInt("TriggerCount") : 0;

        // 读取槽位限制，默认为null（任意槽位）
        EquipmentSlot slot = null; // 默认值为任意槽位
        if (nbt.contains("Slot")) {
            if (nbt.getString("Slot") instanceof String) {
                String slotName = nbt.getString("Slot");
                try {
                    slot = EquipmentSlot.byName(slotName.toLowerCase());
                } catch (IllegalArgumentException ignored) {
                    // 如果无效槽位名，使用默认值
                }
            } else if (nbt.get("Slot") instanceof NumericTag) {
                // 如果是数字，尝试转换为EquipmentSlot
                int slotIndex = nbt.getInt("Slot");
                EquipmentSlot[] slots = EquipmentSlot.values();
                if (slotIndex >= 0 && slotIndex < slots.length) {
                    slot = slots[slotIndex];
                } else {
                    slot = null; // 如果索引超出范围，使用默认值
                }
            }
        }

        return new Affix(trigger, condition, operation, cooldown, triggerCount, slot, index);
    }

    /**
     * 将词缀保存到物品NBT中
     */
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();

        if (trigger != null && !trigger.isEmpty()) {
            nbt.putString("Trigger", trigger);
        }

        if (condition != null && !condition.isEmpty()) {
            nbt.putString("Condition", condition);
        }

        if (operation != null) {
            nbt.put("Operation", operation.toNBT());
        }

        if (cooldown > 0) {
            nbt.putLong("Cooldown", cooldown);
        }

        if (triggerCount > 0) {
            nbt.putInt("TriggerCount", triggerCount);
        }

        if (slot != null) {
            nbt.putString("Slot", slot.getName());
        }

        return nbt;
    }

    /**
     * 检查词缀是否在非指定槽位触发
     */
    public boolean triggerInInvalidSlot(EquipmentSlot equipmentSlot) {
        // 如果slot为null（任意槽位）或匹配指定槽位，则返回true
        return slot != null && slot != equipmentSlot;
    }

    /**
     * 检查条件是否不满足
     */
    public boolean checkCondition(AffixContext context) {
        // 如果没有条件，默认为true
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        // 使用ExpressionHelper评估条件
        return evaluateCondition(condition, context.getVariables());

    }

    /**
     * 执行词缀操作
     */
    public void execute(AffixContext context) {
        if (operation != null) {
            // 触发词缀执行事件
            AffixEvent.AffixExecuteEvent executeEvent = new AffixEvent.AffixExecuteEvent(context);
            MinecraftForge.EVENT_BUS.post(executeEvent);

            if (executeEvent.isCanceled()) return;

            operation.apply(context);
        }
    }

    /**
     * 执行词缀移除时操作
     */
    public void remove(AffixContext context) {
        if (operation != null) {
            // 触发词缀移除事件
            AffixEvent.AffixRemoveEvent removeEvent = new AffixEvent.AffixRemoveEvent(context);
            MinecraftForge.EVENT_BUS.post(removeEvent);

            if (removeEvent.isCanceled()) return;

            operation.remove(context);
        }
    }
}