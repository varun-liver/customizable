package com.customizable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class CustomPaintingBlock extends Block implements EntityBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty HAS_PAINTING = net.minecraft.world.level.block.state.properties.BlockStateProperties.HAS_RECORD; // Reusing a boolean property

    public CustomPaintingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HAS_PAINTING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_PAINTING);
    }

    private static final net.minecraft.world.phys.shapes.VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
    private static final net.minecraft.world.phys.shapes.VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
    private static final net.minecraft.world.phys.shapes.VoxelShape WEST_AABB = Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final net.minecraft.world.phys.shapes.VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 0.0D, 16.0D);

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        if (!state.getValue(HAS_PAINTING)) {
            return net.minecraft.world.phys.shapes.Shapes.block();
        }
        switch (state.getValue(FACING)) {
            case NORTH: return NORTH_AABB;
            case SOUTH: return SOUTH_AABB;
            case WEST: return WEST_AABB;
            case EAST: return EAST_AABB;
            default: return SOUTH_AABB;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CustomPaintingBlockEntity(pos, state);
    }
}
