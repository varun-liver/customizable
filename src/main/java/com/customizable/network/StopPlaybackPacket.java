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
                // #region agent log
                com.customizable.debug.DebugNdjsonLog.log(
                        "D8",
                        "StopPlaybackPacket.handle",
                        "received stop packet",
                        "{\"pos\":\"" + this.pos.toShortString() + "\"}");
                // #endregion
                // Client-side only: stop playback at this position
                if (net.minecraft.client.Minecraft.getInstance().level != null) {
                    com.customizable.client.MP3MusicManager.stop(this.pos);
                }
                // also clear caches and suppression for this specific position
                try { com.customizable.client.ClientJukeboxCache.remove(this.pos); } catch (Exception ignored) {}
                try { com.customizable.client.ClientPlaybackSuppress.clear(this.pos); } catch (Exception ignored) {}
            } catch (Exception e) { }
        });
        return true;
    }
}
