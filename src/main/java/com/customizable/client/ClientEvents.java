package com.customizable.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.customizable.customizable;
import com.customizable.item.CustomMusicDiscItem;

@Mod.EventBusSubscriber(modid = customizable.MODID, value = Dist.CLIENT)
public class ClientEvents {
    private static int tickCounter = 0;

    private static net.minecraft.world.level.Level lastLevel = null;

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            // Detect level change (including logout and dimension travel)
            if (mc.level != lastLevel) {
                if (lastLevel != null) {
                    // #region agent log
                    com.customizable.debug.DebugNdjsonLog.log("D8", "ClientEvents.onClientTick", "level change or world unload detected", "{}");
                    // #endregion
                    try { com.customizable.client.MP3MusicManager.stopAll(); } catch (Exception ignored) {}
                    try { com.customizable.client.ClientJukeboxCache.clearAll(); } catch (Exception ignored) {}
                    com.customizable.client.ClientPlaybackSuppress.clearAll();
                }
                lastLevel = mc.level;
            }
            if (mc.level != null && mc.player != null) {
                tickCounter++;
                if (tickCounter % 200 == 0) {

                }

                try {
                    com.customizable.client.MP3MusicManager.tick(mc.level);
                } catch (Exception e) {
                }
                
                // Scan range
                BlockPos center = mc.player.blockPosition();
                int range = 5; 
                for (int x = -range; x <= range; x++) {
                    for (int y = -range; y <= range; y++) {
                        for (int z = -range; z <= range; z++) {
                            BlockPos p = center.offset(x, y, z);
                            if (mc.level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.JUKEBOX)) {
                                BlockEntity be = mc.level.getBlockEntity(p);
                                if (be instanceof JukeboxBlockEntity jukebox) {
                                    ItemStack disc = jukebox.getItem(0); // Using getItem(0) because Forge Jukebox implements Container
                                    if (tickCounter % 40 == 0) {

                                    }
                                    net.minecraft.world.item.ItemStack effectiveDisc = disc;
                                    if (effectiveDisc.isEmpty()) {
                                        // consult client-side cache
                                        effectiveDisc = com.customizable.client.ClientJukeboxCache.get(p);
                                    }
                                    // If playback is ongoing but the client now sees no disc, stop locally and notify server
                                    try {
                                        if (com.customizable.client.MP3MusicManager.isPlaying(p) && effectiveDisc.isEmpty()) {

                                            com.customizable.client.MP3MusicManager.stop(p);
                                            com.customizable.client.ClientPlaybackSuppress.suppress(p, 2000);
                                            com.customizable.network.ModMessages.sendToServer(new com.customizable.network.EjectDiscPacket(p));
                                            continue; // skip auto-start logic
                                        }
                                    } catch (Exception ignored) {}

                                    // Do not auto-start playback if this pos is suppressed (recently ejected)
                                    if (com.customizable.client.ClientPlaybackSuppress.isSuppressed(p)) {

                                        continue;
                                    }
                                    if (!effectiveDisc.isEmpty() && (effectiveDisc.getItem() instanceof CustomMusicDiscItem)) {
                                        if (!com.customizable.client.MP3MusicManager.isPlaying(p)) {
                                            if (effectiveDisc.hasTag() && effectiveDisc.getTag().contains("SelectedFile")) {
                                                String path = effectiveDisc.getTag().getString("SelectedFile");
                                                // #region agent log
                                                com.customizable.debug.DebugNdjsonLog.logOnce(
                                                        "tick-play:" + p.asLong(),
                                                        "D6",
                                                        "ClientEvents.onClientTick",
                                                        "tick auto play",
                                                        com.customizable.debug.DebugNdjsonLog.pathFieldsJson(path));
                                                // #endregion
                                                com.customizable.client.MP3MusicManager.play(p, path);
                                            } else {
                                                // Request server to sync NBT for this jukebox position occasionally
                                                if (tickCounter % 40 == 0) {
                                                    // #region agent log
                                                    com.customizable.debug.DebugNdjsonLog.logOnce(
                                                            "tick-req:" + p.asLong(),
                                                            "D6",
                                                            "ClientEvents.onClientTick",
                                                            "tick request disc info",
                                                            "{}");
                                                    // #endregion
                                                    com.customizable.network.ModMessages.sendToServer(new com.customizable.network.RequestDiscInfoPacket(p));
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (tickCounter % 40 == 0) {

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        // #region agent log
        com.customizable.debug.DebugNdjsonLog.log("D8", "ClientEvents.onClientLogout", "logout event", "{}");
        // #endregion
        try { com.customizable.client.MP3MusicManager.stopAll(); } catch (Exception ignored) {}
    }

    private static java.util.List<BlockPos> getJukeboxesInRange(Player player, int range) {
        java.util.List<BlockPos> positions = new java.util.ArrayList<>();
        if (player == null) return positions;
        BlockPos center = player.blockPosition();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos p = center.offset(x, y, z);
                    if (player.level().getBlockState(p).is(net.minecraft.world.level.block.Blocks.JUKEBOX)) {
                        positions.add(p);
                    }
                }
            }
        }
        return positions;
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;

        if (sound.getLocation().equals(customizable.DUMMY_MUSIC.getId())) {
            // #region agent log
            com.customizable.debug.DebugNdjsonLog.log(
                    "D9",
                    "ClientEvents.onPlaySound",
                    "dummy sound intercepted",
                    "{\"x\":" + sound.getX() + ",\"y\":" + sound.getY() + ",\"z\":" + sound.getZ() + "}");
            // #endregion
            // Prevent vanilla/dummy record sound from continuing independently of custom MP3 state.
            event.setSound(null);

            BlockPos pos = new BlockPos((int) sound.getX(), (int) sound.getY(), (int) sound.getZ());
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be instanceof JukeboxBlockEntity jukebox) {
                    ItemStack disc = jukebox.getItem(0);

                    if (disc.getItem() instanceof CustomMusicDiscItem) {
                        if (disc.hasTag() && disc.getTag().contains("SelectedFile")) {
                            String path = disc.getTag().getString("SelectedFile");

                            com.customizable.client.MP3MusicManager.play(pos, path);
                        } else {

                        }
                    } else {

                    }
                } else {

                }
            }
        }
    }
}
