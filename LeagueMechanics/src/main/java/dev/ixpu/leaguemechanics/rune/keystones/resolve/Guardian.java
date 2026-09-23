package dev.ixpu.leaguemechanics.rune.keystones.resolve;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.ItemStatsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;

public class Guardian extends CooldownHandler {

    private int MAX_PLAYERS;
    private double BASE_ARMOR;
    private double BASE_MAGIC_RESIST;
    private double ARMOR_PERCENTAGE;
    private double MAGIC_RESIST_PERCENTAGE;

    private int COOLDOWN_SECONDS;

    private static final double DETECTION_RANGE = 3.0;
    private final static int PEACE_DURATION_TICKS = 200;
    private final static int GUARD_RAISE_DURATION_TICKS = 200;
    private final static int BUFF_DURATION_TICKS = 400;

    private final Map<UUID, List<UUID>> trackedPlayers = new HashMap<>();
    private final Map<UUID, Integer> windupCounter = new HashMap<>();
    private final Map<UUID, Long> lastShieldTime = new HashMap<>();
    private final Map<UUID, Long> lastCombatTime = new HashMap<>();
    private final Map<UUID, Integer> buffTaskIds = new HashMap<>();
    private final Map<UUID, Map<UUID, Double>> appliedArmorBonuses = new HashMap<>();
    private final Map<UUID, Map<UUID, Double>> appliedMRBonuses = new HashMap<>();

    public Guardian(ConfigurationSection config) {
        super("guardian", RunePath.RESOLVE, RuneSlot.KEYSTONE);
        ConfigurationSection section = config.getConfigurationSection("runes.keystones.resolve.guardian");
        if (section != null) {
            this.MAX_PLAYERS = section.getInt("max-players", this.MAX_PLAYERS);
            this.BASE_ARMOR = section.getDouble("base-armor", this.BASE_ARMOR);
            this.BASE_MAGIC_RESIST = section.getDouble("base-magic-resist", this.BASE_MAGIC_RESIST);
            this.ARMOR_PERCENTAGE = section.getDouble("armor-percentage", this.ARMOR_PERCENTAGE);
            this.MAGIC_RESIST_PERCENTAGE = section.getDouble("magic-resist-percentage", this.MAGIC_RESIST_PERCENTAGE);
            this.COOLDOWN_SECONDS = section.getInt("cooldown", this.COOLDOWN_SECONDS);
        }
        this.setCooldownSeconds(COOLDOWN_SECONDS);
    }

    @Override
    public void onEnable(Player player) {
        UUID uuid = player.getUniqueId();
        trackedPlayers.put(uuid, new ArrayList<>());
        windupCounter.put(uuid, 0);
        lastShieldTime.put(uuid, 0L);
        lastCombatTime.put(uuid, 0L);
        buffTaskIds.put(uuid, -1);
        appliedArmorBonuses.put(uuid, new HashMap<>());
        appliedMRBonuses.put(uuid, new HashMap<>());
    }

    @Override
    public void onDisable(Player player) {
        UUID uuid = player.getUniqueId();
        clearPlayerCooldown(player);

        Integer taskId = buffTaskIds.remove(uuid);
        if (taskId != null && taskId != -1) {
            LeagueMechanics.getInstance().getServer().getScheduler().cancelTask(taskId);
        }

        clearAllResistances(player);

        trackedPlayers.remove(uuid);
        windupCounter.remove(uuid);
        lastShieldTime.remove(uuid);
        lastCombatTime.remove(uuid);
        appliedArmorBonuses.remove(uuid);
        appliedMRBonuses.remove(uuid);
    }

    public void onPlayerDamage(Player victim, double damage) {
        activateGuardian(victim);
    }

    private void activateGuardian(Player player) {
        UUID uuid = player.getUniqueId();
        if (windupCounter.getOrDefault(uuid, 0) > 0) {
            windupCounter.put(uuid, 0);
            trackedPlayers.put(uuid, new ArrayList<>());
        }
    }

