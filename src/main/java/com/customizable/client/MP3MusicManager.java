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

    public static void play(BlockPos pos, String path) {
        synchronized (activePlayers) {
            PlayerHolder existing = activePlayers.get(pos);
            String existingPath = activePaths.get(pos);
            if (existing != null && path.equals(existingPath)) {
                // Already playing this path at this position
                return;
            }

            // Stop any existing playback first
            stop(pos);

            // Reserve the path and holder atomically
            activePaths.put(pos, path);
            PlayerHolder holder = new PlayerHolder();
            activePlayers.put(pos, holder);

            Thread t = new Thread(() -> {
                java.io.FileInputStream fis = null;
                java.io.BufferedInputStream bis = null;
                try {
                    fis = new java.io.FileInputStream(path);
                    bis = new java.io.BufferedInputStream(fis);
                    holder.stream = bis;
                    Player player = new Player(bis);
                    holder.player = player;
                    holder.thread = Thread.currentThread();

                    player.play();

                    // finished normally
                    synchronized (activePlayers) {
                        if (activePlayers.containsKey(pos) && !holder.stopping) {

                            // suppress immediate restart on client for 2 seconds to allow server to clear state
                            try { com.customizable.client.ClientPlaybackSuppress.suppress(pos, 2000); } catch (Exception ignored) {}
                            ModMessages.sendToServer(new EjectDiscPacket(pos));
                        }
                    }
                } catch (Exception e) {
                } finally {
                    // cleanup
                    try { if (holder.stream != null) holder.stream.close(); } catch (Exception ignored) {}
                    try { if (fis != null) fis.close(); } catch (Exception ignored) {}
                    synchronized (activePlayers) {
                        activePlayers.remove(pos);
                        activePaths.remove(pos);
                    }
                }
            }, "MP3-Player-" + pos);
            t.setDaemon(true);
            holder.thread = t;
            t.start();
        }
    }

    public static void stop(BlockPos pos) {

        activePaths.remove(pos);
        PlayerHolder holder = activePlayers.get(pos);
        if (holder != null) {
            holder.stopping = true;
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
            activePlayers.remove(pos);
        }
    }

    public static void stopAll() {
        activePlayers.forEach((pos, holder) -> {
            holder.stopping = true;
            try { if (holder.player != null) { holder.player.close(); } } catch (Exception e) { }
            try { if (holder.stream != null) { holder.stream.close(); } } catch (Exception e) { }
            try { if (holder.thread != null) { holder.thread.interrupt(); } } catch (Exception e) { }
        });
        activePlayers.clear();
        activePaths.clear();
    }

    public static boolean isPlaying(BlockPos pos) {
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

                // If neither the block state, the block entity slot, nor the client cache indicate a record, stop playback
                if (!isJukebox || (!hasRecordFlag && !beHasRecord && !clientStored)) {
                    stop(pos);
                }
            } catch (Exception e) {
                // On any unexpected error, be conservative and stop playback for this pos to avoid orphaned players
                stop(pos);
            }
        }
    }
}
