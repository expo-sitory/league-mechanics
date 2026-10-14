package dev.ixpu.leaguemechanics.rune.paths.sorcery;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.paths.RunePath;
import dev.ixpu.leaguemechanics.rune.slots.RuneSlot;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import dev.ixpu.leaguemechanics.debuff.DebuffType;
import dev.ixpu.leaguemechanics.util.Icons;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class ManaflowBand extends CooldownHandler {

    private static int COOLDOWN_SECONDS = 20; // 20 second cooldown
    private static final int SATURATION_AMOUNT = 10; // 10 saturation granted

    // Track when players last gained saturation from this rune
    private static final Map<UUID, Long> lastSaturationTime = new HashMap<>();

    private PlayerEventListener listener;

    public ManaflowBand(ConfigurationSection config, PlayerEventListener listener) {
        super("manaflow-band", RunePath.SORCERY, RuneSlot.PRIMARY_SLOT_2);
        this.listener = listener;

        ConfigurationSection section = config.getConfigurationSection("runes.primary-slots.sorcery.slot-2.manaflow-band");
        if (section != null) {
            // Config values can be read here if needed for future customization
        }
        // Set the cooldown duration for this rune
        this.setCooldownSeconds(COOLDOWN_SECONDS);
    }

    /**
     * Called when a class ability deals damage to check if we should grant saturation.
     *
     * @param player The player who used the class ability
     * @param target The target that was damaged
     * @param damageAmount The amount of damage dealt
     */
    public void onClassAbilityDamage(Player player, Entity target, double damageAmount) {
        if (damageAmount <= 0) return;
        grantSaturationIfPossible(player);
    }

    /**
     * Called when a class ability applies crowd control to check if we should grant saturation.
     *
     * @param player The player who used the class ability
     * @param target The target that received crowd control
     * @param debuffType The type of crowd control applied
     */
    public void onClassAbilityCrowdControl(Player player, Entity target, DebuffType debuffType) {
        if (debuffType == null) return;
        grantSaturationIfPossible(player);
    }

    /**
     * Grants saturation to the player if the rune is not on cooldown.
     *
     * @param player The player to grant saturation to
     */
    private void grantSaturationIfPossible(Player player) {
        // Check if on cooldown
        if (isOnCooldown(player)) {
            return;
        }

        // Check if we can let runes through
        if (!listener.letRunesThrough(player)) {
            return;
        }

        // Grant saturation
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        // Add saturation - this would need to be implemented in PlayerStats or through food system
        // For now, we'll use a temporary approach

        // Start cooldown
        resetCooldown(player);

        // Track when we last granted saturation
        UUID playerUUID = player.getUniqueId();
        lastSaturationTime.put(playerUUID, System.currentTimeMillis());

        // Debug message
        dev.ixpu.leaguemechanics.util.DebugLogger.debug(player, "§f[§dSource§f] [§eManaflow Band§f] Granted §6" + SATURATION_AMOUNT + " §fsaturation");
    }

    @Override
    public void tick(Player player) {
        // Update display based on cooldown state
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);
        String runeDisplay = getDisplaySection(player);
        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    @Override
    public String getDisplaySection(Player player) {
        // Show cooldown if active, otherwise show rune icon
        if (isOnCooldown(player)) {
            double remaining = getRemainingCooldown(player);
            return Icons.get("runes-slots.manaflow-band") + " §7(" + String.format("%.1fs", remaining) + ")";
        }
        return Icons.get("runes-slots.manaflow-band");
    }

    private boolean letRunesThrough(Player player) {
        return LeagueMechanics.getInstance().getPlayerEventListener().letRunesThrough(player);
    }
}