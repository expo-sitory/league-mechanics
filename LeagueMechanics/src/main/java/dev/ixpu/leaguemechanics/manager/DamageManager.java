package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.mob.MobStats;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import dev.ixpu.leaguemechanics.rune.shards.ShardStats;
import dev.ixpu.leaguemechanics.util.ItemModifier;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;


public class DamageManager {
    private final ItemStatsManager itemStatsManager;
    private ItemPassivesRegistry passiveRegistry;

    private double RESISTANCE_BALANCER;
    private double DAMAGE_BALANCER;
    private double lastBonusMagicDamage = 0;

    protected boolean isAdaptiveScaling = false;
    protected boolean isAdaptiveDamage = false;
    protected boolean isTrueDamage = false;
    protected boolean isPerStack = false;
    protected boolean isOnlyAP = false;
    private boolean prefersMagic = false;
    private boolean isProjectileDamage = false;

    private static final double CRIT_DAMAGE_MULTIPLIER = 1.75;

    private static final double BASE_BONUS = 1.07;
    private static final double LEVEL_3_BONUS = 1.15;
    private static final double LEVEL_8_BONUS = 1.3;
    private static final double LEVEL_13_BONUS = 1.5;
    private static final double LEVEL_18_BONUS = 1.7;

    private static final double ADAPTIVE_DAMAGE_THRESHOLD = 0.7;

