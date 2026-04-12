package com.customizable.client;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import net.minecraft.core.BlockPos;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MP3MusicManager {
    private static final Map<BlockPos, Player> activePlayers = new ConcurrentHashMap<>();

    public static void play(BlockPos pos, String path) {
        stop(pos);
        new Thread(() -> {
            try {
                Player player = new Player(new BufferedInputStream(new FileInputStream(path)));
                activePlayers.put(pos, player);
                player.play();
                activePlayers.remove(pos);
            } catch (FileNotFoundException | JavaLayerException e) {
                System.err.println("Failed to play MP3: " + e.getMessage());
                activePlayers.remove(pos);
            }
        }, "MP3-Player-" + pos).start();
    }

    public static void stop(BlockPos pos) {
        Player player = activePlayers.get(pos);
        if (player != null) {
            player.close();
            activePlayers.remove(pos);
        }
    }

    public static void stopAll() {
        activePlayers.forEach((pos, player) -> player.close());
        activePlayers.clear();
    }

    public static void tick(net.minecraft.world.level.Level level) {
        if (activePlayers.isEmpty()) return;

        for (BlockPos pos : activePlayers.keySet()) {
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jukebox) || jukebox.getItem(0).isEmpty()) {
                stop(pos);
            }
        }
    }
}
