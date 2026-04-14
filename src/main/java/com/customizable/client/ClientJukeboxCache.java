package com.customizable.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientJukeboxCache {
    private static final Map<BlockPos, ItemStack> CACHE = new ConcurrentHashMap<>();

    public static void store(BlockPos pos, ItemStack stack) {
        if (pos == null) return;
        if (stack == null) {
            CACHE.remove(pos);
            return;
        }
        CACHE.put(pos.immutable(), stack.copy());
    }

    public static ItemStack get(BlockPos pos) {
        ItemStack s = CACHE.get(pos);
        return s == null ? ItemStack.EMPTY : s.copy();
    }

    public static void remove(BlockPos pos) {
        CACHE.remove(pos);
    }

    public static void clearAll() {
        CACHE.clear();
    }
}
