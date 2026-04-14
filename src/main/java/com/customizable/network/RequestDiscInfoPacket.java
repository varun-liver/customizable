package com.customizable.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestDiscInfoPacket {
    private final BlockPos pos;

    public RequestDiscInfoPacket(BlockPos pos) {
        this.pos = pos;
    }

    public RequestDiscInfoPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            // Server-side: find stored stack and send back to requester
            var sender = ctx.getSender();
            if (sender == null) return;
            var level = sender.level();
            if (level == null) return;
            var be = level.getBlockEntity(pos);
            net.minecraft.world.item.ItemStack stack = net.minecraft.world.item.ItemStack.EMPTY;
            if (be instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jb) {
                stack = jb.getItem(0);
            }
            if (stack.isEmpty()) {
                stack = com.customizable.JukeboxRecordStorage.get(pos);
            }
            // send response to the player
            com.customizable.network.ModMessages.sendToPlayer(new DiscInfoResponsePacket(pos, stack), sender);
        });
        return true;
    }
}
