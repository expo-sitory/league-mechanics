package dev.ixpu.leaguemechanics.entity.player.abilities;

public enum AbilityEffectType {
    SHIELD_OF_DURAND("shield-of-durand", "Shield of Durand", "passives.galio_durand", "§e"),
    FINAL_HOUR("final-hour", "Final Hour", "passives.vayne_final_hour", "§c"),
    UNDYING_RAGE("undying-rage", "Undying Rage", "passives.tryndamere_undying_rage", "§6");

    private final String configKey;
    private final String displayName;
    private final String iconKey;
    private final String timerColor;

    AbilityEffectType(String configKey, String displayName, String iconKey, String timerColor) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.iconKey = iconKey;
        this.timerColor = timerColor;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconKey() {
        return iconKey;
    }

    public String getTimerColor() {
        return timerColor;
    }
}