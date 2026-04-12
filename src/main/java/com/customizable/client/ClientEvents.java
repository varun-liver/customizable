package com.customizable.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
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

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                MP3MusicManager.tick(mc.level);
            }
        }
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;

        if (sound.getLocation().equals(customizable.DUMMY_MUSIC.getId())) {
            BlockPos pos = new BlockPos((int) sound.getX(), (int) sound.getY(), (int) sound.getZ());
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be instanceof JukeboxBlockEntity jukebox) {
                    ItemStack disc = jukebox.getItem(0);
                    if (disc.getItem() instanceof CustomMusicDiscItem) {
                        if (disc.hasTag() && disc.getTag().contains("SelectedFile")) {
                            String path = disc.getTag().getString("SelectedFile");
                            MP3MusicManager.play(pos, path);
                        }
                    }
                }
            }
        }
    }
}
