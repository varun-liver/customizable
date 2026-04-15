package com.customizable.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectedFilePacket {
    private final String filePath;

    public SelectedFilePacket(String filePath) {
        this.filePath = filePath;
    }

    public SelectedFilePacket(FriendlyByteBuf buffer) {
        this.filePath = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.filePath);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            // #region agent log
            boolean mainDisc = false, offDisc = false, applied = false;
            // #endregion
            if (player != null) {

                ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
                // #region agent log
                mainDisc = stack.getItem() instanceof com.customizable.item.CustomMusicDiscItem;
                // #endregion
                // Simple check for the custom disc, we could check both hands
                if (!(stack.getItem() instanceof com.customizable.item.CustomMusicDiscItem)) {
                    stack = player.getItemInHand(InteractionHand.OFF_HAND);
                }
                // #region agent log
                offDisc = stack.getItem() instanceof com.customizable.item.CustomMusicDiscItem;
                // #endregion

                if (stack.getItem() instanceof com.customizable.item.CustomMusicDiscItem) {
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.putString("SelectedFile", this.filePath);

                    // Update display name for feedback?
                    stack.setHoverName(net.minecraft.network.chat.Component.literal("Music Disc: " + getFileName(this.filePath)));
                    // #region agent log
                    applied = true;
                    // #endregion
                } else {

                }
            }
            // #region agent log
            String d = com.customizable.debug.DebugNdjsonLog.pathFieldsJson(this.filePath);
            d = d.substring(0, d.length() - 1) + ",\"playerNull\":" + (player == null)
                    + ",\"mainHandDisc\":" + mainDisc + ",\"effectiveOffHandDisc\":" + offDisc + ",\"nbtApplied\":" + applied + "}";
            com.customizable.debug.DebugNdjsonLog.log("D1", "SelectedFilePacket.handle", "server file pick result", d);
            // #endregion
        });
        return true;
    }

    private String getFileName(String path) {
        int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
    }
}
