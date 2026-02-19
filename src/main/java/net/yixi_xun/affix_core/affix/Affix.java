package net.yixi_xun.affix_core.affix;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.common.MinecraftForge;
import net.yixi_xun.affix_core.affix.operation.IOperation;
import net.yixi_xun.affix_core.affix.operation.OperationManager;
import net.yixi_xun.affix_core.api.AffixEvent.AffixExecuteEvent;
import net.yixi_xun.affix_core.api.AffixEvent.AffixRemoveEvent;
import net.yixi_xun.affix_core.curios.CuriosSlotType;

import java.util.UUID;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;
import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluateCondition;

/**
 * 表示一个词缀，包含触发器、条件、操作、冷却时间和槽位限制等信息
 */
public class Affix {
    private final UUID uuid;
    private final String trigger;
    private final String condition;
    private final IOperation operation;
    private final Long cooldown;
    private int triggerCount;
    private final EquipmentSlot slot;
    private final CuriosSlotType curiosSlot;
    private final long priority;

    /**
     * 从物品NBT中读取词缀信息
     */
    public Affix(UUID uuid, String trigger, String condition, IOperation operation, Long cooldown, int triggerCount, EquipmentSlot slot, CuriosSlotType curiosSlot, long priority) {
        this.uuid = uuid;
        this.trigger = trigger;
        this.condition = condition;
        this.operation = operation;
        this.cooldown = cooldown;
        this.triggerCount = triggerCount;
        this.slot = slot;
        this.curiosSlot = curiosSlot;
        this.priority = priority;
    }

    public static Affix fromNBT(CompoundTag nbt) {
        UUID uuid = nbt.contains("UUID") ? UUID.fromString(nbt.getString("UUID")) : UUID.randomUUID();
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
        CuriosSlotType curiosSlot = CuriosSlotType.ANY; // 默认值为任意槽位
        
        if (nbt.contains("Slot")) {
            if (nbt.getString("Slot") instanceof String) {
                String slotName = nbt.getString("Slot");
                // 首先尝试解析为Curios槽位
                curiosSlot = CuriosSlotType.fromString(slotName);
                
                // 如果不是Curios槽位，尝试解析为Vanilla槽位
                if (curiosSlot == CuriosSlotType.ANY) {
                    try {
                        slot = EquipmentSlot.byName(slotName.toLowerCase());
                    } catch (IllegalArgumentException ignored) {
                        // 如果无效槽位名，使用默认值
                    }
                }
            } else if (nbt.get("Slot") instanceof NumericTag) {
                // 如果是数字，尝试转换为EquipmentSlot
                int slotIndex = nbt.getInt("Slot");
                EquipmentSlot[] slots = EquipmentSlot.values();
                if (slotIndex >= 0 && slotIndex < slots.length) {
                    slot = slots[slotIndex];
                    curiosSlot = CuriosSlotType.fromEquipmentSlot(slot);
                } else {
                    slot = null; // 如果索引超出范围，使用默认值
                    curiosSlot = CuriosSlotType.ANY;
                }
            }
        }

        // 读取优先级
        long priority = nbt.contains("Priority") ? nbt.getLong("Priority") : 0L;

        return new Affix(uuid, trigger, condition, operation, cooldown, triggerCount, slot, curiosSlot, priority);
    }

    // Getter方法
    public UUID uuid() { return uuid; }
    public String trigger() { return trigger; }
    public String condition() { return condition; }
    public IOperation operation() { return operation; }
    public Long cooldown() { return cooldown; }
    public int triggerCount() { return triggerCount; }
    public EquipmentSlot slot() { return slot; }
    public CuriosSlotType curiosSlot() { return curiosSlot; }
    public long priority() { return priority; }

    /**
     * 将词缀保存到物品NBT中
     */
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();

        nbt.putString("UUID", uuid.toString());

        if (trigger != null) {
            nbt.putString("Trigger", trigger);
        }

        if (condition != null) {
            nbt.putString("Condition", condition);
        }

        if (operation != null) {
            nbt.put("Operation", operation.toNBT());
        }

        if (cooldown >= 0) {
            nbt.putLong("Cooldown", cooldown);
        }

        if (triggerCount >= 0) {
            nbt.putInt("TriggerCount", triggerCount);
        }

        // 保存槽位信息
        if (slot != null) {
            nbt.putString("Slot", slot.getName());
        }
        if (curiosSlot != null && !curiosSlot.isAnySlot()) {
            nbt.putString("Slot", curiosSlot.getSlotName());
        }

        nbt.putLong("Priority", priority);

        return nbt;
    }

    /**
     * 检查词缀是否在非指定槽位触发
     */
    public boolean triggerInInvalidSlot(EquipmentSlot equipmentSlot) {
        return triggerInInvalidSlot(equipmentSlot, null);
    }
    
    /**
     * 检查词缀是否在非指定槽位触发（支持Curios槽位）
     */
    public boolean triggerInInvalidSlot(EquipmentSlot equipmentSlot, String curiosSlotIdentifier) {
        // 如果是任意槽位，总是有效
        if (curiosSlot() == null || curiosSlot().isAnySlot()) {
            return slot() != null && slot() != equipmentSlot;
        }
        
        // 检查Curios槽位是否匹配
        if (curiosSlotIdentifier != null && !curiosSlotIdentifier.isEmpty()) {
            CuriosSlotType currentSlot = CuriosSlotType.fromString(curiosSlotIdentifier);
            return curiosSlot() != currentSlot;
        }
        
        // 回退到检查Vanilla槽位
        return slot() != null && slot() != equipmentSlot;
    }

    /**
     * 检查条件是否满足
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
            AffixExecuteEvent executeEvent = new AffixExecuteEvent(this, context);
            MinecraftForge.EVENT_BUS.post(executeEvent);

            if (executeEvent.isCanceled()) return;

            operation.apply(context);
            this.triggerCount++;
        }
    }

    /**
     * 执行词缀移除时操作
     */
    public void remove(AffixContext context) {
        if (operation != null) {
            // 触发词缀移除事件
            AffixRemoveEvent removeEvent = new AffixRemoveEvent(context);
            MinecraftForge.EVENT_BUS.post(removeEvent);

            if (removeEvent.isCanceled()) return;

            operation.remove(context);
        }
    }
}