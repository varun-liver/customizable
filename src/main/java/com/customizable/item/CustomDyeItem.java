package com.customizable.item;

import com.customizable.network.ModMessages;
import com.customizable.network.UpdateDyeColorPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.util.List;

public class CustomDyeItem extends Item {
    public CustomDyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            if (level.isClientSide()) {
                int currentColor = getColor(stack);
                openColorPicker(String.format("#%06X", currentColor));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                com.mojang.logging.LogUtils.getLogger().info("Universal Dye: Shift-Right Click detected. Hand: " + hand);
                int currentColor = getColor(stack);
                openColorPicker(String.format("#%06X", currentColor));
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.consume(stack);
        }
        return super.use(level, player, hand);
    }

    private void openColorPicker(String initialHex) {
        new Thread(() -> {
            try {
                com.mojang.logging.LogUtils.getLogger().info("Universal Dye: Thread started. Initial hex: " + initialHex);
                
                // TinyFileDialogs might need a buffer for RGB even if we use hex string
                java.nio.ByteBuffer rgb = org.lwjgl.system.MemoryUtil.memAlloc(3);
                // Extract R, G, B from hex for the initial color buffer
                int r = Integer.parseInt(initialHex.substring(1, 3), 16);
                int g = Integer.parseInt(initialHex.substring(3, 5), 16);
                int b = Integer.parseInt(initialHex.substring(5, 7), 16);
                rgb.put(0, (byte)r);
                rgb.put(1, (byte)g);
                rgb.put(2, (byte)b);

                String result = TinyFileDialogs.tinyfd_colorChooser("Select Dye Color", initialHex, rgb, rgb);
                org.lwjgl.system.MemoryUtil.memFree(rgb);
                
                if (result != null) {
                    com.mojang.logging.LogUtils.getLogger().info("Universal Dye: Color picked: " + result);
                    int color = Integer.parseInt(result.substring(1), 16);
                    ModMessages.sendToServer(new UpdateDyeColorPacket(color));
                } else {
                    com.mojang.logging.LogUtils.getLogger().info("Universal Dye: Color picker was cancelled or failed.");
                }
            } catch (Exception e) {
                com.mojang.logging.LogUtils.getLogger().error("Universal Dye: Error in color picker thread", e);
            }
        }).start();
    }

    public static int getColor(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("DyeColor")) {
            return tag.getInt("DyeColor");
        }
        return 0xFFFFFF; // Default white
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int color = getColor(stack);
        tooltip.add(Component.literal("Color: #" + String.format("%06X", color)).withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.literal("Shift-Right Click to change color").withStyle(net.minecraft.ChatFormatting.BLUE));
    }
}
