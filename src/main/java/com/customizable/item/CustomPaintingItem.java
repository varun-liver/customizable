package com.customizable.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.customizable.network.ModMessages;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CustomPaintingItem extends Item {
    public CustomPaintingItem(Properties properties) {
        super(properties);
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

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        // Only allow horizontal faces
        if (!face.getAxis().isHorizontal()) return InteractionResult.PASS;
        BlockPos placePos = pos.relative(face);
        if (!level.isEmptyBlock(placePos)) return InteractionResult.PASS;

        if (!level.isClientSide) {
            BlockState newState = com.customizable.customizable.CUSTOM_PAINTING_BLOCK.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, face.getOpposite());
            level.setBlock(placePos, newState, 3);
            BlockEntity be = level.getBlockEntity(placePos);
            if (be instanceof com.customizable.CustomPaintingBlockEntity cpbe) {
                ItemStack stack = context.getItemInHand();
                String path = "";
                int w = 1, h = 1;
                if (stack.hasTag() && stack.getTag().contains("SelectedPaintingFile")) {
                    path = stack.getTag().getString("SelectedPaintingFile");
                    w = stack.getTag().contains("PaintingWidth") ? stack.getTag().getInt("PaintingWidth") : 1;
                    h = stack.getTag().contains("PaintingHeight") ? stack.getTag().getInt("PaintingHeight") : 1;
                }
                cpbe.setData(path, w, h);
                // consume one
                stack.shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void openFilePicker() {
        new Thread(() -> {
            // Allow PNG files (expandable later)
            org.lwjgl.PointerBuffer filters = org.lwjgl.system.MemoryUtil.memAllocPointer(1);
            filters.put(org.lwjgl.system.MemoryUtil.memUTF8("*.png"));
            filters.flip();
            String result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Select an image file", "", filters, "PNG Files", false);
            org.lwjgl.system.MemoryUtil.memFree(filters);

            if (result != null) {
                java.io.File file = new java.io.File(result);
                if (file.exists()) {
                    // Ask for size as WxH, default 1x1
                    String sizeInput = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_inputBox("Painting Size", "Enter size as WxH (e.g. 1x1 or 2x3):", "1x1");
                    int w = 1, h = 1;
                    if (sizeInput != null && sizeInput.contains("x")) {
                        try {
                            String[] parts = sizeInput.split("x");
                            w = Integer.parseInt(parts[0].trim());
                            h = Integer.parseInt(parts[1].trim());
                        } catch (Exception ignored) {}
                    }
                    ModMessages.sendToServer(new com.customizable.network.SelectedPaintingPacket(file.getAbsolutePath(), w, h));
                }
            }
        }).start();
    }
}
