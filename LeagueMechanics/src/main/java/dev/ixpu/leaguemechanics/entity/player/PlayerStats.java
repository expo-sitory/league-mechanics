package dev.ixpu.leaguemechanics.entity.player;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.ItemStatsManager;
import dev.ixpu.leaguemechanics.rune.keystones.precision.LethalTempo;
import dev.ixpu.leaguemechanics.rune.keystones.domination.HailOfBlades;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.rune.DebuffType;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import dev.ixpu.leaguemechanics.item.passives.dark_seal;
import dev.ixpu.leaguemechanics.rune.shards.ShardStats;
import dev.ixpu.leaguemechanics.rune.RuneShard;

import dev.ixpu.leaguemechanics.util.Icons;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static dev.ixpu.leaguemechanics.entity.player.PlayerClass.*;

public class PlayerStats {
    private static final Map<UUID, PlayerStats> INSTANCE_CACHE = new ConcurrentHashMap<>();

    private static final double BASE_HEALING_MULTIPLIER = 1.0;

    private double temporaryADModification = 0.0;
    private double temporaryAPModification = 0.0;

    private double temporaryARModification = 0.0;
    private double temporaryMRModification = 0.0;
    private double temporaryTDModification = 0.0;
    private double temporaryMSModification = 0.0;
    private double temporaryASModification = 0.0;
    private double temporaryCritDamageModification = 0.0;

    private int graspHearts = 0;

    private ShardStats shardStats;
    private int leagueLevel = 1;

    public static PlayerStats getOrCreate(Player player) {
        UUID uuid = player.getUniqueId();
        return INSTANCE_CACHE.computeIfAbsent(uuid, k -> {
            PlayerStats stats = new PlayerStats();
            int leagueLevel = LeagueMechanics.getInstance().getRunePersistence().loadLeagueLevel(uuid);
            stats.setLeagueLevel(leagueLevel);
            return stats;
        });
    }

    public static void invalidateCache(UUID uuid) {
        PlayerStats current = INSTANCE_CACHE.remove(uuid);
        int graspHearts = (current != null) ? current.getGraspHearts() : 0;
        if (graspHearts > 0) {
            PlayerStats restored = new PlayerStats();
            int leagueLevel = LeagueMechanics.getInstance().getRunePersistence().loadLeagueLevel(uuid);
            restored.setLeagueLevel(leagueLevel);
            restored.setGraspHearts(graspHearts);
            INSTANCE_CACHE.put(uuid, restored);
        }
    }

    public int getLeagueLevel() {
        return leagueLevel;
    }

    public void setLeagueLevel(int level) {
        this.leagueLevel = level;
    }

    public void saveLeagueLevel(Player player) {
        UUID uuid = player.getUniqueId();
        LeagueMechanics.getInstance().getRunePersistence().saveLeagueLevel(uuid, this.leagueLevel);
    }

    public double getPlayerHP(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        double itemHP = 0;
        double baseHP = getPlayerClassBaseHP(player) + player.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (itemStatsManager != null) {
            itemHP += itemStatsManager.getItemHP(player);
        }
        double shardsHP = getRuneShards(player).getHealth();
        double leagueLevelHP = getLeagueLevel() * 8.0;
        return baseHP + itemHP + shardsHP + leagueLevelHP;
    }

    public double getPlayerHR(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        return itemStatsManager.getItemHR(player) + getRuneShards(player).getHealthRegen();
    }


    public double getPlayerAD(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        ItemStack weapon = player.getInventory().getItemInMainHand();
        double itemAD = 0;
        double baseAD = getPlayerClassBaseAD(player) + player.getAttribute(Attribute.ATTACK_DAMAGE).getValue();
        double enchantAD = getWeaponEnchant(player);
        if (isWeapon(weapon)) {
            baseAD++;
        }
        if (itemStatsManager != null) {
            itemAD += itemStatsManager.getItemAD(player);
        }
        double totalAD = Math.max(0, (baseAD + itemAD + temporaryADModification) * (1.0 + enchantAD));

        if (player.hasPotionEffect(PotionEffectType.WEAKNESS)) {
            int amplifier = player.getPotionEffect(PotionEffectType.WEAKNESS).getAmplifier();
            double multiplier = 1 - (0.2 * (amplifier + 1));
            totalAD = Math.max(0, totalAD * multiplier);
        }

        return totalAD;
    }

