package dev.ixpu.leaguemechanics.rune.keystones.inspiration;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.DebuffType;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.manager.StatScalingManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;

public class GlacialAugment extends CooldownHandler {
    private int BASE_FREEZE_STRENGTH;

    private double AD_PERCENTAGE_MULTIPLIER;
    private double AP_PERCENTAGE_MULTIPLIER;

    private int COOLDOWN_DURATION_SECONDS;

    private static final double DEBUFF_REDUCTION_PERCENT = 15.0;

    private final Map<UUID, Map<UUID, Integer>> frozenTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> debuffedTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Double>> debuffAmounts = new ConcurrentHashMap<>();

    private final Map<UUID, List<org.bukkit.Location>> placedSnowByAttacker = new ConcurrentHashMap<>();

    private LeagueMechanics plugin;

    public GlacialAugment(ConfigurationSection config, LeagueMechanics plugin) {
        super("glacial-augment", RunePath.INSPIRATION, RuneSlot.KEYSTONE);
        this.plugin = plugin;
        ConfigurationSection section = config.getConfigurationSection("runes.keystones.inspiration.glacial-augment");

        if (section != null) {
            this.COOLDOWN_DURATION_SECONDS = section.getInt("cooldown", 45);
            this.BASE_FREEZE_STRENGTH = section.getInt("base-freeze-strength", this.BASE_FREEZE_STRENGTH);
            this.AD_PERCENTAGE_MULTIPLIER = section.getDouble("ad-percentage-multiplier", this.AD_PERCENTAGE_MULTIPLIER);
            this.AP_PERCENTAGE_MULTIPLIER = section.getDouble("ap-percentage-multiplier", this.AP_PERCENTAGE_MULTIPLIER);
        }
        this.setCooldownSeconds(COOLDOWN_DURATION_SECONDS);
    }

    @Override
    public void onEnable(Player player) {
        UUID uuid = player.getUniqueId();
        frozenTargets.put(uuid, new ConcurrentHashMap<>());
    }

    @Override
    public void onDisable(Player player) {
        UUID uuid = player.getUniqueId();
        clearPlayerCooldown(player);
        frozenTargets.remove(uuid);
        clearSnowByAttacker(uuid);
    }

    public void clearAllSnowBlocks() {
        for (List<org.bukkit.Location> locations : placedSnowByAttacker.values()) {
            for (org.bukkit.Location loc : locations) {
                if (loc.getBlock().getType() == org.bukkit.Material.POWDER_SNOW) {
                    loc.getBlock().setType(org.bukkit.Material.AIR);
                }
            }
        }
        placedSnowByAttacker.clear();
    }

    private void clearSnowByAttacker(UUID attackerUUID) {
        List<org.bukkit.Location> locations = placedSnowByAttacker.remove(attackerUUID);
        if (locations != null) {
            for (org.bukkit.Location loc : locations) {
                if (loc.getBlock().getType() == org.bukkit.Material.POWDER_SNOW) {
                    loc.getBlock().setType(org.bukkit.Material.AIR);
                }
            }
        }
    }

    public void onProjectileHit(Player shooter, Entity target) {
        activateGlacialAugment(shooter, target);
    }

    private void activateGlacialAugment(Player player, Entity target) {
        UUID attackerUUID = player.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget.getMaxHealth() < 20) {
            return;
        }
        if (CheckEnchant(weapon)) {
            return;
        }
        if (isOnCooldown(player)) {
            return;
        }

        Map<UUID, Integer> frozen = frozenTargets.get(attackerUUID);
        if (frozen == null) {
            frozen = new ConcurrentHashMap<>();
            frozenTargets.put(attackerUUID, frozen);
        }

        int scaledFreezeDuration = getScaledFreezeDuration(player);
        frozen.put(targetUUID, scaledFreezeDuration);
        debuffedTargets.put(targetUUID, scaledFreezeDuration);

        org.bukkit.Location loc = target.getLocation();

