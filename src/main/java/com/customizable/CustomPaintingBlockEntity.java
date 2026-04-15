package com.customizable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

public class CustomPaintingBlockEntity extends BlockEntity {
    private String filePath = null;
    private int width = 1;
    private int height = 1;

    public CustomPaintingBlockEntity(BlockPos pos, BlockState state) {
        super(customizable.CUSTOM_PAINTING_BE.get(), pos, state);
    }

    public void setData(String filePath, int width, int height) {
        this.filePath = filePath;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        setChanged();
        if (level != null) {
            BlockState oldState = getBlockState();
            boolean hasP = filePath != null && !filePath.isEmpty();
            BlockState newState = oldState;
            if (oldState.hasProperty(CustomPaintingBlock.HAS_PAINTING)) {
                newState = oldState.setValue(CustomPaintingBlock.HAS_PAINTING, hasP);
                if (newState != oldState) {
                    level.setBlock(worldPosition, newState, 3);
                }
            }
            level.sendBlockUpdated(worldPosition, oldState, newState, 3);
        }
    }

    public String getFilePath() { return this.filePath; }
    public int getWidth() { return this.width; }
    public int getHeight() { return this.height; }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("SelectedPaintingFile")) this.filePath = tag.getString("SelectedPaintingFile");
        if (tag.contains("PaintingWidth")) this.width = tag.getInt("PaintingWidth");
        if (tag.contains("PaintingHeight")) this.height = tag.getInt("PaintingHeight");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.filePath != null) tag.putString("SelectedPaintingFile", this.filePath);
        tag.putInt("PaintingWidth", this.width);
        tag.putInt("PaintingHeight", this.height);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
            if (level != null && level.isClientSide) {
                com.mojang.logging.LogUtils.getLogger().info("Received painting data on client: {} ({}x{})", filePath, width, height);
                // #region agent log
                com.customizable.debug.DebugNdjsonLog.log(
                        "P3",
                        "CustomPaintingBlockEntity.onDataPacket",
                        "client be nbt",
                        com.customizable.debug.DebugNdjsonLog.mergeObjects(
                                "{\"w\":" + width + ",\"h\":" + height + "}",
                                com.customizable.debug.DebugNdjsonLog.pathFieldsJson(this.filePath)));
                // #endregion
            }
        }
    }
}
