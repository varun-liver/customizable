package com.customizable.client;

import net.minecraft.core.BlockPos;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientPlaybackSuppress {
    private static final Map<BlockPos, Long> SUPPRESSED = new ConcurrentHashMap<>();

    public static void suppress(BlockPos pos, long durationMs) {
        if (pos == null) return;
        SUPPRESSED.put(pos.immutable(), System.currentTimeMillis() + durationMs);
    }

    public static boolean isSuppressed(BlockPos pos) {
        Long until = SUPPRESSED.get(pos);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            SUPPRESSED.remove(pos);
            return false;
        }
        return true;
    }

    public static void clear(BlockPos pos) {
        SUPPRESSED.remove(pos);
    }

    public static void clearAll() {
        SUPPRESSED.clear();
    }
}
