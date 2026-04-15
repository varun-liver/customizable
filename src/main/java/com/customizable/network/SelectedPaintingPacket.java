package com.customizable.network;

import com.customizable.item.CustomPaintingItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
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

    // Decoder constructor
    public SelectedPaintingPacket(FriendlyByteBuf buffer) {
        this.filePath = buffer.readUtf(32767); // Max string length
        this.width = buffer.readInt();
        this.height = buffer.readInt();
    }

    // Encoder method
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.filePath, 32767);
        buffer.writeInt(this.width);
        buffer.writeInt(this.height);
    }

    // Handler method
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                // #region agent log
                com.customizable.debug.DebugNdjsonLog.log("P1", "SelectedPaintingPacket.handle", "no player", "{}");
                // #endregion
                return;
            }

            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(stack.getItem() instanceof CustomPaintingItem)) {
                stack = player.getItemInHand(InteractionHand.OFF_HAND);
            }

            boolean applied = false;
            if (stack.getItem() instanceof CustomPaintingItem) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putString("SelectedPaintingFile", this.filePath);
                tag.putInt("PaintingWidth", this.width);
                tag.putInt("PaintingHeight", this.height);
                stack.setHoverName(Component.literal("Painting: " + getFileName(this.filePath)));
                applied = true;
            }
            // #region agent log
            com.customizable.debug.DebugNdjsonLog.log(
                    "P1",
                    "SelectedPaintingPacket.handle",
                    "server selected png",
                    com.customizable.debug.DebugNdjsonLog.mergeObjects(
                            "{\"applied\":" + applied + ",\"w\":" + this.width + ",\"h\":" + this.height + "}",
                            com.customizable.debug.DebugNdjsonLog.pathFieldsJson(this.filePath)));
            // #endregion
        });
        return true;
    }

    private static String getFileName(String path) {
        int lastSeparator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
    }
}
