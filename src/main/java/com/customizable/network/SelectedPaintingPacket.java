package com.customizable.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectedPaintingPacket {
    private final String filePath;
    private final int width;
    private final int height;

    public SelectedPaintingPacket(String filePath, int width, int height) {
        this.filePath = filePath;
        this.width = width;
        this.height = height;
    }

    public SelectedPaintingPacket(FriendlyByteBuf buffer) {
        this.filePath = buffer.readUtf();
        this.width = buffer.readInt();
        this.height = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.filePath);
        buffer.writeInt(this.width);
        buffer.writeInt(this.height);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (!(stack.getItem() instanceof com.customizable.item.CustomPaintingItem)) {
                    stack = player.getItemInHand(InteractionHand.OFF_HAND);
                }

                if (stack.getItem() instanceof com.customizable.item.CustomPaintingItem) {
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.putString("SelectedPaintingFile", this.filePath);
                    tag.putInt("PaintingWidth", this.width);
                    tag.putInt("PaintingHeight", this.height);

                    stack.setHoverName(net.minecraft.network.chat.Component.literal("Painting: " + getFileName(this.filePath) + " (" + this.width + "x" + this.height + ")"));
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
