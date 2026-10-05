package dev.ixpu.leaguemechanics.util;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.paths.RunePath;
import dev.ixpu.leaguemechanics.rune.slots.RuneSlot;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public abstract class StacksHandler extends CooldownHandler {

    protected int maxStacks;
    protected int stackResetTicks = 0;

    protected boolean expiryEnabled = false;
    protected boolean isPerTargetExpiry = false;
    protected boolean isPerTargetMode = false;

    protected final Map<UUID, UUID> lastTarget = new HashMap<>();
    protected final Map<UUID, Integer> playerStacks = new HashMap<>();
    protected final Map<UUID, Integer> stackExpiryTicks = new HashMap<>();
    protected final Map<UUID, Map<UUID, Integer>> perTargetStackExpiryTicks = new HashMap<>();
    protected final Map<UUID, Map<UUID, Integer>> perTargetStacks = new HashMap<>();

    protected void enablePerTargetStacking() {
        this.isPerTargetMode = true;
    }
    protected void enablePerTargetExpiry() {
        this.isPerTargetExpiry = true;
    }
    protected void onStackAdded(Player player, int newStackCount) {}
    protected void onStacksReset(Player player) {}
    protected void onStacksExpired(Player player) {}

    @Override
    public void onDisable(Player player) {
        resetStacks(player);
    }

    public StacksHandler(String id, RunePath path, RuneSlot slot, int maxStacks) {
        super(id, path, slot);
        this.maxStacks = maxStacks;
        this.hasStacking = true;
    }

    public StacksHandler(String id, RunePath path, RuneSlot slot, int maxStacks, int stackResetTicks) {
        super(id, path, slot);
        this.maxStacks = maxStacks;
        this.stackResetTicks = stackResetTicks;
        this.expiryEnabled = stackResetTicks > 0;
        this.hasStacking = true;
    }

    public int getStacks(Player player) {
        return playerStacks.getOrDefault(player.getUniqueId(), 0);
    }

    public int getStacks(Player player, UUID targetUUID) {
        if (!isPerTargetMode) {
            return getStacks(player);
        }
        Map<UUID, Integer> targetMap = perTargetStacks.get(player.getUniqueId());
        if (targetMap == null) return 0;
        Integer value = targetMap.get(targetUUID);
        return value != null ? value : 0;
    }

    public void addStack(Player player) {
        UUID uuid = player.getUniqueId();
        int currentStacks = playerStacks.getOrDefault(uuid, 0);

        if (currentStacks < maxStacks) {
            int newStacks = currentStacks + 1;
            playerStacks.put(uuid, newStacks);

            if (expiryEnabled) {
                stackExpiryTicks.put(uuid, stackResetTicks);
            }

            onStackAdded(player, newStacks);
        }
    }

    public void addStack(Player player, UUID targetUUID) {
        if (!isPerTargetMode) {
            addStack(player);
            return;
        }

        UUID playerUUID = player.getUniqueId();
        Map<UUID, Integer> targetMap = perTargetStacks.computeIfAbsent(playerUUID, k -> new HashMap<>());
        int currentStacks = targetMap.getOrDefault(targetUUID, 0);

        if (currentStacks < maxStacks) {
            int newStacks = currentStacks + 1;
            targetMap.put(targetUUID, newStacks);

            if (expiryEnabled) {
                if (isPerTargetExpiry) {
                    Map<UUID, Integer> targetExpiry = perTargetStackExpiryTicks.computeIfAbsent(playerUUID, k -> new HashMap<>());
                    targetExpiry.put(targetUUID, stackResetTicks);
                } else {
                    stackExpiryTicks.put(playerUUID, stackResetTicks);
                }
            }

            onStackAdded(player, newStacks);
        }
    }

    public void switchTarget(Player player, UUID newTargetUUID) {
        if (!isPerTargetMode) return;

        UUID playerUUID = player.getUniqueId();
        UUID previousTarget = lastTarget.get(playerUUID);

        if (previousTarget != null && !previousTarget.equals(newTargetUUID)) {
            Map<UUID, Integer> targetMap = perTargetStacks.get(playerUUID);
            if (targetMap != null) {
                targetMap.clear();
            }
        }

        lastTarget.put(playerUUID, newTargetUUID);
    }

    public void resetStacks(Player player) {
        UUID uuid = player.getUniqueId();
        playerStacks.remove(uuid);
        if (isPerTargetMode) {
            perTargetStacks.remove(uuid);
            lastTarget.remove(uuid);
        }
        if (expiryEnabled) {
            stackExpiryTicks.remove(uuid);
        }
        if (isPerTargetExpiry) {
            perTargetStackExpiryTicks.remove(uuid);
        }
        onStacksReset(player);
    }

    public void resetStacksForTarget(Player player, UUID targetUUID) {
        if (!isPerTargetMode) {
            resetStacks(player);
            return;
        }

        UUID playerUUID = player.getUniqueId();
        Map<UUID, Integer> targetMap = perTargetStacks.get(playerUUID);
        if (targetMap != null) {
            targetMap.remove(targetUUID);
        }
        if (isPerTargetExpiry) {
            Map<UUID, Integer> targetExpiry = perTargetStackExpiryTicks.get(playerUUID);
            if (targetExpiry != null) {
                targetExpiry.remove(targetUUID);
            }
        }
    }

    public void clearAllStacks() {
        playerStacks.clear();
        perTargetStacks.clear();
        lastTarget.clear();
        if (expiryEnabled) {
            stackExpiryTicks.clear();
            perTargetStackExpiryTicks.clear();
        }
    }

    protected void tickStackExpiry(Player player) {
        if (!expiryEnabled) return;

        UUID uuid = player.getUniqueId();

        if (isPerTargetExpiry) {
            tickPerTargetExpiry(player);
        } else {
            tickPlayerExpiry(player);
        }
    }

    private void tickPlayerExpiry(Player player) {
        UUID playerUUID = player.getUniqueId();
        int expiry = stackExpiryTicks.getOrDefault(playerUUID, 0);

        if (expiry > 0) {
            expiry--;
            stackExpiryTicks.put(playerUUID, expiry);

            if (expiry == 0) {
                if (isPerTargetMode) {
                    Map<UUID, Integer> targetMap = perTargetStacks.get(playerUUID);
                    if (targetMap != null) {
                        targetMap.clear();
                    }
                    lastTarget.put(playerUUID, null);
                } else {
                    playerStacks.put(playerUUID, 0);
                }
                onStacksExpired(player);
            }
        }
    }

    private void tickPerTargetExpiry(Player player) {
        UUID playerUUID = player.getUniqueId();
        Map<UUID, Integer> targetExpiry = perTargetStackExpiryTicks.get(playerUUID);
        if (targetExpiry == null) return;

        List<UUID> expiredTargets = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : targetExpiry.entrySet()) {
            int newTime = entry.getValue() - 1;
            if (newTime <= 0) {
                expiredTargets.add(entry.getKey());
            } else {
                entry.setValue(newTime);
            }
        }

        Map<UUID, Integer> targetMap = perTargetStacks.get(playerUUID);
        if (targetMap != null) {
            for (UUID targetUUID : expiredTargets) {
                targetMap.remove(targetUUID);
                targetExpiry.remove(targetUUID);
            }
        }

        if (!expiredTargets.isEmpty()) {
            onStacksExpired(player);
        }

        if (targetMap != null && targetMap.isEmpty()) {
            lastTarget.put(playerUUID, null);
        }
    }
}