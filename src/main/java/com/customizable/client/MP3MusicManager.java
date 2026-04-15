package com.customizable.client;

import javazoom.jl.player.Player;
import net.minecraft.core.BlockPos;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.customizable.network.ModMessages;
import com.customizable.network.EjectDiscPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;

public class MP3MusicManager {
    private static class PlayerHolder {
        volatile Player player;
        volatile Thread thread;
        volatile java.io.InputStream stream;
        volatile boolean stopping = false;
    }

    private static final Map<BlockPos, PlayerHolder> activePlayers = new ConcurrentHashMap<>();
    private static final Map<BlockPos, String> activePaths = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> lastFailureAt = new ConcurrentHashMap<>();

    public static void play(BlockPos pos, String path) {
        // #region agent log
        com.customizable.debug.DebugNdjsonLog.log(
                "D3",
                "MP3MusicManager.play",
                "enter play",
                com.customizable.debug.DebugNdjsonLog.mergeObjects(
                        "{\"pos\":\"" + pos.toShortString() + "\"}",
                        com.customizable.debug.DebugNdjsonLog.pathFieldsJson(path)));
        // #endregion
        synchronized (activePlayers) {
            Long lastFail = lastFailureAt.get(pos);
            if (lastFail != null && (System.currentTimeMillis() - lastFail) < 5000L) {
                return;
            }
            PlayerHolder existing = activePlayers.get(pos);
            String existingPath = activePaths.get(pos);
            if (existing != null) {
                if (path.equals(existingPath) && !existing.stopping) {
                    // Already playing this path at this position and not stopping
                    return;
                }
                // Stop the existing one if it's different or stopping
                stop(pos);
            }

            // Reserve the path and holder atomically
            activePaths.put(pos, path);
            PlayerHolder holder = new PlayerHolder();
            activePlayers.put(pos, holder);

            Thread t = new Thread(() -> {
                java.io.FileInputStream fis = null;
                java.io.BufferedInputStream bis = null;
                try {
                    if (holder.stopping) return;

                    fis = new java.io.FileInputStream(path);
                    bis = new java.io.BufferedInputStream(fis);
                    bis.mark(16);
                    byte[] head = new byte[10];
                    int read = bis.read(head);
                    if (read == 10 && head[0] == 'I' && head[1] == 'D' && head[2] == '3') {
                        int size = ((head[6] & 0x7F) << 21) | ((head[7] & 0x7F) << 14) | ((head[8] & 0x7F) << 7) | (head[9] & 0x7F);
                        long skipTotal = size;
                        while (skipTotal > 0) {
                            long s = bis.skip(skipTotal);
                            if (s <= 0) break;
                            skipTotal -= s;
                        }
                    } else {
                        bis.reset();
                    }
                    
                    if (holder.stopping) return;

                    holder.stream = bis;
                    Player player = new Player(bis);
                    holder.player = player;
                    holder.thread = Thread.currentThread();

                    if (holder.stopping) {
                        player.close();
                        return;
                    }

                    player.play();

                    // finished normally
                    if (!holder.stopping) {
                        // suppress immediate restart on client for 2 seconds to allow server to clear state
                        try { com.customizable.client.ClientPlaybackSuppress.suppress(pos, 2000); } catch (Exception ignored) {}
                        ModMessages.sendToServer(new EjectDiscPacket(pos));
                    }
                } catch (Exception e) {
                    lastFailureAt.put(pos, System.currentTimeMillis());
                } finally {
                    // cleanup
                    try { if (holder.player != null) holder.player.close(); } catch (Exception ignored) {}
                    try { if (holder.stream != null) holder.stream.close(); } catch (Exception ignored) {}
                    try { if (fis != null) fis.close(); } catch (Exception ignored) {}
                    synchronized (activePlayers) {
                        // Only remove if it's still OUR holder
                        if (activePlayers.get(pos) == holder) {
                            activePlayers.remove(pos);
                            activePaths.remove(pos);
                        }
                    }
                }
            }, "MP3-Player-" + pos);
            t.setDaemon(true);
            holder.thread = t;
            t.start();
        }
    }

    public static void stop(BlockPos pos) {
        PlayerHolder holder = activePlayers.get(pos);
        if (holder != null) {
            holder.stopping = true;
            activePaths.remove(pos); // mark path as gone immediately
            try {
                if (holder.player != null) {
                    holder.player.close();
                }
            } catch (Exception e) { }
            try {
                if (holder.stream != null) {
                    holder.stream.close();
                }
            } catch (Exception e) { }
            try {
                if (holder.thread != null) {
                    holder.thread.interrupt();
                }
            } catch (Exception e) { }
            // Note: we do NOT remove from activePlayers here; the thread's finally block will do it.
            // This ensures isPlaying() stays true until the thread is actually gone.
        }
    }

    public static void stopAll() {
        activePlayers.forEach((pos, holder) -> {
            stop(pos);
        });
    }

    public static boolean isPlaying(BlockPos pos) {
        PlayerHolder holder = activePlayers.get(pos);
        return holder != null && !holder.stopping;
    }

    public static boolean isAnyActive(BlockPos pos) {
        return activePlayers.containsKey(pos);
    }

    public static void tick(net.minecraft.world.level.Level level) {
        // Iterate over a copy to avoid concurrent modification when stopping removes entries
        for (BlockPos pos : new java.util.ArrayList<>(activePlayers.keySet())) {
            try {
                var state = level.getBlockState(pos);
                boolean isJukebox = state.is(Blocks.JUKEBOX);
                boolean hasRecordFlag = isJukebox && state.getValue(JukeboxBlock.HAS_RECORD);

                // Check block entity slot if available
                boolean beHasRecord = false;
                var be = level.getBlockEntity(pos);
                if (be instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jb) {
                    var item = jb.getItem(0);
                    if (!item.isEmpty()) beHasRecord = true;
                }

                // On client, consult client cache as a fallback
                boolean clientStored = false;
                try {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc != null && mc.level == level) {
                        var cached = com.customizable.client.ClientJukeboxCache.get(pos);
                        if (!cached.isEmpty()) clientStored = true;
                    }
                } catch (Exception ignored) {}

                // If the block is a jukebox but its state indicates no record, clear the client cache for this pos
                if (isJukebox && !hasRecordFlag && !beHasRecord) {
                    try { com.customizable.client.ClientJukeboxCache.remove(pos); } catch (Exception ignored) {}
                    clientStored = false;
                }

                // If neither the block state, the block entity slot, nor the client cache indicate a record, stop playback
                if (!isJukebox || (!hasRecordFlag && !beHasRecord && !clientStored)) {
                    // #region agent log
                    com.customizable.debug.DebugNdjsonLog.log(
                            "D8",
                            "MP3MusicManager.tick",
                            "tick stop condition",
                            "{\"pos\":\"" + pos.toShortString() + "\",\"isJukebox\":" + isJukebox + ",\"hasRecordFlag\":" + hasRecordFlag + ",\"beHasRecord\":" + beHasRecord + ",\"clientStored\":" + clientStored + "}");
                    // #endregion
                    stop(pos);
                }
            } catch (Exception e) {
                // On any unexpected error, be conservative and stop playback for this pos to avoid orphaned players
                stop(pos);
            }
        }
    }
}
