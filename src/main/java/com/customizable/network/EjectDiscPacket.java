package com.customizable.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EjectDiscPacket {
    private final BlockPos pos;

    public EjectDiscPacket(BlockPos pos) {
        this.pos = pos;
    }

    public EjectDiscPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Safely get sender and level
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            Level level = sender.level();
            if (level == null) {
                return;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof JukeboxBlockEntity jukebox) {

                try {
                    var state = level.getBlockState(pos);
                    boolean hasRecord = state.is(net.minecraft.world.level.block.Blocks.JUKEBOX) && state.getValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD);
                    net.minecraft.world.item.ItemStack disc = jukebox.getItem(0);

                    if (!disc.isEmpty()) {
                        net.minecraft.world.item.ItemStack toDrop = disc.copy();
                        // Clear server jukebox slot and mark changed so clients get the update
                        jukebox.setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
                        com.customizable.JukeboxRecordStorage.remove(pos);
                        jukebox.setChanged();
                        // Update block state and notify clients explicitly
                        var newState = state.setValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD, false);
                        level.setBlock(pos, newState, 3);
                        level.sendBlockUpdated(pos, state, newState, 3);

                        // Spawn ItemEntity with preserved NBT and a small pickup delay
                        net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, toDrop);
                        entity.setPickUpDelay(10);
                        boolean added = level.addFreshEntity(entity);
                        level.levelEvent(null, 1011, pos, 0);

                        try { com.customizable.network.ModMessages.sendToAll(new com.customizable.network.DiscInfoResponsePacket(pos, net.minecraft.world.item.ItemStack.EMPTY)); com.customizable.network.ModMessages.sendToAll(new com.customizable.network.StopPlaybackPacket(pos)); } catch (Exception ignored) {}
                    } else {
                        // Try to recover from stored data in the server-side map
                        net.minecraft.world.item.ItemStack toDrop = com.customizable.JukeboxRecordStorage.retrieveAndRemove(pos);
                        if (!toDrop.isEmpty()) {
                            jukebox.setChanged();
                            var newState = state.setValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD, false);
                            level.setBlock(pos, newState, 3);
                            level.sendBlockUpdated(pos, state, newState, 3);

                            net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, toDrop);
                            entity.setPickUpDelay(10);
                            boolean added = level.addFreshEntity(entity);
                            level.levelEvent(null, 1011, pos, 0);

                            try { com.customizable.network.ModMessages.sendToAll(new com.customizable.network.DiscInfoResponsePacket(pos, net.minecraft.world.item.ItemStack.EMPTY)); com.customizable.network.ModMessages.sendToAll(new com.customizable.network.StopPlaybackPacket(pos)); } catch (Exception ignored) {}
                        } else if (hasRecord) {
                            // Fallback to popOutRecord if no stored data is available but state says there is a record

                            jukebox.popOutRecord();
                            var newState = state.setValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD, false);
                            level.setBlock(pos, newState, 3);
                            level.sendBlockUpdated(pos, state, newState, 3);
                            level.levelEvent(null, 1011, pos, 0);

                        } else {

                        }
                    }
                } catch (Exception e) {
                }
                // Ensure clients are cleared/stopped even if nothing was dropped
                try {
                    com.customizable.JukeboxRecordStorage.remove(pos);
                    com.customizable.network.ModMessages.sendToAll(new com.customizable.network.DiscInfoResponsePacket(pos, net.minecraft.world.item.ItemStack.EMPTY));
                    com.customizable.network.ModMessages.sendToAll(new com.customizable.network.StopPlaybackPacket(pos));
                } catch (Exception ignored) {}
            } else {
                try { com.customizable.network.ModMessages.sendToAll(new com.customizable.network.DiscInfoResponsePacket(pos, net.minecraft.world.item.ItemStack.EMPTY)); com.customizable.network.ModMessages.sendToAll(new com.customizable.network.StopPlaybackPacket(pos)); } catch (Exception ignored) {}
            }
        });
        return true;
    }
}
