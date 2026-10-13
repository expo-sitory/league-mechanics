package dev.ixpu.leaguemechanics.entity.player.abilities;

public enum AbilityEffectType {
    SHIELD_OF_DURAND("shield-of-durand", "Shield of Durand", "passives.galio_durand", "§e", 10.0),
    FINAL_HOUR("final-hour", "Final Hour", "passives.vayne_final_hour", "§9", 10.0),
    UNDYING_RAGE("undying-rage", "Undying Rage", "passives.tryndamere_undying_rage", "§c", 10.0),
    RAGNAROK("ragnarok", "Ragnarok", "passives.ragnarok", "§6", 10.0),
    HOLLOWED_MIST("hollowed-mist", "Hollowed Mist", "passives.hollowed_mist", "§b", 5.0),
    SANGUINE_POOL("saguine-pool", "Sanguine Pool", "passives.saguine_pool", "§4", 4.0),
    LAMBS_RESPITE("lambs-sprite", "Lamb's Respite", "passives.lambs_sprite", "§1", 5.0),
    SPRAY_AND_PRAY("spray-and-pray", "Spray and Pray", "passives.spray_and_pray", "§a", 12.0),
    SURROUND_SOUND("surround-sounds", "Surround Sound", "passives.surround_sounds", "§e", 5.0),
    INSPIRE("inspire", "Inspire", "passives.inspire", "§2", 7.0),
    MEDITATE("meditate", "Meditate", "passives.meditate", "§a", 5.0),
    BATTLE_ROAR("battle-roar", "Battle Roar", "passives.battle_roar", "§6", 3.0),
    COSMIC_RADIANCE("cosmic-radiance", "Cosmic Radiance", "passives.cosmic_radiance", "§9", 3.0),
    INVULNERABLE("invulnerable", "Invulnerable", "status.invulnerable", "§e", 2.0),
    SEISMIC_BASTION("seismic-bastion", "Seismic Bastion", "passives.seismic_field", "§e", 7.0);

    private final String configKey;
    private final String displayName;
    private final String iconKey;
    private final String timerColor;
    private final double defaultDurationSeconds;

    AbilityEffectType(String configKey, String displayName, String iconKey, String timerColor, double defaultDurationSeconds) {
        this.defaultDurationSeconds = defaultDurationSeconds;
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

    public double getDefaultDurationSeconds() {
        return defaultDurationSeconds;
    }

    public String getTimerColor() {
        return timerColor;
    }
}