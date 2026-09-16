package dev.ixpu.leaguemechanics.rune.keystones.resolve;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.Component;

public class Guardian extends CooldownHandler {

    private int MAX_PLAYERS;
    private double ABSORPTION_PERCENTAGE;

    private int COOLDOWN_SECONDS;

    private static final double DETECTION_RANGE = 10.0;
    private final static int PEACE_DURATION_TICKS = 200;
    private final static int GUARD_RAISE_DURATION_TICKS = 200;
    private final static int ABSORPTION_DURATION_TICKS = 1000;

    private final Map<UUID, List<UUID>> trackedPlayers = new HashMap<>();
    private final Map<UUID, Integer> windupCounter = new HashMap<>();
    private final Map<UUID, Long> lastShieldTime = new HashMap<>();
    private final Map<UUID, Long> lastCombatTime = new HashMap<>();

    public Guardian(ConfigurationSection config) {
        super("guardian", RunePath.RESOLVE, RuneSlot.KEYSTONE);
        ConfigurationSection section = config.getConfigurationSection("runes.keystones.resolve.guardian");
        if (section != null) {
            this.MAX_PLAYERS = section.getInt("max-players", this.MAX_PLAYERS);
            this.ABSORPTION_PERCENTAGE = section.getDouble("absorption-percentage", this.ABSORPTION_PERCENTAGE);
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
    }

    @Override
    public void onDisable(Player player) {
        UUID uuid = player.getUniqueId();
        clearPlayerCooldown(player);
        trackedPlayers.remove(uuid);
        windupCounter.remove(uuid);
        lastShieldTime.remove(uuid);
        lastCombatTime.remove(uuid);
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

        applyShield(player);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.5f);

        int count = 0;
        for (UUID trackedUUID : shields) {
            if (count >= MAX_PLAYERS) break;

            Player trackedPlayer = player.getServer().getPlayer(trackedUUID);
            if (trackedPlayer != null && trackedPlayer.isOnline()) {
                applyShield(trackedPlayer);
                trackedPlayer.playSound(trackedPlayer.getLocation(), org.bukkit.Sound.ITEM_TRIDENT_RETURN, 1.0f, 0.5f);
                count++;
            }
        }
        resetCooldown(player);
    }

    private void applyShield(Player player) {

        int maxHealth = (int) Math.ceil(player.getMaxHealth());
        int absorbAmount = (int) Math.ceil(maxHealth * (ABSORPTION_PERCENTAGE / 100) / 4.0);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,
                ABSORPTION_DURATION_TICKS,
                Math.min(absorbAmount, 255),
                false,
                false
        ));
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