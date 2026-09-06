package dev.ixpu.leaguemechanics.player;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.ItemStatsManager;
import dev.ixpu.leaguemechanics.rune.keystones.precision.LethalTempo;
import dev.ixpu.leaguemechanics.rune.keystones.domination.HailOfBlades;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.rune.DebuffType;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import dev.ixpu.leaguemechanics.item.passives.dark_seal;

import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static dev.ixpu.leaguemechanics.player.PlayerClass.*;

public class PlayerStats {
    private static final Map<UUID, PlayerStats> INSTANCE_CACHE = new ConcurrentHashMap<>();

    private double temporaryADModification = 0.0;
    private double temporaryAPModification = 0.0;

    private double temporaryARModification = 0.0;
    private double temporaryMRModification = 0.0;
    private double temporaryTDModification = 0.0;
    private double temporaryMSModification = 0.0;
    private double temporaryASModification = 0.0;
    private double temporaryCritDamageModification = 0.0;
    private double temporaryHealingMultiplier = 1.0;

    public static PlayerStats getOrCreate(Player player) {
        UUID uuid = player.getUniqueId();
        return INSTANCE_CACHE.computeIfAbsent(uuid, k -> new PlayerStats());
    }

    public static void invalidateCache(UUID uuid) {
        INSTANCE_CACHE.remove(uuid);
    }

    public double getPlayerHP(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        double itemHP = 0;
        double baseHP = getPlayerClassBaseHP(player) + player.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (itemStatsManager != null) {
            itemHP += itemStatsManager.getItemHP(player);
        }
        return baseHP + itemHP;
    }

    public double getPlayerHR(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        return itemStatsManager.getItemHR(player);
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
        return Math.max(0, baseAD + itemAD + enchantAD + temporaryADModification);
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

        return Math.max(0, baseAP + itemAP + temporaryAPModification);
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
        double asRatio = getPlayerClassAttackSpeedRatio(player);
        double bonusASMultiplier = (itemAS + runeAS) / 100.0;
        return asRatio * bonusASMultiplier + baseAS;
    }

    public double getPlayerAR(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        double itemAR = 0;
        double baseAR = getPlayerClassBaseAR(player) + player.getAttribute(Attribute.ARMOR).getValue();
        double enchantAR = getArmorEnchant(player);
        if (itemStatsManager != null) {
            itemAR += itemStatsManager.getItemAR(player);
        }
        return baseAR + itemAR + enchantAR + temporaryARModification;
    }

    public double getPlayerMR(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        double itemMR = 0;
        double baseMR = getPlayerClassBaseMR(player);
        if (itemStatsManager != null) {
            itemMR += itemStatsManager.getItemMR(player);
        }
        return baseMR + itemMR + temporaryMRModification;
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
            double totalMS = (baseMS + attributeMS) * 100;
            totalMS += (speedEffectBonus * 100);
            totalMS += itemMS;
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
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        if (itemStatsManager == null) return 0;
        return itemStatsManager.getItemCH(player);
    }

    public double getPlayerTN(Player player) {
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();
        if (itemStatsManager == null) return 0;
        return itemStatsManager.getItemTN(player);
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
        return 0.5 + (sharpnessLevel * 0.5);
    }

    public double getArmorEnchant(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        int totalBonusProtectionLevel = 0;

        if (helmet != null && helmet.getType() != Material.AIR) {
            totalBonusProtectionLevel += helmet.getEnchantmentLevel(Enchantment.PROTECTION);
        }
        if (chestplate != null && chestplate.getType() != Material.AIR) {
            totalBonusProtectionLevel += chestplate.getEnchantmentLevel(Enchantment.PROTECTION);
        }
        if (leggings != null && leggings.getType() != Material.AIR) {
            totalBonusProtectionLevel += leggings.getEnchantmentLevel(Enchantment.PROTECTION);
        }
        if (boots != null && boots.getType() != Material.AIR) {
            totalBonusProtectionLevel += boots.getEnchantmentLevel(Enchantment.PROTECTION);
        }

        return totalBonusProtectionLevel * 0.4;
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

    public void modifyTD(double amount) {
        this.temporaryTDModification += amount;
    }

    public void modifyMS(double amount) {
        this.temporaryMSModification += amount;
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

    public void modifyHealingMultiplier(double multiplier) {
        this.temporaryHealingMultiplier = Math.max(0.0, this.temporaryHealingMultiplier + multiplier);
    }

    public double getHealingMultiplier() {
        return temporaryHealingMultiplier;
    }

    public boolean hasGrievousWounds(Player player) {
        if (player == null) return false;
        return DebuffManager.getInstance().hasDebuff(player, DebuffType.GRIEVOUS_WOUNDS);
    }

    public double getEffectiveHealingMultiplier(Player player) {
        double base = getHealingMultiplier();
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

    public double getTemporaryTDModification() {
        return temporaryTDModification;
    }

    public double getCritDamageBonus(Player player) {
        return temporaryCritDamageModification;
    }

    public void resetTemporaryModifications() {
        this.temporaryADModification = 0.0;
        this.temporaryAPModification = 0.0;
        this.temporaryARModification = 0.0;
        this.temporaryMRModification = 0.0;
        this.temporaryTDModification = 0.0;
        this.temporaryMSModification = 0.0;
        this.temporaryASModification = 0.0;
        this.temporaryCritDamageModification = 0.0;
        this.temporaryHealingMultiplier = 1.0;
    }

    public void clearDebuffs(Player player) {
        if (player != null) {
            DebuffManager.getInstance().clearDebuffs(player);
        }
    }

    public String getActionBarSections(Player player) {
        if (player == null) return "";

        StringBuilder sb = new StringBuilder();

        DebuffManager debuffs = DebuffManager.getInstance();
        if (debuffs.hasDebuff(player, DebuffType.GRIEVOUS_WOUNDS)) {
            double remaining = debuffs.getRemainingSeconds(player, DebuffType.GRIEVOUS_WOUNDS);
            sb.append(" §2🍀 (").append(String.format("%.1f", remaining)).append("s)");
        }
        if (debuffs.hasDebuff(player, DebuffType.INFLAME)) {
            double remaining = debuffs.getRemainingSeconds(player, DebuffType.INFLAME);
            sb.append(" §6🔥 (").append(String.format("%.1f", remaining)).append("s)");
        }
        if (debuffs.hasDebuff(player, DebuffType.SLOW)) {
            double remaining = debuffs.getRemainingSeconds(player, DebuffType.SLOW);
            sb.append(" §3❄ (").append(String.format("%.1f", remaining)).append("s)");
        }

        dev.ixpu.leaguemechanics.manager.ItemPassivesManager passiveManager =
                dev.ixpu.leaguemechanics.manager.ItemPassivesManager.getInstance();
        if (passiveManager != null) {
            appendPassiveCooldown(sb, passiveManager, player, "hextech-alternator", "§7🕹");
            appendPassiveCooldown(sb, passiveManager, player, "hexdrinker", "§7🛡");
            appendPassiveCooldown(sb, passiveManager, player, "bamis-cinder", "§7🔥");
            appendPassiveCooldown(sb, passiveManager, player, "verdant-barrier", "§7⛨");
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
}