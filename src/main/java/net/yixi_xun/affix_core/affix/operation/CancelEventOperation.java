package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.eventbus.api.Event;
import net.yixi_xun.affix_core.AffixCoreMod;
import net.yixi_xun.affix_core.affix.AffixContext;

/**
 * 事件取消操作，用于取消或拒绝事件
 */
public class CancelEventOperation extends BaseOperation {

    private final String cancelCondition;

    public CancelEventOperation(String cancelCondition) {
        this.cancelCondition = cancelCondition != null ? cancelCondition : "false";
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }
        
        try {
            Event event = context.getEvent();
            if (event == null) {
                return;
            }
            
            double result = evaluateOrDefaultValue(cancelCondition, context.getVariables(), 0.0);
            boolean shouldCancel = result > 1e-10;
            
            if (event.isCancelable()) {
                event.setCanceled(shouldCancel);
            } else if (event.hasResult()) {
                event.setResult(shouldCancel ? Event.Result.DENY : Event.Result.DEFAULT);
            }
        } catch (Exception e) {
            AffixCoreMod.LOGGER.error("处理事件取消时发生错误", e);
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
        nbt.putString("Cancel", cancelCondition);
        return nbt;
    }

    @Override
    public String getType() {
        return "cancel_event";
    }

    /**
     * 工厂方法，从NBT创建CancelEventOperation
     */
    public static CancelEventOperation fromNBT(CompoundTag nbt) {
        String cancel = nbt.contains("Cancel") ? nbt.getString("Cancel") : "false";

        return new CancelEventOperation(cancel);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("cancel_event", CancelEventOperation::fromNBT);
    }
}
