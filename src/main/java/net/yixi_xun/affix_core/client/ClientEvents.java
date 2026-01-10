package net.yixi_xun.affix_core.client;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.AffixCoreMod;
import net.yixi_xun.affix_core.gui.screen.ResetAffixScreen;
import net.minecraft.client.Minecraft;

@Mod.EventBusSubscriber(modid = AffixCoreMod.MODID, value = Dist.CLIENT)
public class ClientEvents {
    
    // 用于存储待打开的重置界面参数
    private static ItemStack pendingItemStack;
    private static int pendingSlotIndex = -1;
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // 当客户端处于下一刻时打开界面
        if (pendingItemStack != null && Minecraft.getInstance().player != null && 
            Minecraft.getInstance().screen == null) {
            Minecraft.getInstance().setScreen(
                new ResetAffixScreen(pendingItemStack, pendingSlotIndex)
            );
            // 重置待处理参数
            pendingItemStack = null;
            pendingSlotIndex = -1;
        }
    }
    
    // 公共方法用于打开重置界面
    public static void openResetAffixScreen(ItemStack itemStack, int slotIndex) {
        pendingItemStack = itemStack;
        pendingSlotIndex = slotIndex;
    }
}