    @Override
    public void tick(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<UUID> nearbyPlayers = getNearbyPeacefulPlayers(player);

        int windupCount = windupCounter.getOrDefault(playerUUID, 0);

        if (windupCount > 0) {
            List<UUID> trackedPlayers = this.trackedPlayers.getOrDefault(playerUUID, new ArrayList<>());
            long currentTime = System.currentTimeMillis();
            long peaceDurationMs = PEACE_DURATION_TICKS * 50L;

            boolean anyTrackedPlayerInCombat = false;
            for (UUID trackedUUID : trackedPlayers) {
                long lastCombat = lastCombatTime.getOrDefault(trackedUUID, 0L);
                if ((currentTime - lastCombat) < peaceDurationMs) {
                    anyTrackedPlayerInCombat = true;
                    break;
                }
            }

            if (anyTrackedPlayerInCombat) {
                windupCounter.put(playerUUID, 0);
                this.trackedPlayers.put(playerUUID, new ArrayList<>());
                String runeDisplay = getRuneDisplay(RuneState.IDLE, player, 0, 0);
                setPlayerDisplay(player, runeDisplay);
                return;
            }

            String runeDisplay = getRuneDisplay(RuneState.WINDUP, player, windupCount, trackedPlayers.size());
            setPlayerDisplay(player, runeDisplay);

            windupCounter.put(playerUUID, windupCount - 1);
            if (windupCount == 1) {
                activateEffects(player);
            }
            return;
        }

        trackedPlayers.put(playerUUID, nearbyPlayers);

        long lastCombat = lastCombatTime.getOrDefault(playerUUID, 0L);
        long currentTime = System.currentTimeMillis();
        long peaceDurationMs = PEACE_DURATION_TICKS * 50L;
        boolean playerIsPeaceful = (currentTime - lastCombat) >= peaceDurationMs;

        if (!nearbyPlayers.isEmpty() && playerIsPeaceful && !isOnCooldown(player)) {
            startGuardRaise(player);
            return;
        }

        if (isOnCooldown(player)) {
            String runeDisplay = getRuneDisplay(RuneState.COOLDOWN, player, 0, 0);
            setPlayerDisplay(player, runeDisplay);
            return;
        }

        String runeDisplay = getRuneDisplay(RuneState.IDLE, player, 0, 0);
        setPlayerDisplay(player, runeDisplay);
    }

    private List<UUID> getNearbyPeacefulPlayers(Player player) {
        List<UUID> peaceful = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            if (nearby.getLocation().distance(player.getLocation()) > DETECTION_RANGE) {
                continue;
            }

            long lastCombat = lastCombatTime.getOrDefault(nearby.getUniqueId(), 0L);
            long peaceDurationMs = PEACE_DURATION_TICKS * 50L;

            if ((currentTime - lastCombat) >= peaceDurationMs) {
                peaceful.add(nearby.getUniqueId());
                if (peaceful.size() >= MAX_PLAYERS) {
                    break;
                }
            }
        }

