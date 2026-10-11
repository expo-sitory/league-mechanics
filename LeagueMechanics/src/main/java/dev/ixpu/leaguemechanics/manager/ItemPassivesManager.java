package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemPassivesManager {
    private static ItemPassivesManager instance;

    private final Map<String, Map<UUID, Integer>> killCounts = new HashMap<>();
    private final Map<String, Map<UUID, Boolean>> passiveDisabled = new HashMap<>();
    private final Map<String, CooldownHandler> passiveCooldowns = new HashMap<>();

    public static ItemPassivesManager getInstance() {
        if (instance == null) {
            instance = new ItemPassivesManager();
        }
        return instance;
    }

    public int getKillCount(Player player, String itemId) {
        UUID uuid = player.getUniqueId();
        return killCounts.computeIfAbsent(itemId, k -> new HashMap<>()).getOrDefault(uuid, 0);
    }

    public void addKill(Player player, String itemId) {
        UUID uuid = player.getUniqueId();
        killCounts.computeIfAbsent(itemId, k -> new HashMap<>()).put(uuid, getKillCount(player, itemId) + 1);
    }

    public boolean isPassiveDisabled(Player player, String itemId) {
        UUID uuid = player.getUniqueId();
        return passiveDisabled.computeIfAbsent(itemId, k -> new HashMap<>()).getOrDefault(uuid, false);
    }

    public void disablePassive(Player player, String itemId) {
        UUID uuid = player.getUniqueId();
        passiveDisabled.computeIfAbsent(itemId, k -> new HashMap<>()).put(uuid, true);
    }

    public void setKillCount(Player player, String itemId, int count) {
        UUID uuid = player.getUniqueId();
        killCounts.computeIfAbsent(itemId, k -> new HashMap<>()).put(uuid, count);
    }

    public boolean isOnCooldown(Player player, String passiveId) {
        if (player == null) return false;
        CooldownHandler handler = passiveCooldowns.get(passiveId);
        return handler != null && handler.isOnCooldown(player);
    }

    public void setCooldown(Player player, String passiveId, int durationTicks) {
        if (player == null) return;
        CooldownHandler handler = passiveCooldowns.computeIfAbsent(passiveId, CooldownHandler::new);
        if (durationTicks <= 0) {
            handler.clearPlayerCooldown(player);
            return;
        }
        handler.setCooldownSeconds(durationTicks / 20.0);
        handler.resetCooldown(player);
    }

    public double getRemainingCooldownSeconds(Player player, String passiveId) {
        if (player == null) return 0;
        CooldownHandler handler = passiveCooldowns.get(passiveId);
        return handler == null ? 0 : handler.getRemainingCooldown(player);
    }
}