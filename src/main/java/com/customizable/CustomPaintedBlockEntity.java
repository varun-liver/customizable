package com.customizable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class CustomPaintedBlockEntity extends BlockEntity {
    private int color = 0xFFFFFF;
    private String baseBlockId = "minecraft:white_wool";

    public CustomPaintedBlockEntity(BlockPos pos, BlockState state) {
        super(customizable.CUSTOM_PAINTED_BE.get(), pos, state);
    }

    public void setData(int color, String baseBlockId) {
        this.color = color;
        this.baseBlockId = baseBlockId;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getColor() { return color; }
    public String getBaseBlockId() { return baseBlockId; }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Color")) this.color = tag.getInt("Color");
        if (tag.contains("BaseBlock")) this.baseBlockId = tag.getString("BaseBlock");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Color", this.color);
        tag.putString("BaseBlock", this.baseBlockId);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        load(pkt.getTag());
    }
}
