package dev.ixpu.leaguemechanics.rune.paths.sorcery;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.paths.RunePath;
import dev.ixpu.leaguemechanics.rune.slots.RuneSlot;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;
import dev.ixpu.leaguemechanics.util.Icons;
import dev.ixpu.leaguemechanics.util.DebugLogger;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class NimbusCloak extends CooldownHandler {

    private static final int DECAY_SECONDS = 2; // 2 second decay time
    private static final int BASE_MOVEMENT_SPEED = 15; // 15 * levelBasedBonus()

    // Track movement speed bonus amounts and expiration times for players
    private static final Map<UUID, Double> speedBonusAmounts = new HashMap<>();
    private static final Map<UUID, Long> speedBonusExpiry = new HashMap<>();

    private PlayerEventListener listener;

    public NimbusCloak(ConfigurationSection config, PlayerEventListener listener) {
        super("nimbus-cloak", RunePath.SORCERY, RuneSlot.PRIMARY_SLOT_3);
        this.listener = listener;

        ConfigurationSection section = config.getConfigurationSection("runes.primary-slots.sorcery.slot-3.nimbus-cloak");
        if (section != null) {
            // Config values can be read here if needed for future customization
        }
        // This rune doesn't have a traditional cooldown - it triggers on class ability use
        // The effect is temporary and decays over time
    }

    /**
     * Called when a class ability is used to grant the movement speed bonus.
     *
     * @param player The player who used the class ability
     */
    public void onClassAbilityUsed(Player player) {
        // Check if we can let runes through
        if (!listener.letRunesThrough(player)) {
            return;
        }

        // Calculate the movement speed bonus: 15 * levelBasedBonus()
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        double levelBasedBonus = playerStats.levelBasedBonus();
        double speedBonus = BASE_MOVEMENT_SPEED * levelBasedBonus;

        // Apply the temporary movement speed modifier
        playerStats.temporaryMSModification += speedBonus;

        // Track the bonus amount and expiry time for this player
        UUID playerUUID = player.getUniqueId();
        speedBonusAmounts.put(playerUUID, speedBonus);
        long expiryTime = System.currentTimeMillis() + (DECAY_SECONDS * 1000L);
        speedBonusExpiry.put(playerUUID, expiryTime);

        // Debug message
        dev.ixpu.leaguemechanics.util.DebugLogger.debug(player, "§f[§dSource§f] [§eNimbus Cloak§f] Granted §6" +
                String.format("%.1f", speedBonus) + " §fmovement speed (decays over " + DECAY_SECONDS + "s)");
    }

    @Override
    public void tick(Player player) {
        // Check if the speed bonus has expired and remove it if so
        UUID playerUUID = player.getUniqueId();
        Long expiryTime = speedBonusExpiry.get(playerUUID);
        Double bonusAmount = speedBonusAmounts.get(playerUUID);

        if (expiryTime != null && bonusAmount != null && System.currentTimeMillis() >= expiryTime) {
            // Remove the specific speed bonus we added
            PlayerStats playerStats = PlayerStats.getOrCreate(player);
            playerStats.temporaryMSModification -= bonusAmount;

            // Ensure we don't go below zero (though it shouldn't happen with proper tracking)
            if (playerStats.temporaryMSModification < 0) {
                playerStats.temporaryMSModification = 0.0;
            }

            // Remove tracking for this player
            speedBonusAmounts.remove(playerUUID);
            speedBonusExpiry.remove(playerUUID);
        }

        // Update display
        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        String statsDisplay = playerStats.getActionBarSections(player);
        String runeDisplay = getDisplaySection(player);
        String actionBarMessage = runeDisplay + " " + statsDisplay;
        player.sendActionBar(Component.text(actionBarMessage));
    }

    @Override
    public String getDisplaySection(Player player) {
        // Show movement speed bonus if active
        UUID playerUUID = player.getUniqueId();
        Long expiryTime = speedBonusExpiry.get(playerUUID);
        Double bonusAmount = speedBonusAmounts.get(playerUUID);

        if (expiryTime != null && bonusAmount != null && System.currentTimeMillis() < expiryTime) {
            long secondsRemaining = (expiryTime - System.currentTimeMillis()) / 1000;
            return Icons.get("runes-slots.nimbus-cloak") + " §f+" + String.format("%.1f", bonusAmount) + " §6ms";
        }

        return Icons.get("runes-slots.nimbus-cloak");
    }

    private boolean letRunesThrough(Player player) {
        return LeagueMechanics.getInstance().getPlayerEventListener().letRunesThrough(player);
    }
}