        List<org.bukkit.Location> snowLocations = new ArrayList<>();
        snowLocations.add(loc.clone());
        snowLocations.add(loc.clone().add(1, 0, 0));
        snowLocations.add(loc.clone().add(2, 0, 0));
        snowLocations.add(loc.clone().add(-1, 0, 0));
        snowLocations.add(loc.clone().add(-2, 0, 0));
        snowLocations.add(loc.clone().add(0, 0, 1));
        snowLocations.add(loc.clone().add(0, 0, 2));
        snowLocations.add(loc.clone().add(0, 0, -1));
        snowLocations.add(loc.clone().add(0, 0, -2));

        List<org.bukkit.Location> placedThisActivation = new ArrayList<>();
        for (org.bukkit.Location snowLoc : snowLocations) {
            snowLoc.getChunk().load();
            if (snowLoc.getBlock().getType() == org.bukkit.Material.AIR) {
                snowLoc.getBlock().setType(org.bukkit.Material.POWDER_SNOW);
                snowLoc.getBlock().getState().update();
                placedThisActivation.add(snowLoc.clone());
            }
        }
        placedSnowByAttacker.computeIfAbsent(attackerUUID, k -> new ArrayList<>())
                .addAll(placedThisActivation);

        if (livingTarget instanceof Player slowPlayer && player != null) {
            StatScalingManager ssm = new StatScalingManager();
            double slowStrength = ssm.calculateScaledValue(player, BASE_FREEZE_STRENGTH, AD_PERCENTAGE_MULTIPLIER, AP_PERCENTAGE_MULTIPLIER);
            int slowDurationTicks = 60;
            DebuffManager.getInstance().applyDebuff(slowPlayer, DebuffType.SLOW, slowDurationTicks, slowStrength);
        }