    public double getPlayerAP(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        double itemAP = 0;
        double baseAP = getPlayerClassBaseAP(player);
        if (itemStatsManager != null) {
            itemAP += itemStatsManager.getItemAP(player);
        }

        dark_seal darkSeal = (dark_seal) ItemPassivesRegistry.getInstance().getPassive("dark-seal");
        if (darkSeal != null) {
            itemAP += darkSeal.getAbilityPower(player);
        }

        double totalAP = Math.max(0, baseAP + itemAP + temporaryAPModification);

        if (player.hasPotionEffect(PotionEffectType.WEAKNESS)) {
            int amplifier = player.getPotionEffect(PotionEffectType.WEAKNESS).getAmplifier();
            double multiplier = 1 - (0.2 * (amplifier + 1));
            totalAP = Math.max(0, totalAP * multiplier);
        }

        return totalAP;
    }

    public double getPlayerAF(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        double bonusAD = itemStatsManager.getItemAD(player);
        double bonusAP = itemStatsManager.getItemAP(player);
        if (bonusAD > bonusAP) {
            return 0.6;
        } else if (bonusAP > bonusAD) {
            return 0.8;
        }
        return 0;
    }

    public double getPlayerTD(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        double itemTD = 0;
        double baseTD = 0;
        double bonusTD = levelBasedTD(player);
        if (itemStatsManager != null) {
            itemTD += itemStatsManager.getItemTD(player);
        }
        return baseTD + itemTD + bonusTD + temporaryTDModification;
    }

    public double getPlayerAS(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();

        double itemAS = 0;
        double runeAS = 0;
        double baseAS = getPlayerClassBaseAS(player);

        if (itemStatsManager != null) {
            itemAS += itemStatsManager.getItemAS(player);
        }

        PlayerRuneData runeData = LeagueMechanics.getInstance().getRuneManager().getPlayerRuneData(player);
        if (runeData != null) {
            for (CooldownHandler rune : runeData.getAllRunes()) {
                if (rune == null) {
                    continue;
                }
                if (rune instanceof LethalTempo lethalTempo) {
                    runeAS += lethalTempo.getActiveASBonus(player);
                }
                if (rune instanceof HailOfBlades hailOfBlades) {
                    runeAS += hailOfBlades.getActiveASBonus(player);
                }
            }
        }
        double weaponAS = getWeaponAS(player);
        double shardsAS = getRuneShards(player).getAttackSpeed();
        double asRatio = getPlayerClassAttackSpeedRatio(player);
        double bonusASMultiplier = (itemAS + weaponAS + asRatio + shardsAS) / 100.0;
        return asRatio * bonusASMultiplier + baseAS;
    }

    public double getPlayerAR(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        double itemAR = 0;
        double baseAR = getPlayerClassBaseAR(player) + player.getAttribute(Attribute.ARMOR).getValue();
        if (itemStatsManager != null) {
            itemAR += itemStatsManager.getItemAR(player);
        }
        double totalAR = baseAR + itemAR + temporaryARModification;
        totalAR = totalAR * getProtectionARBonus(player);

        if (player.hasPotionEffect(PotionEffectType.RESISTANCE)) {
            int amplifier = player.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier();
            double multiplier = 1 + (0.2 * (amplifier + 1));
            totalAR *= multiplier;
        }

        return Math.max(0, totalAR);
    }

    public double getPlayerMR(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        double itemMR = 0;
        double baseMR = getPlayerClassBaseMR(player);
        if (itemStatsManager != null) {
            itemMR += itemStatsManager.getItemMR(player);
        }
        double totalMR = baseMR + itemMR + temporaryMRModification;
        totalMR = totalMR * getProtectionMRBonus(player);

        if (player.hasPotionEffect(PotionEffectType.RESISTANCE)) {
            int amplifier = player.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier();
            double multiplier = 1 + (0.2 * (amplifier + 1));
            totalMR *= multiplier;
        }

        return Math.max(0, totalMR);
    }

    public double getPlayerMS(Player player) {
        try {
            double baseMS = 0.1;
            double attributeMS = player.getAttribute(Attribute.MOVEMENT_SPEED).getValue();
            double speedEffectBonus = 0;
            if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED)) {
                org.bukkit.potion.PotionEffect speedEffect = player.getPotionEffect(org.bukkit.potion.PotionEffectType.SPEED);
                if (speedEffect != null) {
                    speedEffectBonus = 0.2 * (speedEffect.getAmplifier() + 1);
                }
            }
            ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
            double itemMS = itemStatsManager != null ? itemStatsManager.getItemMS(player) : 0;
            double shardsMS = getRuneShards(player).getMovementSpeed();
            double totalMS = (baseMS + attributeMS) * 100;
            totalMS += (speedEffectBonus * 100);
            totalMS += itemMS + shardsMS;
            totalMS += temporaryMSModification;

