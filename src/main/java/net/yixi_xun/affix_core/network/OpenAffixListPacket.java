package net.yixi_xun.affix_core.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.yixi_xun.affix_core.gui.screen.AffixListScreen;

import java.util.function.Supplier;

public class OpenAffixListPacket {
    private final ItemStack itemStack;

    public OpenAffixListPacket(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public OpenAffixListPacket(FriendlyByteBuf buf) {
        this.itemStack = buf.readItem();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeItem(itemStack);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new AffixListScreen(
            itemStack
        )));
    }
}