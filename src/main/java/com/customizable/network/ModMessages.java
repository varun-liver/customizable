package com.customizable.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import com.customizable.customizable;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(customizable.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(SelectedPaintingPacket.class, id())
                .encoder(SelectedPaintingPacket::encode)
                .decoder(SelectedPaintingPacket::new)
                .consumerMainThread(SelectedPaintingPacket::handle)
                .add();

        net.messageBuilder(UpdateDyeColorPacket.class, id())
                .encoder(UpdateDyeColorPacket::encode)
                .decoder(UpdateDyeColorPacket::new)
                .consumerMainThread(UpdateDyeColorPacket::handle)
                .add();

        net.messageBuilder(SelectedFilePacket.class, id())
                .encoder(SelectedFilePacket::encode)
                .decoder(SelectedFilePacket::new)
                .consumerMainThread(SelectedFilePacket::handle)
                .add();

        net.messageBuilder(DiscInfoResponsePacket.class, id())
                .encoder(DiscInfoResponsePacket::encode)
                .decoder(DiscInfoResponsePacket::new)
                .consumerMainThread(DiscInfoResponsePacket::handle)
                .add();

        net.messageBuilder(RequestDiscInfoPacket.class, id())
                .encoder(RequestDiscInfoPacket::encode)
                .decoder(RequestDiscInfoPacket::new)
                .consumerMainThread(RequestDiscInfoPacket::handle)
                .add();

        net.messageBuilder(EjectDiscPacket.class, id())
                .encoder(EjectDiscPacket::encode)
                .decoder(EjectDiscPacket::new)
                .consumerMainThread(EjectDiscPacket::handle)
                .add();

        net.messageBuilder(StopPlaybackPacket.class, id())
                .encoder(StopPlaybackPacket::encode)
                .decoder(StopPlaybackPacket::new)
                .consumerMainThread(StopPlaybackPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        if (player == null) return;
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAll(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
