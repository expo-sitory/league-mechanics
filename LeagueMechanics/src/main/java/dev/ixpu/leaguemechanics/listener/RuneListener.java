package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.player.PlayerRuneData;

import dev.ixpu.leaguemechanics.manager.RuneManager;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import org.bukkit.entity.Player;

import java.util.*;

public class RuneListener implements Listener {
    private final RuneManager runeManager;

    public RuneListener(LeagueMechanics plugin) {
        this.runeManager = plugin.getRuneManager();
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerRuneData runeData = runeManager.getPlayerRuneData(player);
        if (runeData == null) {
            return;
        }
        for (CooldownHandler rune : runeData.getAllRunes()) {
            if (rune == null) {
                continue;
            }
            rune.onBlockBreak(player, 1);
        }
    }
}