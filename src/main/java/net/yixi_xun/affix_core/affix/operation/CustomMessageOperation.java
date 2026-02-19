package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.nbt.CompoundTag;
import net.yixi_xun.affix_core.AffixCoreMod;
import net.yixi_xun.affix_core.affix.AffixContext;
import net.yixi_xun.affix_core.api.AffixEvent.CustomMessageEvent;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;

/**
 * 自定义消息操作，用于发送自定义消息事件
 */
public class CustomMessageOperation extends BaseOperation {
    private final String message;
    private final String target;

    public CustomMessageOperation(String message, String target) {
        this.message = message != null ? message : "default_message";
        this.target = target != null ? target : "self";
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null) {
            return;
        }
        
        try {
            var targetEntity = getTargetEntity(context, target);
            if (targetEntity != null) {
                CustomMessageEvent event = new CustomMessageEvent(targetEntity, message);
                EVENT_BUS.post(event);
            }
        } catch (Exception e) {
            AffixCoreMod.LOGGER.error("发送自定义消息时发生错误: {}", message, e);
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
        nbt.putString("Target", target);
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
