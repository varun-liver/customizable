package com.customizable.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StopPlaybackPacket {
    private final BlockPos pos;

    public StopPlaybackPacket(BlockPos pos) {
        this.pos = pos;
    }

    public StopPlaybackPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            try {
                // Client-side only: stop playback at this position — use stopAll as a stronger guarantee
                if (net.minecraft.client.Minecraft.getInstance().level != null) {
                    com.customizable.client.MP3MusicManager.stopAll();
                }
                // also clear caches and suppression
                try { com.customizable.client.ClientJukeboxCache.clearAll(); } catch (Exception ignored) {}
                try { com.customizable.client.ClientPlaybackSuppress.clearAll(); } catch (Exception ignored) {}
            } catch (Exception e) { }
        });
        return true;
    }
}
