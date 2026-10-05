package dev.ixpu.leaguemechanics.skill;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class ClassSkill {
    private final String id;
    private final String displayName;
    private final PlayerClassType classType;
    private final String iconKey;
    private final String model;
    private final String fallbackModel;
    protected final LeagueMechanics plugin;

    protected ClassSkill(LeagueMechanics plugin, String id, String displayName, PlayerClassType classType,
                         String iconKey, String model, String fallbackModel) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = displayName;
        this.classType = classType;
        this.iconKey = iconKey;
        this.model = model;
        this.fallbackModel = fallbackModel;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PlayerClassType getClassType() {
        return classType;
    }

    public String getIconKey() {
        return iconKey;
    }

    public String getModel() {
        return model;
    }

    public String getFallbackModel() {
        return fallbackModel;
    }

    public abstract double getCooldownSeconds();

    public abstract List<String> getDescription();

    public abstract boolean cast(Player caster);
}
