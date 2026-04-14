package com.customizable.network;

import com.customizable.item.CustomDyeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateDyeColorPacket {
    private final int color;

    public UpdateDyeColorPacket(int color) {
        this.color = color;
    }

    public UpdateDyeColorPacket(FriendlyByteBuf buffer) {
        this.color = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.color);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (!(stack.getItem() instanceof CustomDyeItem)) {
                    stack = player.getItemInHand(InteractionHand.OFF_HAND);
                }

                if (stack.getItem() instanceof CustomDyeItem) {
                    stack.getOrCreateTag().putInt("DyeColor", this.color);
                }
            }
        });
        return true;
    }
}
