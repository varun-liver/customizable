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
            if (player != null) {

                ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
                // Simple check for the custom disc, we could check both hands
                if (!(stack.getItem() instanceof com.customizable.item.CustomMusicDiscItem)) {
                    stack = player.getItemInHand(InteractionHand.OFF_HAND);
                }

                if (stack.getItem() instanceof com.customizable.item.CustomMusicDiscItem) {
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.putString("SelectedFile", this.filePath);

                    // Update display name for feedback?
                    stack.setHoverName(net.minecraft.network.chat.Component.literal("Music Disc: " + getFileName(this.filePath)));
                } else {

                }
            }
        });
        return true;
    }

    private String getFileName(String path) {
        int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
    }
}