            return totalMS;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public double getPlayerCC(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        return itemStatsManager.getItemCC(player);
    }

    public double getPlayerCH(Player player) {
        double shardsCH = getRuneShards(player).getCooldownHaste();
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        if (itemStatsManager == null) {
            return shardsCH;
        }
        return itemStatsManager.getItemCH(player) + shardsCH;
    }

    public double getPlayerTN(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        if (itemStatsManager == null) return 0;
        double shardsTN = getRuneShards(player).getTenacity();
        return itemStatsManager.getItemTN(player) + shardsTN;
    }

    private double levelBasedTD(Player player) {
        double playerLevel = player.getLevel();
        if (playerLevel >= 300) {
            return 15;
        } else if (playerLevel >= 200) {
            return 12;
        } else if (playerLevel >= 100) {
            return 8;
        } else if (playerLevel >= 50) {
            return 4;
        } else {
            return 2;
        }
    }

    private static final Set<Material> WEAPON_MATERIALS = EnumSet.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.GOLDEN_SWORD,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.GOLDEN_AXE,
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE, Material.GOLDEN_PICKAXE,
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
            Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL, Material.GOLDEN_SHOVEL,
            Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
            Material.DIAMOND_HOE, Material.NETHERITE_HOE, Material.GOLDEN_HOE,
            Material.TRIDENT, Material.MACE
    );

    private boolean isWeapon(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return WEAPON_MATERIALS.contains(item.getType());
    }

    public double getWeaponEnchant(Player player) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon.getType() == Material.AIR) {
            return 0;
        }

        int sharpnessLevel = weapon.getEnchantmentLevel(Enchantment.SHARPNESS);
        if (sharpnessLevel == 0) {
            return 0;
        }
        return sharpnessLevel * 0.10;
    }

    public double getWeaponAS(Player player) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType().isAir()) {
            return 0;
        }
        return player.getAttribute(Attribute.ATTACK_SPEED).getValue();
    }

    public double getProtectionMRBonus(Player player) {
        int totalProtectionLevel = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) {
                totalProtectionLevel += armor.getEnchantmentLevel(Enchantment.PROTECTION);
            }
        }
        return 1.0 + (totalProtectionLevel * 0.15);
    }

    public double getProtectionARBonus(Player player) {
        int totalProtectionLevel = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) {
                totalProtectionLevel += armor.getEnchantmentLevel(Enchantment.PROTECTION);
            }
        }
        return 1.0 + (totalProtectionLevel * 0.10);
    }

    public void modifyAD(double amount) {
        this.temporaryADModification += amount;
    }

    public void modifyAP(double amount) {
        this.temporaryAPModification += amount;
    }

    public void modifyAR(double amount) {
        this.temporaryARModification += amount;
    }

    public void modifyMR(double amount) {
        this.temporaryMRModification += amount;
    }

    public void setTemporaryMSModification(double value) {
        this.temporaryMSModification = value;
    }

    public double getTemporaryMSModification() {
        return temporaryMSModification;
    }

    public void setTemporaryASModification(double value) {
        this.temporaryASModification = value;
    }

    public double getTemporaryASModification() {
        return temporaryASModification;
    }

    public void modifyCritDamage(double amount) {
        this.temporaryCritDamageModification += amount;
    }

    public double getHealingMultiplier(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        double itemHM = itemStatsManager != null ? itemStatsManager.getItemHM(player) : 0;
        return BASE_HEALING_MULTIPLIER * (1.0 + itemHM / 100.0);
    }

    public boolean hasGrievousWounds(Player player) {
        if (player == null) return false;
        return DebuffManager.getInstance().hasDebuff(player, DebuffType.GRIEVOUS_WOUNDS);
    }

    public double getEffectiveHealingMultiplier(Player player) {
        double base = getHealingMultiplier(player);
        if (hasGrievousWounds(player)) {
            base *= 0.6;
        }
        return base;
    }

    public double getEffectiveLifeSteal(Player player, double baseLifeStealPercent) {
        if (hasGrievousWounds(player)) {
            return baseLifeStealPercent * 0.6;
        }
        return baseLifeStealPercent;
    }

    public double getEffectiveHealthRegen(Player player, double baseRegen) {
        if (hasGrievousWounds(player)) {
            return baseRegen * 0.6;
        }
        return baseRegen;
    }

    public double getPlayerCSD(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        if (itemStatsManager == null) return 0;
        return itemStatsManager.getItemCSD(player);
    }

    public double getCritDamageBonus(Player player) {
        return temporaryCritDamageModification;
    }

    public String getActionBarSections(Player player) {
        if (player == null) return "";

        StringBuilder sb = new StringBuilder();

        DebuffManager debuffs = DebuffManager.getInstance();
        if (debuffs.hasDebuff(player, DebuffType.GRIEVOUS_WOUNDS)) {
            double remaining = debuffs.getRemainingSeconds(player, DebuffType.GRIEVOUS_WOUNDS);
            sb.append("§f" + Icons.get("status.grievous-wounds") + " §4(").append(String.format("%.1f", remaining)).append("s)§f");
        }
        if (debuffs.hasDebuff(player, DebuffType.INFLAME)) {
            double remaining = debuffs.getRemainingSeconds(player, DebuffType.INFLAME);
            sb.append("§f" + Icons.get("status.burned") + " §6(").append(String.format("%.1f", remaining)).append("s)§f");
        }
        org.bukkit.potion.PotionEffect slow = player.getPotionEffect(PotionEffectType.SLOWNESS);
        if (slow != null && slow.getDuration() > 0) {
            sb.append("§f" + Icons.get("status.slowed") + " §b(").append(String.format("%.1f", slow.getDuration() / 20.0)).append("s)§f");
        }

        dev.ixpu.leaguemechanics.manager.ItemPassivesManager passiveManager =
                dev.ixpu.leaguemechanics.manager.ItemPassivesManager.getInstance();
        if (passiveManager != null) {
            appendPassiveCooldown(sb, passiveManager, player, "hextech-alternator", "§7" + Icons.get("items.hextech-alternator"));
            appendPassiveCooldown(sb, passiveManager, player, "scouts-slingshot", "§7" + Icons.get("items.scouts-slingshot"));
            appendPassiveCooldown(sb, passiveManager, player, "hexdrinker", "§7" + Icons.get("items.hexdrinker"));
            appendPassiveCooldown(sb, passiveManager, player, "bamis-cinder", "§7" + Icons.get("items.bamis-cinder"));
            appendPassiveCooldown(sb, passiveManager, player, "verdant-barrier", "§7" + Icons.get("items.verdant-barrier"));
        }

        return sb.toString();
    }

    private void appendPassiveCooldown(StringBuilder sb,
                                       dev.ixpu.leaguemechanics.manager.ItemPassivesManager manager,
                                       Player player, String passiveId, String icon) {
        double remaining = manager.getRemainingCooldownSeconds(player, passiveId);
        if (remaining <= 0) return;
        sb.append(" ").append(icon).append(" §7").append(String.format("%.1fs", remaining));
    }

    public ShardStats getRuneShards(Player player) {
        if (shardStats == null) {
            String[] shardSelections = LeagueMechanics.getInstance().getRunePersistence().loadRuneShards(player.getUniqueId());
            if (shardSelections != null) {
                shardStats = new ShardStats();
                try {
                    RuneShard shard1 = (shardSelections.length > 0 && shardSelections[0] != null) ?
                            RuneShard.valueOf(shardSelections[0]) : null;
                    RuneShard shard2 = (shardSelections.length > 1 && shardSelections[1] != null) ?
                            RuneShard.valueOf(shardSelections[1]) : null;
                    RuneShard shard3 = (shardSelections.length > 2 && shardSelections[2] != null) ?
                            RuneShard.valueOf(shardSelections[2]) : null;
                    shardStats.selectShards(shard1, shard2, shard3);
                } catch (IllegalArgumentException e) {
                    shardStats = new ShardStats();
                }
            } else {
                shardStats = new ShardStats();
            }
        }
        return shardStats;
    }

    public int getGraspHearts() {
        return graspHearts;
    }

    public void addGraspHearts(int amount) {
        this.graspHearts += amount;
    }

    public void setGraspHearts(int amount) {
        this.graspHearts = amount;
    }
}