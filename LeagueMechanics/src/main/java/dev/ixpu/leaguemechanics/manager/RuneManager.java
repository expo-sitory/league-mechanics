package dev.ixpu.leaguemechanics.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.player.PlayerRuneData;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneRegistry;
import dev.ixpu.leaguemechanics.util.RunePersistence;

public class RuneManager {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerRuneData> playerRuneData = new HashMap<>();
    private final Map<UUID, Boolean> playerRunesActive = new HashMap<>();

    public RuneManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadPlayerRunes(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = loadRunesFromPersistence(player);
        playerRuneData.put(uuid, runeData);
        playerRunesActive.put(uuid, true);

        for (CooldownHandler rune : runeData.getAllRunes()) {
            if (rune != null) {
                rune.onEnable(player);
            }
        }
    }

    private PlayerRuneData loadRunesFromPersistence(Player player) {
        RunePersistence persistence = LeagueMechanics.getInstance().getRunePersistence();
        RuneRegistry registry = LeagueMechanics.getInstance().getRuneRegistry();

        PlayerRuneData data = new PlayerRuneData(player);

        RunePath primaryPath = persistence.loadPrimaryPath(player.getUniqueId());
        data.setPrimaryPath(primaryPath);

        RunePath secondaryPath = persistence.loadSecondaryPath(player.getUniqueId());
        data.setSecondaryPath(secondaryPath);

        String keystoneId = persistence.loadKeystoneRune(player.getUniqueId());
        if (keystoneId != null) {
            data.setKeystoneRune(registry.getRune(keystoneId));
        }

        String primarySlot1Id = persistence.loadPrimarySlot1Rune(player.getUniqueId());
        if (primarySlot1Id != null) {
            data.setPrimarySlot1Rune(registry.getRune(primarySlot1Id));
        }

        String primarySlot2Id = persistence.loadPrimarySlot2Rune(player.getUniqueId());
        if (primarySlot2Id != null) {
            data.setPrimarySlot2Rune(registry.getRune(primarySlot2Id));
        }

        String primarySlot3Id = persistence.loadPrimarySlot3Rune(player.getUniqueId());
        if (primarySlot3Id != null) {
            data.setPrimarySlot3Rune(registry.getRune(primarySlot3Id));
        }

        String secondarySlot1Id = persistence.loadSecondarySlot1Rune(player.getUniqueId());
        if (secondarySlot1Id != null) {
            data.setSecondarySlot1Rune(registry.getRune(secondarySlot1Id));
        }

        String secondarySlot2Id = persistence.loadSecondarySlot2Rune(player.getUniqueId());
        if (secondarySlot2Id != null) {
            data.setSecondarySlot2Rune(registry.getRune(secondarySlot2Id));
        }

        return data;
    }

    public void reloadPlayerRunes(Player player) {
        unloadPlayerRunes(player);
        loadPlayerRunes(player);
    }

    public void unloadPlayerRunes(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.get(uuid);

        if (runeData != null) {
            for (CooldownHandler rune : runeData.getAllRunes()) {
                if (rune != null) {
                    rune.onDisable(player);
                }
            }
        }

        playerRuneData.remove(uuid);
        playerRunesActive.remove(uuid);
    }

