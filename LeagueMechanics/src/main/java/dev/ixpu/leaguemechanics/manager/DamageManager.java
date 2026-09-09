package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.player.PlayerStats;
import dev.ixpu.leaguemechanics.util.ItemModifier;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;

import java.util.Random;


public class DamageManager {
    private final ItemStatsManager itemStatsManager;

    protected boolean isAdaptiveScaling = false;
    protected boolean isAdaptiveDamage = false;
    protected boolean isTrueDamage = false;
    protected boolean isPerStack = false;
    protected boolean isOnlyAP = false;

    private static final Random RANDOM = new Random();

    private static final double CRIT_DAMAGE_MULTIPLIER = 1.75;

    private static final double DEFAULT_MAGIC_RATIO = 0.6;

    public DamageManager(ItemStatsManager itemStatsManager) {
        this.itemStatsManager = itemStatsManager;
    }

    public void enableAdaptiveScaling() {
        this.isAdaptiveScaling = true;
    }
    public void enableAdaptiveDamage() {
        this.isAdaptiveDamage = true;
    }
    public void enableTrueDamage() {
        this.isTrueDamage = true;
    }
    public void enablePerStackScaling() {
        this.isPerStack = true;
    }
    public void enableOnlyAP() {
        this.isOnlyAP = true;
    }

    public boolean isMagicDamage() {
        return isOnlyAP;
    }


    public double DamageCalculation(Player player, Entity target, int currentStacks, double runesAdaptive, double runesTrueDamage) {
        PlayerStats stats = PlayerStats.getOrCreate(player);
        ItemStatsManager statsManager = LeagueMechanics.getInstance().getStatsManager();

        double doransBonus = getDoransOnHitAD(player);

        double attackerAD = (stats.getPlayerAD(player) - statsManager.getItemAD(player)) + (statsManager.getItemAP(player) / 17) + doransBonus;
        double attackerAP = (stats.getPlayerAP(player) - statsManager.getItemAP(player)) + (statsManager.getItemAP(player) / 17);

        double targetAR = getTargetAR(target);
        double targetMR = getTargetMR(target);

        double apenFlat = statsManager.getItemAPen(player);
        double apenPercent = statsManager.getItemAPenPercent(player);
        double mpenFlat = statsManager.getItemMPen(player);
        double mpenPercent = statsManager.getItemMPenPercent(player);

        double baseDamage;

        if (isOnlyAP) {
            baseDamage = attackerAP;
        } else if (isTrueDamage) {
            baseDamage = stats.getPlayerTD(player)
                    + ((attackerAD + attackerAP) * (runesTrueDamage / 100.0));
        } else if (isAdaptiveDamage) {
            double adaptive = runesAdaptive * levelBasedBonus(player);
            if (isAdaptiveScaling) {
                adaptive += adaptive * stats.getPlayerAF(player);
            }
            boolean preferMagic = statsManager.getItemAP(player) > statsManager.getItemAD(player);
            baseDamage = applyResistance(adaptive, preferMagic, targetAR, targetMR, apenFlat, apenPercent, mpenFlat, mpenPercent);
        } else {
            double physical = applyResistance(attackerAD, false, targetAR, targetMR, apenFlat, apenPercent, mpenFlat, mpenPercent);
            double magic = applyResistance(attackerAP * DEFAULT_MAGIC_RATIO, true, targetAR, targetMR, apenFlat, apenPercent, mpenFlat, mpenPercent);
            baseDamage = physical + magic;
        }

        int stacks = isPerStack ? currentStacks : 1;
        return baseDamage * stacks;
    }

    private double applyResistance(double damage, boolean isMagic, double targetAR, double targetMR,
                                   double apenFlat, double apenPercent, double mpenFlat, double mpenPercent) {
        double resist = isMagic ? targetMR : targetAR;
        double flatPen = isMagic ? mpenFlat : apenFlat;
        double percentPen = isMagic ? mpenPercent : apenPercent;

        double effectiveResist = Math.max(0, resist - flatPen);
        effectiveResist = effectiveResist * (1.0 - percentPen / 100.0);

        return damage / (1.0 + (effectiveResist / 100.0));
    }

    public double getPlayerCritChance(Player player) {
        if (itemStatsManager != null) {
            return itemStatsManager.getItemCC(player);
        }
        return 0;
    }

    public static boolean criticalChance(Player player, double playerCritChance) {
        return CritManager.getInstance().rollCrit(player, playerCritChance);
    }

    public static double getCritDamageMultiplier(Player player) {
        double bonus = PlayerStats.getOrCreate(player).getCritDamageBonus(player);
        return CRIT_DAMAGE_MULTIPLIER + bonus;
    }

    private double levelBasedBonus(Player player) {
        return levelBasedBonusForLevel(player.getLevel());
    }


    public static double levelBasedBonusForLevel(double playerLevel) {
        if (playerLevel >= 300) {
            return 1.3;
        } else if (playerLevel >= 200) {
            return 1.2;
        } else if (playerLevel >= 100) {
            return 1.07;
        } else if (playerLevel >= 50) {
            return 1.05;
        } else {
            return 1.02;
        }
    }

    public double getTargetAR(Entity target) {
        if (!(target instanceof Player targetPlayer)) {
            return 0;
        }
        return PlayerStats.getOrCreate(targetPlayer).getPlayerAR(targetPlayer);
    }

    public double getTargetMR(Entity target) {
        if (!(target instanceof Player targetPlayer)) {
            return 0;
        }
        return PlayerStats.getOrCreate(targetPlayer).getPlayerMR(targetPlayer);
    }

    public double getDoransOnHitAD(Player player) {
        double bonus = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            String itemId = ItemModifier.getItemId(item);
            if (itemId != null && (itemId.equals("dorans-ring") || itemId.equals("dorans-shield"))) {
                bonus += 10;
            }
        }
        return bonus;
    }
}

