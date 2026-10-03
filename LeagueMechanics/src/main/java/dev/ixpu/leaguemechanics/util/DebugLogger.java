package dev.ixpu.leaguemechanics.util;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class DebugLogger {
    private static final LeagueMechanics plugin = LeagueMechanics.getInstance();

    public static void debug(Player player, String message) {
        if (plugin.isDebugMode()) {
            String worldName = player.getWorld().getName();
            java.util.List<String> disabledWorlds = plugin.getConfig().getStringList("general-settings.debug-disabled-worlds");

            if (!disabledWorlds.contains(worldName)) {
                player.sendMessage(Component.text(message));
            }
        }
    }
}