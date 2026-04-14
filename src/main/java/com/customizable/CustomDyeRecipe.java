package com.customizable;

import com.customizable.item.CustomDyeItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class CustomDyeRecipe extends CustomRecipe {
    public CustomDyeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private boolean isWhiteBlock(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return false;
        String name = rl.getPath();
        return name.startsWith("white_") && (
            name.endsWith("_wool") || 
            name.endsWith("_concrete") || 
            name.endsWith("_concrete_powder") || 
            name.endsWith("_terracotta") || 
            name.endsWith("_carpet") || 
            name.endsWith("_glass") || 
            name.endsWith("_stained_glass") || 
            name.endsWith("_shulker_box")
        );
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        int dyes = 0;
        int blocks = 0;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof CustomDyeItem) {
                    dyes++;
                } else if (isWhiteBlock(stack)) {
                    blocks++;
                } else {
                    return false;
                }
            }
        }

        return dyes == 1 && blocks == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        ItemStack dye = ItemStack.EMPTY;
        ItemStack block = ItemStack.EMPTY;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof CustomDyeItem) {
                    dye = stack;
                } else if (isWhiteBlock(stack)) {
                    block = stack;
                }
            }
        }

        if (!dye.isEmpty() && !block.isEmpty()) {
            ItemStack result = new ItemStack(customizable.CUSTOM_PAINTED_ITEM.get());
            int color = CustomDyeItem.getColor(dye);
            ResourceLocation blockRl = ForgeRegistries.ITEMS.getKey(block.getItem());
            String blockId = blockRl != null ? blockRl.toString() : "minecraft:white_wool";
            
            CompoundTag beTag = result.getOrCreateTagElement("BlockEntityTag");
            beTag.putInt("Color", color);
            beTag.putString("BaseBlock", blockId);
            
            // Set hover name based on base block
            String baseName = block.getHoverName().getString().replace("White ", "");
            result.setHoverName(net.minecraft.network.chat.Component.literal("Painted " + baseName).withStyle(net.minecraft.ChatFormatting.RESET));
            
            return result;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return customizable.CUSTOM_DYE_RECIPE_SERIALIZER.get();
    }
}