    public DamageManager(ItemStatsManager itemStatsManager) {
        this.itemStatsManager = itemStatsManager;
        this.passiveRegistry = ItemPassivesRegistry.getInstance();

        FileConfiguration config = LeagueMechanics.getInstance().getConfig();
        ConfigurationSection section = config.getConfigurationSection("general-settings");
        if (section != null) {
            this.DAMAGE_BALANCER = section.getDouble("damage-divisor-balancer", 0.0);
            this.RESISTANCE_BALANCER = section.getDouble("resistance-multiplier-balancer", 0.0);
        }
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
    public void enableProjectileDamage() {
        this.isProjectileDamage = true;
    }
    public double getLastBonusMagicDamage() {
        return lastBonusMagicDamage;
    }

    public boolean isMagicDamage() {
        return isOnlyAP || (isAdaptiveDamage && prefersMagic);
    }

    public double DamageCalculation(Entity source, Entity target, int currentStacks, double runesAdaptive, double runesTrueDamage, double procDamage) {
        ItemStatsManager statsManager = LeagueMechanics.getInstance().getStatsManager();
        boolean isPlayerSource = source instanceof Player;

        double itemAP = 0, itemAD = 0, doransBonus = 0, shardsAdOrAp = 0;
        double af, rawAD, rawAP, sourceAD, sourceAP, playerTD, itemAPen, itemAPenPercent, itemMPen, itemMPenPercent, leagueLevel;
        PlayerStats stats;

        if (isPlayerSource) {
            Player player = (Player) source;
            stats = PlayerStats.getOrCreate(player);

            doransBonus = getDoransOnHitAD(player);
            ShardStats shards = stats.getRuneShards(player);
            shardsAdOrAp = shards.getAdOrAp(player);

            af = stats.getPlayerAF(player);

            playerTD = stats.getPlayerTD(player);

            itemAP = statsManager.getItemAP(player);
            itemAD = statsManager.getItemAD(player);

            rawAD = stats.getPlayerAD(player);
            rawAP = stats.getPlayerAP(player);

            itemAPen = statsManager.getItemAPen(player);
            itemAPenPercent = statsManager.getItemAPenPercent(player);
            itemMPen = statsManager.getItemMPen(player);
            itemMPenPercent = statsManager.getItemMPenPercent(player);
            leagueLevel = stats.getLeagueLevel();
        } else {

            rawAD = MobStats.getMobAD(source);
            rawAP = MobStats.getMobAP(source);

            af = 0.0;
            playerTD = 0.0;
            itemAPen = 0.0;
            itemAPenPercent = 0.0;
            itemMPen = 0.0;
            itemMPenPercent = 0.0;

            leagueLevel = 1.0;
        }

        sourceAD = rawAD + doransBonus;
        sourceAP = rawAP;

        if (af <= ADAPTIVE_DAMAGE_THRESHOLD) {
            sourceAD += shardsAdOrAp;
        } else {
            sourceAP += shardsAdOrAp;
        }

        double targetAR = getTargetAR(target);
        double targetMR = getTargetMR(target);

        double apenFlat = itemAPen;
        double apenPercent = itemAPenPercent;
        double mpenFlat = itemMPen;
        double mpenPercent = itemMPenPercent;

        double baseDamage;

        if (isOnlyAP) {
            baseDamage = applyResistance(procDamage * levelBasedBonusForLevel(leagueLevel), true, targetAR, targetMR, apenFlat, apenPercent, mpenFlat, mpenPercent)/ DAMAGE_BALANCER;
        } else if (isTrueDamage) {
            baseDamage = (playerTD
                    + ((sourceAD + sourceAP) * (runesTrueDamage / 100.0))) / DAMAGE_BALANCER;
        } else if (isAdaptiveDamage) {
            double adaptive = runesAdaptive * levelBasedBonusForLevel(leagueLevel);
            if (isAdaptiveScaling) {
                adaptive += adaptive + af;
            }
            boolean preferMagic = itemAP > itemAD;
            this.prefersMagic = preferMagic;
            baseDamage = applyResistance(adaptive, preferMagic, targetAR, targetMR, apenFlat, apenPercent, mpenFlat, mpenPercent) / DAMAGE_BALANCER;
        } else {
            double physical = applyResistance(sourceAD, false, targetAR, targetMR, apenFlat, apenPercent, mpenFlat, mpenPercent);
            baseDamage = physical / DAMAGE_BALANCER;
        }

        int stacks = isPerStack ? currentStacks : 1;

        if (isProjectileDamage && isPlayerSource) {
            double bonusMagic = applyResistance(sourceAP * 0.6, true, targetAR, targetMR, itemAPen, itemAPenPercent, itemMPen, itemMPenPercent);
            lastBonusMagicDamage = bonusMagic / DAMAGE_BALANCER;
            baseDamage += lastBonusMagicDamage;
        }
        
        double finalDamage = (baseDamage + procDamage) * stacks;

        return finalDamage;
    }

    private double applyResistance(double damage, boolean isMagic, double targetAR, double targetMR,
                                   double apenFlat, double apenPercent, double mpenFlat, double mpenPercent) {

        double resist = isMagic ? targetMR : targetAR;
        double flatPen = isMagic ? mpenFlat : apenFlat;
        double percentPen = isMagic ? mpenPercent : apenPercent;

        double effectiveResist = Math.max(0, resist - flatPen);
        effectiveResist = (effectiveResist * (1.0 - percentPen / 100.0)) * RESISTANCE_BALANCER;

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

    public static double levelBasedBonusForLevel(double leagueLevel) {
        if (leagueLevel >= 18) {
            return LEVEL_18_BONUS;
        } else if (leagueLevel >= 13) {
            return LEVEL_13_BONUS;
        } else if (leagueLevel >= 8) {
            return LEVEL_8_BONUS;
        } else if (leagueLevel >= 3) {
            return LEVEL_3_BONUS;
        } else {
            return BASE_BONUS;
        }
    }

    public double getTargetAR(Entity target) {
        if (target instanceof Player targetPlayer) {
            return PlayerStats.getOrCreate(targetPlayer).getPlayerAR(targetPlayer);
        } else if (MobStats.isSupportedMob(target)) {
            return MobStats.getMobAR(target);
        }
        return 0;
    }

    public double getTargetMR(Entity target) {
        if (target instanceof Player targetPlayer) {
            return PlayerStats.getOrCreate(targetPlayer).getPlayerMR(targetPlayer);
        } else if (MobStats.isSupportedMob(target)) {
            return MobStats.getMobMR(target);
        }
        return 0;
    }

    public double getDoransOnHitAD(Entity entity) {
        if (!(entity instanceof Player player)) {
            return 0;
        }

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