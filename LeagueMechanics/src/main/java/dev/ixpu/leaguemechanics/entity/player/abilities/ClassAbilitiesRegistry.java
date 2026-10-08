package dev.ixpu.leaguemechanics.entity.player.abilities;

import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassAbilitiesRegistry {
    private static ClassAbilitiesRegistry instance;

    private final Map<String, ClassAbilities> skills = new LinkedHashMap<>();

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

    public void clear() {
        skills.clear();
    }
}
