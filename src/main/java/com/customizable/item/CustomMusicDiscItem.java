package com.customizable.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Item;
import com.customizable.customizable;
import com.customizable.network.ModMessages;
import com.customizable.network.SelectedFilePacket;

public class CustomMusicDiscItem extends RecordItem {
    public CustomMusicDiscItem(Properties properties) {
        super(15, customizable.DUMMY_MUSIC.get(), properties, 72000);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        // #region agent log
        com.customizable.debug.DebugNdjsonLog.log(
                "D4",
                "CustomMusicDiscItem.useOn",
                "enter",
                "{\"isJukebox\":" + state.is(Blocks.JUKEBOX) + ",\"hasRecord\":" + (state.is(Blocks.JUKEBOX) && state.getValue(JukeboxBlock.HAS_RECORD)) + ",\"client\":" + level.isClientSide + "}");
        // #endregion
        
        if (state.is(Blocks.JUKEBOX) && !state.getValue(JukeboxBlock.HAS_RECORD)) {
            ItemStack stack = context.getItemInHand();
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof JukeboxBlockEntity jukebox) {
                    // Move the item into the jukebox slot and preserve its NBT in the server-side storage map
                    net.minecraft.world.item.ItemStack moved = stack.split(1);
                    jukebox.setItem(0, moved);
                    try {
                        if (!moved.isEmpty()) {
                            com.customizable.JukeboxRecordStorage.store(pos, moved);
                        }
                    } catch (Exception e) {

                    }
                    // Mark tile changed and notify clients so the client can see the item/NBT in the jukebox slot
                    jukebox.setChanged();
                    var newState = state.setValue(JukeboxBlock.HAS_RECORD, true);
                    level.setBlock(pos, newState, 3);
                    level.sendBlockUpdated(pos, state, newState, 3);
                    level.levelEvent(null, 1010, pos, Item.getId(moved.getItem()));
                    // Broadcast the jukebox item to clients so they immediately get the NBT and can start playback
                    try {
                        // #region agent log
                        boolean hasSf = !moved.isEmpty() && moved.hasTag() && moved.getTag().contains("SelectedFile");
                        com.customizable.debug.DebugNdjsonLog.log(
                                "D5",
                                "CustomMusicDiscItem.useOn",
                                "server after jukebox insert",
                                com.customizable.debug.DebugNdjsonLog.mergeObjects(
                                        "{\"hasSelectedFile\":" + hasSf + ",\"stackEmpty\":" + moved.isEmpty() + "}",
                                        hasSf ? com.customizable.debug.DebugNdjsonLog.pathFieldsJson(moved.getTag().getString("SelectedFile")) : "{}"));
                        // #endregion
                        com.customizable.network.ModMessages.sendToAll(new com.customizable.network.DiscInfoResponsePacket(pos, moved));
                    } catch (Exception e) {
                        // #region agent log
                        com.customizable.debug.DebugNdjsonLog.log(
                                "D5",
                                "CustomMusicDiscItem.useOn",
                                "sendToAll failed",
                                com.customizable.debug.DebugNdjsonLog.throwableFields(e));
                        // #endregion
                    }
                } else {
                    // #region agent log
                    com.customizable.debug.DebugNdjsonLog.log("D4", "CustomMusicDiscItem.useOn", "no jukebox block entity", "{}");
                    // #endregion
                }
            } else {
                if (stack.hasTag() && stack.getTag().contains("SelectedFile")) {
                    String path = stack.getTag().getString("SelectedFile");

                    com.customizable.client.MP3MusicManager.play(pos, path);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // #region agent log
        com.customizable.debug.DebugNdjsonLog.log("D4", "CustomMusicDiscItem.useOn", "pass", "{}");
        // #endregion
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                openFilePicker();
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    private void openFilePicker() {
        new Thread(() -> {
            org.lwjgl.PointerBuffer filters = org.lwjgl.system.MemoryUtil.memAllocPointer(1);
            filters.put(org.lwjgl.system.MemoryUtil.memUTF8("*.mp3"));
            filters.flip();
            String result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Select a .mp3 file", "", filters, "MP3 Files", false);
            org.lwjgl.system.MemoryUtil.memFree(filters);

            if (result != null) {
                java.io.File file = new java.io.File(result);
                if (file.exists()) {
                    ModMessages.sendToServer(new SelectedFilePacket(file.getAbsolutePath()));
                }
            }
        }).start();
    }
}
