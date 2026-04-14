package com.customizable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JukeboxRecordStorage {
    private static final Map<BlockPos, ItemStack> STORE = new ConcurrentHashMap<>();

    public static void store(BlockPos pos, ItemStack stack) {
        if (pos == null || stack == null) return;
        STORE.put(pos.immutable(), stack.copy());
    }

    public static ItemStack retrieveAndRemove(BlockPos pos) {
        ItemStack s = STORE.remove(pos);
        return s == null ? ItemStack.EMPTY : s.copy();
    }

    public static ItemStack get(BlockPos pos) {
        ItemStack s = STORE.get(pos);
        return s == null ? ItemStack.EMPTY : s.copy();
    }

    public static void remove(BlockPos pos) {
        STORE.remove(pos);
    }
}
