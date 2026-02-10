package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.yixi_xun.affix_core.affix.AffixContext;
import net.yixi_xun.affix_core.api.AffixEvent.CustomMessageEvent;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;

public class CustomMessageOperation implements IOperation{
    private final String message;
    private final String targetString;

    public CustomMessageOperation(String message, String targetString) {
        this.message = message;
        this.targetString = targetString;
    }

    @Override
    public void apply(AffixContext context) {
        var target = targetString.equals("self") ? context.getOwner() : context.getTarget();
        CustomMessageEvent event = new CustomMessageEvent(target, message);
        EVENT_BUS.post(event);
    }

    @Override
    public void remove(AffixContext context) {
        // 不需要移除
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("Target", targetString);
        nbt.putString("Message", message);
        return nbt;
    }

    @Override
    public String getType() {
        return "custom_message";
    }

    /**
     * 工厂方法，从NBT创建CustomMessageOperation
     */
    public static CustomMessageOperation fromNBT(CompoundTag nbt) {
        String message = nbt.contains("Message") ? nbt.getString("Message") : "default_message";
        String target = nbt.contains("Target") ? nbt.getString("Target") : "self";

        return new CustomMessageOperation(message, target);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("custom_message", CustomMessageOperation::fromNBT);
    }
}
