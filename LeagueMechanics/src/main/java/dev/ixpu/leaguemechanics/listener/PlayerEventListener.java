package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.item.*;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import dev.ixpu.leaguemechanics.manager.CombatStateManager;
import dev.ixpu.leaguemechanics.manager.ItemStatsManager;
import dev.ixpu.leaguemechanics.manager.RuneManager;
import dev.ixpu.leaguemechanics.manager.CritManager;

import dev.ixpu.leaguemechanics.player.PlayerRuneData;
import dev.ixpu.leaguemechanics.player.PlayerStats;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RuneCooldownGate;
import dev.ixpu.leaguemechanics.rune.RuneRegistry;
import dev.ixpu.leaguemechanics.rune.RuneShard;
import dev.ixpu.leaguemechanics.rune.keystones.resolve.GraspOfTheUndying;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.*;



import dev.ixpu.leaguemechanics.util.RunePersistence;
import java.util.UUID;



public class PlayerEventListener implements Listener, RuneCooldownGate {

    private final RuneManager runeManager;
    private final RuneRegistry runeRegistry;
    private final ItemStatsManager itemStatsManager;
    private final PlayerStatsListener playerStatsListener;
    private final PlayerInventoryListener playerInventoryListener;
    private final DamageListener damageListener;
    private final CombatStateManager combatState = CombatStateManager.getInstance();
    private final RunePersistence runePersistence;

    public PlayerEventListener(LeagueMechanics plugin, PlayerStatsListener playerStatsListener, DamageListener damageListener) {
        this.runeManager = plugin.getRuneManager();
        this.runeRegistry = plugin.getRuneRegistry();
        this.itemStatsManager = plugin.getStatsManager();
        this.runePersistence = new RunePersistence(plugin);
        this.damageListener = damageListener;
        this.playerStatsListener = playerStatsListener;
        this.playerInventoryListener = new PlayerInventoryListener(plugin, playerStatsListener);
    }

    public void applyPlayerStats(Player player) {
        playerStatsListener.applyPlayerStats(player);
    }

    public void removeAllAttributeModifiers(Player player) {
        playerStatsListener.removeAllAttributeModifiers(player);
    }

    public void cancelPendingTasks(UUID playerId) {
        playerInventoryListener.cancelPendingTasks(playerId);
    }

    @Override
    public boolean letRunesThrough(Player player) {
        return damageListener.letRunesThrough(player);
    }

    @Override
    public boolean isAnyHotbarOnCooldown(Player player) {
        return damageListener.isAnyHotbarOnCooldown(player);
    }

    public void removeFromHotbar(Player player, ItemStack item) {
        playerInventoryListener.removeFromHotbar(player, item);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        runeManager.loadPlayerRunes(player);
        dev.ixpu.leaguemechanics.player.PlayerClass.loadPlayerClass(player);
        UUID uuid = player.getUniqueId();
        String[] shardNames = runePersistence.loadRuneShards(uuid);
        if (shardNames != null && shardNames.length >= 3) {
            try {
                if (shardNames[0] != null && shardNames[1] != null && shardNames[2] != null) {
                    RuneShard row1 = RuneShard.valueOf(shardNames[0]);
                    RuneShard row2 = RuneShard.valueOf(shardNames[1]);
                    RuneShard row3 = RuneShard.valueOf(shardNames[2]);
                    PlayerStats.getOrCreate(player).getRuneShards(player).selectShards(row1, row2, row3);
                }
            } catch (IllegalArgumentException e) {
                //
            }
        }
        applyPlayerStats(player);
        CooldownHandler glacial = runeRegistry.getRune("glacial-augment");
        if (glacial instanceof dev.ixpu.leaguemechanics.rune.keystones.inspiration.GlacialAugment glacialAugment) {
            glacialAugment.reapplyDebuffsForRejoin(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (runeRegistry.getRune("grasp-of-the-undying") instanceof GraspOfTheUndying grasp) {
            grasp.resetAbsorption(player);
        }
        runeManager.unloadPlayerRunes(player);
        dev.ixpu.leaguemechanics.player.PlayerClass.unloadPlayer(uuid);
        combatState.clearPlayer(uuid);
        dev.ixpu.leaguemechanics.player.PlayerKDA.getInstance().saveForPlayer(uuid);

        if (ItemPassivesRegistry.getInstance().getPassive("dark-seal") instanceof dev.ixpu.leaguemechanics.item.passives.dark_seal darkSeal) {
            darkSeal.clearStacks(player);
        }

        PlayerStats.invalidateCache(uuid);
        itemStatsManager.invalidateCache(uuid);
        CritManager.getInstance().removePlayer(player);
        removeAllAttributeModifiers(player);
        cancelPendingTasks(uuid);
    }



    @EventHandler
    public void onHealthRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.EATING ||
                event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PlayerRuneData runeData = runeManager.getPlayerRuneData(player);
        if (runeData == null) {
            return;
        }

        for (CooldownHandler rune : runeData.getAllRunes()) {
            if (rune == null) {
                continue;
            }
            rune.onPotionEffectGain(player, event.getNewEffect());
        }
    }
}