        applyDebuffToTarget(livingTarget, targetUUID, scaledFreezeDuration);

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f);

        if (plugin != null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                clearPlacedSnow(snowLocations, placedThisActivation);
            }, scaledFreezeDuration);
        }
        resetCooldown(player);
    }

    private void clearPlacedSnow(List<org.bukkit.Location> snowLocations, List<org.bukkit.Location> placedThisActivation) {
        for (org.bukkit.Location placed : placedThisActivation) {
            if (placed.getBlock().getType() == org.bukkit.Material.POWDER_SNOW) {
                placed.getBlock().setType(org.bukkit.Material.AIR);
            }
        }
        for (List<org.bukkit.Location> tracked : placedSnowByAttacker.values()) {
            tracked.removeAll(placedThisActivation);
        }
    }

    private boolean CheckEnchant(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (item.getType().isAir()) {
            return false;
        }
        if (meta == null) {
            return false;
        }

        return meta.getEnchants().keySet().stream()
                .anyMatch(e -> {
                    String enchantName = e.toString().toLowerCase();
                    return enchantName.contains("flame");
                });
    }

    private void trackFreeze(Player player) {
        UUID playerUUID = player.getUniqueId();

        Map<UUID, Integer> frozen = frozenTargets.get(playerUUID);
        if (frozen == null) {
            String runeDisplay = getRuneDisplay(player, RuneState.IDLE);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        java.util.Iterator<java.util.Map.Entry<UUID, Integer>> frozenIt = frozen.entrySet().iterator();
        while (frozenIt.hasNext()) {
            java.util.Map.Entry<UUID, Integer> entry = frozenIt.next();
            int duration = entry.getValue();
            if (duration > 0) {
                duration--;
                if (duration == 0) {
                    frozenIt.remove();
                } else {
                    entry.setValue(duration);
                }
            } else {
                frozenIt.remove();
            }
        }

        trackDebuffTimers();
    }

    private void trackDebuffTimers() {
        java.util.Iterator<java.util.Map.Entry<UUID, Integer>> it = debuffedTargets.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<UUID, Integer> entry = it.next();
            UUID targetUUID = entry.getKey();
            int duration = entry.getValue();
            if (duration > 0) {
                duration--;
                if (duration == 0) {
                    restoreDebuff(targetUUID);
                    it.remove();
                } else {
                    entry.setValue(duration);
                }
            } else {
                restoreDebuff(targetUUID);
                it.remove();
            }
        }
    }

    private int getScaledFreezeDuration(Player player) {
        StatScalingManager statScalingManager = new StatScalingManager();
        double scaledDuration = statScalingManager.calculateScaledValue(
                player,
                BASE_FREEZE_STRENGTH,
                AD_PERCENTAGE_MULTIPLIER,
                AP_PERCENTAGE_MULTIPLIER
        );
        return (int) scaledDuration;
    }

    private void applyDebuffToTarget(LivingEntity target, UUID targetUUID, int duration) {
        if (target instanceof Player player) {
            PlayerStats stats = PlayerStats.getOrCreate(player);
            StatScalingManager statScalingManager = new StatScalingManager();

            double currentAD = stats.getPlayerAD(player);
            double currentAP = stats.getPlayerAP(player);

            double reducedAD = statScalingManager.calculateReverseScaledValue(currentAD, DEBUFF_REDUCTION_PERCENT);
            double reducedAP = statScalingManager.calculateReverseScaledValue(currentAP, DEBUFF_REDUCTION_PERCENT);

            double adReduction = currentAD - reducedAD;
            double apReduction = currentAP - reducedAP;

            stats.modifyAD(-adReduction);
            stats.modifyAP(-apReduction);

            Map<String, Double> amounts = new HashMap<>();
            amounts.put("AD", adReduction);
            amounts.put("AP", apReduction);
            debuffAmounts.put(targetUUID, amounts);
        }
    }

    public void reapplyDebuffsForRejoin(Player rejoiningPlayer) {
        UUID targetUUID = rejoiningPlayer.getUniqueId();
        Integer remainingTicks = debuffedTargets.get(targetUUID);
        if (remainingTicks == null) {
            return;
        }

        Map<String, Double> amounts = debuffAmounts.get(targetUUID);
        if (amounts == null) {
            debuffedTargets.remove(targetUUID);
            return;
        }

        if (remainingTicks <= 0) {
            PlayerStats stats = PlayerStats.getOrCreate(rejoiningPlayer);
            stats.modifyAD(amounts.getOrDefault("AD", 0.0));
            stats.modifyAP(amounts.getOrDefault("AP", 0.0));
            debuffAmounts.remove(targetUUID);
            debuffedTargets.remove(targetUUID);
        } else {
            PlayerStats stats = PlayerStats.getOrCreate(rejoiningPlayer);
            stats.modifyAD(-amounts.getOrDefault("AD", 0.0));
            stats.modifyAP(-amounts.getOrDefault("AP", 0.0));
        }
    }

    private void restoreDebuff(UUID targetUUID) {
        Map<String, Double> amounts = debuffAmounts.get(targetUUID);
        if (amounts == null) {
            return;
        }
        org.bukkit.entity.Player targetPlayer = Bukkit.getPlayer(targetUUID);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            PlayerStats stats = PlayerStats.getOrCreate(targetPlayer);
            stats.modifyAD(amounts.getOrDefault("AD", 0.0));
            stats.modifyAP(amounts.getOrDefault("AP", 0.0));
            debuffAmounts.remove(targetUUID);
        }
    }

    public boolean isTargetDebuffed(UUID targetUUID) {
        return debuffedTargets.containsKey(targetUUID) && debuffedTargets.get(targetUUID) > 0;
    }

    public double getDebuffReduction(UUID targetUUID) {
        if (isTargetDebuffed(targetUUID)) {
            return DEBUFF_REDUCTION_PERCENT / 100.0;
        }
        return 0.0;
    }

    @Override
    public void tick(Player player) {
        RuneState state = isOnCooldown(player) ? RuneState.COOLDOWN : RuneState.IDLE;
        String runeDisplay = getRuneDisplay(player, state);
        setPlayerDisplay(player, runeDisplay);
        trackFreeze(player);
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);

        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        COOLDOWN, IDLE
    }

    private String getRuneDisplay(Player player, RuneState state) {
        return switch (state) {
            case COOLDOWN -> "§7❄ " + getCooldownDisplay(player);
            case IDLE -> "§3❄";
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        RuneState state = isOnCooldown(player) ? RuneState.COOLDOWN : RuneState.IDLE;
        return getRuneDisplay(player, state);
    }
}