package net.yixi_xun.affix_core.affix;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.MinecraftForge;
import net.yixi_xun.affix_core.affix.operation.IOperation;
import net.yixi_xun.affix_core.affix.operation.OperationManager;
import net.yixi_xun.affix_core.api.AffixEvent.AffixExecuteEvent;
import net.yixi_xun.affix_core.api.AffixEvent.AffixRemoveEvent;

import java.util.UUID;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;
import static net.yixi_xun.affix_core.affix.AffixManager.AFFIX_TAG_KEY;
import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluateCondition;

/**
 * 表示一个词缀，包含触发器、条件、操作、冷却时间和槽位限制等信息
 */
public record Affix(UUID uuid, String trigger, String condition, IOperation operation, Long cooldown, int triggerCount,
                    String slot, long priority) {
    /**
     * 从物品NBT中读取词缀信息
     */
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

        // 读取槽位限制，默认为""（任意槽位）
        String slot = nbt.contains("Slot") ? nbt.getString("Slot").toLowerCase() : "";

        // 读取优先级
        long priority = nbt.contains("Priority") ? nbt.getLong("Priority") : 0L;

        return new Affix(uuid, trigger, condition, operation, cooldown, triggerCount, slot, priority);
    }

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
        nbt.putString("Slot", slot == null ? "" : slot);

        nbt.putLong("Priority", priority);

        return nbt;
    }

    /**
     * 检查词缀是否在非指定槽位触发
     */
    public boolean canTriggerInSlot(String triggerSlot) {
        if (slot.isEmpty()) return true;

        if (!triggerSlot.isEmpty()) {
            return slot.equals(triggerSlot);
        }

        return false;
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

            // 更新词缀触发次数
            context.getItemStack().getOrCreateTag().getList(AFFIX_TAG_KEY, Tag.TAG_COMPOUND)
                    .stream().filter(tag -> tag instanceof CompoundTag compoundTag && compoundTag.getString("UUID").equals(uuid.toString()))
                    .findFirst().ifPresent(tag -> ((CompoundTag)tag).putInt("TriggerCount", triggerCount + 1));
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