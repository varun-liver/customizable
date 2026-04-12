package com.customizable.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import com.customizable.customizable;
import com.customizable.network.ModMessages;
import com.customizable.network.SelectedFilePacket;
import javax.swing.*;
import java.io.File;

public class CustomMusicDiscItem extends RecordItem {
    public CustomMusicDiscItem(Properties properties) {
        super(15, customizable.DUMMY_MUSIC, properties, 1);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                openFilePicker();
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    private void openFilePicker() {
        // Run on AWT thread to avoid blocking MC main thread
        new Thread(() -> {
            try {
                // Ensure the look and feel is set or it might crash/look weird
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select a .mp3 file");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3 Files", "mp3"));
            
            int returnVal = chooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (file.exists()) {
                    ModMessages.sendToServer(new SelectedFilePacket(file.getAbsolutePath()));
                }
            }
        }).start();
    }
}
