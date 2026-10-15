package dev.ixpu.leaguemechanics.entity.player.abilities;

import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClassAbilitiesRegistry {
    private static ClassAbilitiesRegistry instance;

    private final Map<String, ClassAbilities> skills = new LinkedHashMap<>();
    private final Map<UUID, Long> forcedChannels = new HashMap<>();

    public static ClassAbilitiesRegistry getInstance() {
        if (instance == null) {
            instance = new ClassAbilitiesRegistry();
        }
        return instance;
    }

    public void register(ClassAbilities skill) {
        skills.put(skill.getId().toLowerCase(), skill);
    }

    public ClassAbilities get(String id) {
        if (id == null) return null;
        return skills.get(id.toLowerCase());
    }

    public List<ClassAbilities> forClass(PlayerClassType type) {
        List<ClassAbilities> result = new ArrayList<>();
        if (type == null) return result;
        for (ClassAbilities skill : skills.values()) {
            if (skill.getClassType() == type) {
                result.add(skill);
            }
        }
        return result;
    }

    public void forceChannel(Player player, double seconds) {
        if (player == null) return;
        if (seconds <= 0) {
            forcedChannels.remove(player.getUniqueId());
            return;
        }
        forcedChannels.put(player.getUniqueId(), System.currentTimeMillis() + (long) (seconds * 1000));
    }

    public void stopChanneling(Player player) {
        if (player == null) return;
        forcedChannels.remove(player.getUniqueId());
        for (ClassAbilities skill : skills.values()) {
            if (skill.isCharging(player)) {
                skill.interruptCharge(player);
            }
        }
    }

    public boolean isChanneling(Player player) {
        if (player == null) return false;
        Long forcedUntil = forcedChannels.get(player.getUniqueId());
        if (forcedUntil != null) {
            if (System.currentTimeMillis() < forcedUntil) return true;
            forcedChannels.remove(player.getUniqueId());
        }
        for (ClassAbilities skill : skills.values()) {
            if (skill.isCharging(player)) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        skills.clear();
    }
}