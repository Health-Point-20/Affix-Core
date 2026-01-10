package net.yixi_xun.affix_core.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.yixi_xun.affix_core.AffixCoreMod;

import java.util.Optional;

public class NetworkManager {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(AffixCoreMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, UpdateAffixC2SPacket.class,
            UpdateAffixC2SPacket::toBytes,
            UpdateAffixC2SPacket::new,
            UpdateAffixC2SPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, OpenAffixListPacket.class,
            OpenAffixListPacket::toBytes,
            OpenAffixListPacket::new,
            OpenAffixListPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}