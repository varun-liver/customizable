package com.customizable.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DiscInfoResponsePacket {
    private final BlockPos pos;
    private final net.minecraft.world.item.ItemStack stack;

    public DiscInfoResponsePacket(BlockPos pos, net.minecraft.world.item.ItemStack stack) {
        this.pos = pos;
        this.stack = stack.copy();
    }

    public DiscInfoResponsePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.stack = buf.readItem();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeItem(this.stack);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            // Client-side: cache and update client jukebox block entity
            com.customizable.client.ClientJukeboxCache.store(pos, stack);
            try {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level != null) {
                    var be = mc.level.getBlockEntity(pos);
                    if (be instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jb) {
                        jb.setItem(0, stack);
                        jb.setChanged();
                    }
                }
                // If the stack has the SelectedFile tag, attempt to start playback immediately on the client
                if (stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag().contains("SelectedFile")) {                    String path = stack.getTag().getString("SelectedFile");
                    // Clear any suppression for this pos since server explicitly sent the item
                    try { com.customizable.client.ClientPlaybackSuppress.clear(pos); } catch (Exception ignored) {}
                    if (!com.customizable.client.MP3MusicManager.isPlaying(pos)) {
                        com.customizable.client.MP3MusicManager.play(pos, path);
                    } else {
                        // already playing, ignore
                    }
                } else {
                    // empty or no SelectedFile -> clear client cache and set suppression for a short while to avoid immediate restart
                    try {
                        com.customizable.client.ClientJukeboxCache.remove(pos);
                        com.customizable.client.ClientPlaybackSuppress.suppress(pos, 2000);
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
            }
        });
        return true;
    }
}
