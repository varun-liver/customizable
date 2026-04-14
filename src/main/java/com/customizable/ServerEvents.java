package com.customizable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.level.BlockEvent;

@Mod.EventBusSubscriber(modid = customizable.MODID)
public class ServerEvents {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        BlockPos pos = event.getPos();
        var state = level.getBlockState(pos);
        if (!state.is(Blocks.JUKEBOX)) return;
        // If the jukebox has a record and the player is not inserting a disc, treat this as an eject
        if (!state.getValue(JukeboxBlock.HAS_RECORD)) return;
        ItemStack held = event.getItemStack();
        if (held.getItem() instanceof com.customizable.item.CustomMusicDiscItem) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof JukeboxBlockEntity jukebox)) return;

        // Cancel the default interaction so we can handle eject and preserve NBT
        event.setCanceled(true);

        try {
            ItemStack disc = jukebox.getItem(0);
            ItemStack toDrop = ItemStack.EMPTY;
            if (!disc.isEmpty()) {
                toDrop = disc.copy();
            } else {
                toDrop = com.customizable.JukeboxRecordStorage.retrieveAndRemove(pos);
            }

            if (!toDrop.isEmpty()) {
                jukebox.setItem(0, ItemStack.EMPTY);
                com.customizable.JukeboxRecordStorage.remove(pos);
                jukebox.setChanged();
                var newState = state.setValue(JukeboxBlock.HAS_RECORD, false);
                level.setBlock(pos, newState, 3);
                level.sendBlockUpdated(pos, state, newState, 3);

                ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, toDrop);
                entity.setPickUpDelay(10);
                level.addFreshEntity(entity);
                level.levelEvent(null, 1010, pos, 0);
                try { com.customizable.network.ModMessages.sendToAll(new com.customizable.network.DiscInfoResponsePacket(pos, net.minecraft.world.item.ItemStack.EMPTY)); com.customizable.network.ModMessages.sendToAll(new com.customizable.network.StopPlaybackPacket(pos)); } catch (Exception ignored) {}
            } else {
                // fallback to vanilla behavior
                jukebox.popOutRecord();
            }
        } catch (Exception e) {
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        net.minecraft.world.level.LevelAccessor la = event.getLevel();
        if (!(la instanceof Level level)) return;
        if (level.isClientSide()) return;
        BlockPos pos = event.getPos();
        var state = level.getBlockState(pos);
        if (!state.is(Blocks.JUKEBOX)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof JukeboxBlockEntity jukebox)) return;

        try {
            ItemStack disc = jukebox.getItem(0);
            ItemStack toDrop = ItemStack.EMPTY;
            if (!disc.isEmpty()) {
                toDrop = disc.copy();
            } else {
                toDrop = com.customizable.JukeboxRecordStorage.retrieveAndRemove(pos);
            }

            if (!toDrop.isEmpty()) {
                jukebox.setItem(0, ItemStack.EMPTY);
                com.customizable.JukeboxRecordStorage.remove(pos);
                jukebox.setChanged();

                ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, toDrop);
                entity.setPickUpDelay(10);
                level.addFreshEntity(entity);
                try { com.customizable.network.ModMessages.sendToAll(new com.customizable.network.DiscInfoResponsePacket(pos, net.minecraft.world.item.ItemStack.EMPTY)); com.customizable.network.ModMessages.sendToAll(new com.customizable.network.StopPlaybackPacket(pos)); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
        }
    }
}