        return peaceful;
    }

    private void startGuardRaise(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<UUID> nearbyPlayers = getNearbyPeacefulPlayers(player);
        trackedPlayers.put(playerUUID, nearbyPlayers);
        windupCounter.put(playerUUID, GUARD_RAISE_DURATION_TICKS);
        lastShieldTime.put(playerUUID, System.currentTimeMillis());
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
    }

    public void onTakeDamage(Player player) {
        UUID uuid = player.getUniqueId();
        if (windupCounter.getOrDefault(uuid, 0) > 0) {
            windupCounter.put(uuid, 0);
            trackedPlayers.put(uuid, new ArrayList<>());
            resetCooldown(player);
        }
    }

    private void activateEffects(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<UUID> shields = trackedPlayers.getOrDefault(playerUUID, new ArrayList<>());

        applyResistances(player);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.5f);

        int count = 0;
        for (UUID trackedUUID : shields) {
            if (count >= MAX_PLAYERS) break;

            Player trackedPlayer = player.getServer().getPlayer(trackedUUID);
            if (trackedPlayer != null && trackedPlayer.isOnline()) {
                applyResistances(trackedPlayer);
                trackedPlayer.playSound(trackedPlayer.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.0f, 0.5f);
                count++;
            }
        }
        resetCooldown(player);
    }

    private void applyResistances(Player player) {
        UUID playerUUID = player.getUniqueId();
        ItemStatsManager itemStatsManager = LeagueMechanics.getInstance().getStatsManager();

        double itemAR = (itemStatsManager != null) ? itemStatsManager.getItemAR(player) : 0.0;
        double bonusArmor = BASE_ARMOR + (ARMOR_PERCENTAGE / 100.0 * Math.max(0, itemAR));

        double itemMR = (itemStatsManager != null) ? itemStatsManager.getItemMR(player) : 0.0;
        double bonusMR = BASE_MAGIC_RESIST + (MAGIC_RESIST_PERCENTAGE / 100.0 * itemMR);

        PlayerStats stats = PlayerStats.getOrCreate(player);
        stats.modifyAR(bonusArmor);
        stats.modifyMR(bonusMR);

        Map<UUID, Double> armorMap = appliedArmorBonuses.getOrDefault(playerUUID, new HashMap<>());
        Map<UUID, Double> mrMap = appliedMRBonuses.getOrDefault(playerUUID, new HashMap<>());
        armorMap.put(playerUUID, bonusArmor);
        mrMap.put(playerUUID, bonusMR);
        appliedArmorBonuses.put(playerUUID, armorMap);
        appliedMRBonuses.put(playerUUID, mrMap);

        Integer existingTaskId = buffTaskIds.getOrDefault(playerUUID, -1);
        if (existingTaskId != -1) {
            LeagueMechanics.getInstance().getServer().getScheduler().cancelTask(existingTaskId);
        }

        int[] taskId = { -1 };
        taskId[0] = LeagueMechanics.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(
                LeagueMechanics.getInstance(),
                () -> clearResistance(player, playerUUID),
                BUFF_DURATION_TICKS
        );
        buffTaskIds.put(playerUUID, taskId[0]);
    }

    private void clearResistance(Player player, UUID playerUUID) {
        if (!player.isOnline()) {
            appliedArmorBonuses.remove(playerUUID);
            appliedMRBonuses.remove(playerUUID);
            buffTaskIds.remove(playerUUID);
            return;
        }

        PlayerStats stats = PlayerStats.getOrCreate(player);
        Double armorBonus = appliedArmorBonuses.getOrDefault(playerUUID, new HashMap<>()).remove(playerUUID);
        Double mrBonus = appliedMRBonuses.getOrDefault(playerUUID, new HashMap<>()).remove(playerUUID);

        if (armorBonus != null) {
            stats.modifyAR(-armorBonus);
        }
        if (mrBonus != null) {
            stats.modifyMR(-mrBonus);
        }

        buffTaskIds.put(playerUUID, -1);
    }

    private void clearAllResistances(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!player.isOnline()) {
            appliedArmorBonuses.remove(playerUUID);
            appliedMRBonuses.remove(playerUUID);
            return;
        }

        PlayerStats stats = PlayerStats.getOrCreate(player);
        Map<UUID, Double> armorMap = appliedArmorBonuses.getOrDefault(playerUUID, new HashMap<>());
        Map<UUID, Double> mrMap = appliedMRBonuses.getOrDefault(playerUUID, new HashMap<>());

        for (Double bonus : armorMap.values()) {
            if (bonus != null) {
                stats.modifyAR(-bonus);
            }
        }
        for (Double bonus : mrMap.values()) {
            if (bonus != null) {
                stats.modifyMR(-bonus);
            }
        }

        armorMap.clear();
        mrMap.clear();
    }

    private void setPlayerDisplay(Player player, String runeDisplay) {
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);

        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    enum RuneState {
        WINDUP, COOLDOWN, IDLE
    }

    private String getRuneDisplay(RuneState state, Player player, int remaining, int nearbyCount) {
        return switch (state) {
            case COOLDOWN -> "§7《❖》 " + getCooldownDisplay(player);
            case WINDUP -> {
                String message;
                if (remaining > GUARD_RAISE_DURATION_TICKS * 2 / 3) {
                    message = "§a《§2❖》 §a" + nearbyCount + "/" + MAX_PLAYERS;
                } else if (remaining > GUARD_RAISE_DURATION_TICKS / 3) {
                    message = "§a《❖§2》 §a" + nearbyCount + "/" + MAX_PLAYERS;
                } else {
                    message = "§a《❖》 §a" + nearbyCount + "/" + MAX_PLAYERS;
                }
                yield message;
            }
            case IDLE -> "§2《❖》";
        };
    }

    @Override
    public String getDisplaySection(Player player) {
        UUID playerUUID = player.getUniqueId();
        int remaining = windupCounter.getOrDefault(playerUUID, 0);
        int nearbyCount = trackedPlayers.getOrDefault(playerUUID, new ArrayList<>()).size();
        if (remaining > 0) {
            return getRuneDisplay(RuneState.WINDUP, player, remaining, nearbyCount);
        }
        if (isOnCooldown(player)) {
            return getRuneDisplay(RuneState.COOLDOWN, player, 0, 0);
        }
        return getRuneDisplay(RuneState.IDLE, player, 0, 0);
    }
}