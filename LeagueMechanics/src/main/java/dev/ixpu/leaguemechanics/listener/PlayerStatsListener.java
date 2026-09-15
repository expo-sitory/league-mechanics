package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.util.ItemModifier;

import dev.ixpu.leaguemechanics.item.ItemStatsData;
import dev.ixpu.leaguemechanics.item.ItemStatsRegistry;
import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;

import dev.ixpu.leaguemechanics.manager.ItemStatsManager;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatsListener {
    private final LeagueMechanics plugin;

    private static final String LEAGUE_HP_MODIFIER = "league_hp";
    private static final String LEAGUE_MS_MODIFIER = "league_ms";

    private final Map<UUID, UUID> hpModifierIds = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> msModifierIds = new ConcurrentHashMap<>();

    public PlayerStatsListener(LeagueMechanics plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("removal")
    public void removeAllAttributeModifiers(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();

        UUID hpId = hpModifierIds.remove(playerId);
        if (hpId != null) {
            var hpAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (hpAttr != null) {
                hpAttr.removeModifier(hpId);
            }
        }
        UUID msId = msModifierIds.remove(playerId);
        if (msId != null) {
            var msAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (msAttr != null) {
                msAttr.removeModifier(msId);
            }
        }
    }

    public void applyPlayerStats(Player player) {
        syncItemStats(player);
        applyHealthModifier(player);
        applyMovementSpeedModifier(player);
    }

    public void syncItemStats(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        ItemModifier.syncItemStats(mainHand);

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (!offHand.getType().isAir()) {
            ItemModifier.syncItemStats(offHand);
        }

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && !armor.getType().isAir()) {
                ItemModifier.syncItemStats(armor);
            }
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                ItemModifier.syncItemStats(item);
            }
        }
    }

    @SuppressWarnings("removal")
    public void applyHealthModifier(Player player) {
        ItemStatsManager statsManager = plugin.getStatsManager();
        if (statsManager == null) return;

        double classBaseHP = dev.ixpu.leaguemechanics.entity.player.PlayerClass.getPlayerClassBaseHP(player);
        double itemBonusHP = statsManager.getItemHP(player);
        double runeBonusHP = PlayerStats.getOrCreate(player).getRuneShards(player).getHealth();
        double leagueLevelHP = PlayerStats.getOrCreate(player).getLeagueLevel() * 4.0;
        double bonusHP = classBaseHP + itemBonusHP + runeBonusHP + leagueLevelHP;
        UUID playerId = player.getUniqueId();
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            UUID existingId = hpModifierIds.remove(playerId);
            if (existingId != null) {
                attr.removeModifier(existingId);
                hpModifierIds.remove(playerId);
            }
        }

        if (bonusHP > 0) {
            UUID newId = UUID.randomUUID();
            hpModifierIds.put(playerId, newId);
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).addModifier(
                    new AttributeModifier(newId, LEAGUE_HP_MODIFIER, bonusHP, AttributeModifier.Operation.ADD_NUMBER)
            );
        }

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    @SuppressWarnings("removal")
    public void applyMovementSpeedModifier(Player player) {
        ItemStatsManager statsManager = plugin.getStatsManager();
        if (statsManager == null) return;

        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        double itemMS = statsManager.getItemMS(player);
        double runeMS = playerStats.getTemporaryMSModification();
        double bonusMS = itemMS + runeMS;
        UUID playerId = player.getUniqueId();

        var attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null) {
            UUID existingId = msModifierIds.remove(playerId);
            if (existingId != null) {
                attr.removeModifier(existingId);
                msModifierIds.remove(playerId);
            }
        }

        if (bonusMS != 0.0) {
            double speedBonus = 0.1 * (bonusMS / 100.0);
            UUID newId = UUID.randomUUID();
            msModifierIds.put(playerId, newId);
            Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).addModifier(
                    new AttributeModifier(newId, LEAGUE_MS_MODIFIER, speedBonus, AttributeModifier.Operation.ADD_NUMBER)
            );
        }
    }

    public void syncItemStatsOnMove(ItemStack cursor, ItemStack currentItem) {
        if (!cursor.getType().isAir()) {
            ItemModifier.syncItemStats(cursor);
        }
        if (currentItem != null && !currentItem.getType().isAir()) {
            ItemModifier.syncItemStats(currentItem);
        }
    }

    public ItemPassive getEquippedPassive(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        String itemId = ItemModifier.getItemId(item);
        if (itemId == null) return null;
        ItemStatsRegistry data = ItemStatsData.getInstance().getItem(itemId);
        if (data == null || !data.hasPassive()) return null;
        return ItemPassivesRegistry.getInstance().getPassive(data.getPassiveId());
    }
}