package dev.ixpu.leaguemechanics.manager;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class KillSourceTracker {

    private static final KillSourceTracker INSTANCE = new KillSourceTracker();

    private final Map<UUID, UUID> victimToSource = new ConcurrentHashMap<>();

    private KillSourceTracker() {}

    public static KillSourceTracker getInstance() {
        return INSTANCE;
    }

    public void setSource(Player victim, Player source) {
        if (victim == null || source == null) return;
        victimToSource.put(victim.getUniqueId(), source.getUniqueId());
    }

    public Player getAndClearSource(Player victim) {
        if (victim == null) return null;
        UUID sourceUUID = victimToSource.remove(victim.getUniqueId());
        if (sourceUUID == null) return null;
        return victim.getServer().getPlayer(sourceUUID);
    }


    public void clearSource(Player victim) {
        if (victim == null) return;
        victimToSource.remove(victim.getUniqueId());
    }

    public void clearAll() {
        victimToSource.clear();
    }
}
