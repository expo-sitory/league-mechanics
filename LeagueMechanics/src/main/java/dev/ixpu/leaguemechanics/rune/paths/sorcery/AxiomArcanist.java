package dev.ixpu.leaguemechanics.rune.paths.sorcery;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.paths.RunePath;
import dev.ixpu.leaguemechanics.rune.slots.RuneSlot;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;
import dev.ixpu.leaguemechanics.util.Icons;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import net.kyori.adventure.text.Component;

public class AxiomArcanist extends CooldownHandler {

    private static final double DAMAGE_BONUS_PERCENT = 12.0; // 12% damage increase
    private static final double HEALING_BONUS_PERCENT = 12.0; // 12% healing increase
    private static final double SHIELDING_BONUS_PERCENT = 12.0; // 12% shielding increase

    private PlayerEventListener listener;

    public AxiomArcanist(ConfigurationSection config, PlayerEventListener listener) {
        super("axiom-arcanist", RunePath.SORCERY, RuneSlot.PRIMARY_SLOT_1);
        this.listener = listener;

        ConfigurationSection section = config.getConfigurationSection("runes.primary-slots.sorcery.slot-1.axiom-arcanist");
        if (section != null) {
            // Config values can be read here if needed for future customization
        }
        // Set cooldown to 0 for passive effect
        this.setCooldownSeconds(0.0);
    }

    @Override
    public void onEnable(Player player) {
        // Passive rune - no special enable logic needed
    }

    @Override
    public void onDisable(Player player) {
        // Passive rune - no special disable logic needed
    }

    /**
     * Gets the damage bonus multiplier for class abilities.
     *
     * @param player The player to check
     * @return Damage multiplier (1.0 + bonus percent/100)
     */
    public double getClassAbilityDamageBonus(Player player) {
        if (!listener.letRunesThrough(player)) {
            return 1.0;
        }
        return 1.0 + (DAMAGE_BONUS_PERCENT / 100.0);
    }

    /**
     * Gets the healing bonus multiplier for class abilities.
     *
     * @param player The player to check
     * @return Healing multiplier (1.0 + bonus percent/100)
     */
    public double getClassAbilityHealingBonus(Player player) {
        if (!listener.letRunesThrough(player)) {
            return 1.0;
        }
        return 1.0 + (HEALING_BONUS_PERCENT / 100.0);
    }

    /**
     * Gets the shielding/absorption bonus multiplier for class abilities.
     *
     * @param player The player to check
     * @return Shielding multiplier (1.0 + bonus percent/100)
     */
    public double getClassAbilityShieldingBonus(Player player) {
        if (!listener.letRunesThrough(player)) {
            return 1.0;
        }
        return 1.0 + (SHIELDING_BONUS_PERCENT / 100.0);
    }

    @Override
    public void tick(Player player) {
        // Passive rune - no ticking logic needed for core functionality
        // But we still need to update the display
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);
        String runeDisplay = getDisplaySection(player);
        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    @Override
    public String getDisplaySection(Player player) {
        // Show the rune icon with bonus information
        return Icons.get("runes-slots.axiom-arcanist") + " §f+12% §6CAry"; // CAry = Class Ability
    }

    private boolean letRunesThrough(Player player) {
        return LeagueMechanics.getInstance().getPlayerEventListener().letRunesThrough(player);
    }
}