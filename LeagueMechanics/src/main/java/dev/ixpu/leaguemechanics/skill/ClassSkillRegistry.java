package dev.ixpu.leaguemechanics.skill;

import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassSkillRegistry {
    private static ClassSkillRegistry instance;

    private final Map<String, ClassSkill> skills = new LinkedHashMap<>();

    public static ClassSkillRegistry getInstance() {
        if (instance == null) {
            instance = new ClassSkillRegistry();
        }
        return instance;
    }

    public void register(ClassSkill skill) {
        skills.put(skill.getId().toLowerCase(), skill);
    }

    public ClassSkill get(String id) {
        if (id == null) return null;
        return skills.get(id.toLowerCase());
    }

    public List<ClassSkill> forClass(PlayerClassType type) {
        List<ClassSkill> result = new ArrayList<>();
        if (type == null) return result;
        for (ClassSkill skill : skills.values()) {
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