    public void tickAllPlayerRunes() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            tickPlayerRunes(player);
        }
    }

    public void tickPlayerRunes(Player player) {
        UUID uuid = player.getUniqueId();

        if (!playerRunesActive.getOrDefault(uuid, false)) {
            return;
        }

        PlayerRuneData runeData = playerRuneData.get(uuid);
        if (runeData == null) {
            return;
        }

        for (CooldownHandler rune : runeData.getAllRunes()) {
            if (rune != null) {
                rune.tick(player);
            }
        }
    }

    public PlayerRuneData getPlayerRuneData(Player player) {
        return playerRuneData.get(player.getUniqueId());
    }

    public boolean hasActiveRunes(Player player) {
        return playerRunesActive.getOrDefault(player.getUniqueId(), false);
    }

    public void setRunesActive(Player player, boolean active) {
        playerRunesActive.put(player.getUniqueId(), active);
    }

    public void clearAll() {
        playerRuneData.clear();
        playerRunesActive.clear();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void setPlayerKeystoneRune(Player player, CooldownHandler rune) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.getOrDefault(uuid, new PlayerRuneData(player));

        CooldownHandler oldRune = runeData.getKeystoneRune();
        if (oldRune != null) {
            oldRune.onDisable(player);
        }

        runeData.setKeystoneRune(rune);
        rune.onEnable(player);
        playerRuneData.put(uuid, runeData);
    }

    public void setPlayerPrimarySlot1Rune(Player player, CooldownHandler rune) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.getOrDefault(uuid, new PlayerRuneData(player));

        CooldownHandler oldRune = runeData.getPrimarySlot1Rune();
        if (oldRune != null) {
            oldRune.onDisable(player);
        }

        runeData.setPrimarySlot1Rune(rune);
        rune.onEnable(player);
        playerRuneData.put(uuid, runeData);
    }

    public void setPlayerPrimarySlot2Rune(Player player, CooldownHandler rune) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.getOrDefault(uuid, new PlayerRuneData(player));

        CooldownHandler oldRune = runeData.getPrimarySlot2Rune();
        if (oldRune != null) {
            oldRune.onDisable(player);
        }

        runeData.setPrimarySlot2Rune(rune);
        rune.onEnable(player);
        playerRuneData.put(uuid, runeData);
    }

    public void setPlayerPrimarySlot3Rune(Player player, CooldownHandler rune) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.getOrDefault(uuid, new PlayerRuneData(player));

        CooldownHandler oldRune = runeData.getPrimarySlot3Rune();
        if (oldRune != null) {
            oldRune.onDisable(player);
        }

        runeData.setPrimarySlot3Rune(rune);
        rune.onEnable(player);
        playerRuneData.put(uuid, runeData);
    }

    public void setPlayerSecondarySlot1Rune(Player player, CooldownHandler rune) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.getOrDefault(uuid, new PlayerRuneData(player));

        CooldownHandler oldRune = runeData.getSecondarySlot1Rune();
        if (oldRune != null) {
            oldRune.onDisable(player);
        }

        runeData.setSecondarySlot1Rune(rune);
        rune.onEnable(player);
        playerRuneData.put(uuid, runeData);
    }

    public void setPlayerSecondarySlot2Rune(Player player, CooldownHandler rune) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.getOrDefault(uuid, new PlayerRuneData(player));

        CooldownHandler oldRune = runeData.getSecondarySlot2Rune();
        if (oldRune != null) {
            oldRune.onDisable(player);
        }

        runeData.setSecondarySlot2Rune(rune);
        rune.onEnable(player);
        playerRuneData.put(uuid, runeData);
    }

    public void setPlayerShardSlot1Rune(Player player, CooldownHandler rune) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.getOrDefault(uuid, new PlayerRuneData(player));

        CooldownHandler oldRune = runeData.getShardSlot1Rune();
        if (oldRune != null) {
            oldRune.onDisable(player);
        }

        runeData.setShardSlot1Rune(rune);
        rune.onEnable(player);
        playerRuneData.put(uuid, runeData);
    }

    public void setPlayerShardSlot2Rune(Player player, CooldownHandler rune) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.getOrDefault(uuid, new PlayerRuneData(player));

        CooldownHandler oldRune = runeData.getShardSlot2Rune();
        if (oldRune != null) {
            oldRune.onDisable(player);
        }

        runeData.setShardSlot2Rune(rune);
        rune.onEnable(player);
        playerRuneData.put(uuid, runeData);
    }

    public void setPlayerShardSlot3Rune(Player player, CooldownHandler rune) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.getOrDefault(uuid, new PlayerRuneData(player));

        CooldownHandler oldRune = runeData.getShardSlot3Rune();
        if (oldRune != null) {
            oldRune.onDisable(player);
        }

        runeData.setShardSlot3Rune(rune);
        rune.onEnable(player);
        playerRuneData.put(uuid, runeData);
    }

    public void clearPlayerRunes(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.get(uuid);

        if (runeData != null) {
            for (CooldownHandler rune : runeData.getAllRunes()) {
                if (rune != null) {
                    rune.onDisable(player);
                }
            }

            runeData.setKeystoneRune(null);
            runeData.setPrimaryPath(null);
            runeData.setSecondaryPath(null);
            runeData.setPrimarySlot1Rune(null);
            runeData.setPrimarySlot2Rune(null);
            runeData.setPrimarySlot3Rune(null);
            runeData.setSecondarySlot1Rune(null);
            runeData.setSecondarySlot2Rune(null);
            runeData.setShardSlot1Rune(null);
            runeData.setShardSlot2Rune(null);
            runeData.setShardSlot3Rune(null);
        }
    }

    public void setPlayerPrimaryPath(Player player, RunePath path) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.computeIfAbsent(uuid, k -> new PlayerRuneData(player));
        runeData.setPrimaryPath(path);
    }

    public void setPlayerSecondaryPath(Player player, RunePath path) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.computeIfAbsent(uuid, k -> new PlayerRuneData(player));
        runeData.setSecondaryPath(path);
    }

    public void clearPlayerPrimaryTree(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.get(uuid);
        if (runeData == null) return;

        for (CooldownHandler rune : new CooldownHandler[]{
                runeData.getKeystoneRune(),
                runeData.getPrimarySlot1Rune(),
                runeData.getPrimarySlot2Rune(),
                runeData.getPrimarySlot3Rune()}) {
            if (rune != null) rune.onDisable(player);
        }

        runeData.setPrimaryPath(null);
        runeData.setKeystoneRune(null);
        runeData.setPrimarySlot1Rune(null);
        runeData.setPrimarySlot2Rune(null);
        runeData.setPrimarySlot3Rune(null);
    }

    public void clearPlayerSecondaryTree(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerRuneData runeData = playerRuneData.get(uuid);
        if (runeData == null) return;

        for (CooldownHandler rune : new CooldownHandler[]{
                runeData.getSecondarySlot1Rune(),
                runeData.getSecondarySlot2Rune()}) {
            if (rune != null) rune.onDisable(player);
        }

        runeData.setSecondaryPath(null);
        runeData.setSecondarySlot1Rune(null);
        runeData.setSecondarySlot2Rune(null);
    }
}