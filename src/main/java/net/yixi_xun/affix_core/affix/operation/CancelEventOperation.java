package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.eventbus.api.Event;
import net.yixi_xun.affix_core.affix.AffixContext;

import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluate;

/**
 * 伤害操作，修改造成的伤害
 */
public class CancelEventOperation implements IOperation {

    private final String cancel;

    public CancelEventOperation(String cancel) {
        this.cancel = cancel;
    }

    @Override
    public void apply(AffixContext context) {
        double result = evaluate(cancel, context.getVariables());
        Event event = context.getEvent();
        if (event.isCancelable()) {
            event.setCanceled(result > 1e-10);
        } else if (event.hasResult()) {
            event.setResult(result > 1e-10 ? Event.Result.DENY : Event.Result.DEFAULT);
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
        nbt.putString("Cancel", cancel);
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
