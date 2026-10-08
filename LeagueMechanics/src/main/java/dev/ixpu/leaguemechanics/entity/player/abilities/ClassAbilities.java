package dev.ixpu.leaguemechanics.entity.player.abilities;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class ClassAbilities {
    private final String id;
    private final String displayName;
    private final PlayerClassType classType;
    private final String iconKey;
    private final String model;
    private final String fallbackModel;
    protected final LeagueMechanics plugin;

    protected ClassAbilities(LeagueMechanics plugin, String id, String displayName, PlayerClassType classType,
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

    public double getMinCooldownSeconds() {
        return plugin.getConfig().getDouble("skills." + id + ".min-cooldown-seconds", 0.0);
    }

    public abstract double getCooldownSeconds();

    public double getEffectSeconds() {
        return 0.0;
    }

    public boolean startsCooldownAfterEffect() {
        return plugin.getConfig().getBoolean("skills." + id + ".cooldown-after-effect", false);
    }
    public int getHungerCost() {
        return Math.max(0, plugin.getConfig().getInt("skills." + id + ".hunger-cost", 0));
    }

    public int getHungerRequired() {
        return Math.max(0, plugin.getConfig().getInt("skills." + id + ".hunger-required", getHungerCost()));
    }

    public abstract List<String> getDescription();

    public boolean isChargeable() {
        return false;
    }

    public boolean isCharging(Player player) {
        return false;
    }

    public void interruptCharge(Player player) {}

    public abstract boolean cast(Player caster);
}