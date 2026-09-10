package dev.ixpu.leaguemechanics.placeholder;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.player.PlayerClass;
import dev.ixpu.leaguemechanics.player.PlayerClassType;
import dev.ixpu.leaguemechanics.player.PlayerStats;
import dev.ixpu.leaguemechanics.rune.RunePath;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderRegistry extends PlaceholderExpansion {

    private final LeagueMechanics plugin;

    public PlaceholderRegistry(LeagueMechanics plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "leaguemechanics";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }

        if (params == null || params.isEmpty()) {
            return "";
        }

        PlayerStats stats = PlayerStats.getOrCreate(player);

        return switch (params.toLowerCase()) {
            case "ad" -> String.format("%.0f", stats.getPlayerAD(player));
            case "ap" -> String.format("%.0f", stats.getPlayerAP(player));
            case "ar" -> String.format("%.0f", stats.getPlayerAR(player));
            case "mr" -> String.format("%.0f", stats.getPlayerMR(player));
            case "as" -> String.format("%.0f", stats.getPlayerAS(player));
            case "hp" -> String.format("%.0f", stats.getPlayerHP(player));
            case "ms" -> String.format("%.0f", stats.getPlayerMS(player));
            case "cc" -> String.format("%.0f", stats.getPlayerCC(player));
            case "hr" -> String.format("%.0f", stats.getPlayerHR(player));
            case "td" -> String.format("%.0f", stats.getPlayerTD(player));
            case "af" -> String.format("%.0f", stats.getPlayerAF(player));
            case "ch" -> String.format("%.0f", stats.getPlayerCH(player));

            case "rune_path" -> getRunePathPlaceholder(player);
            case "class"      -> getClassPlaceholder(player);

            case "kda" -> dev.ixpu.leaguemechanics.player.PlayerKDA.getInstance().getFormattedKDA(player.getUniqueId());

            case "line1" -> getLine1(player, stats);
            case "line2" -> getLine2(player, stats);
            case "line3" -> getLine3(player, stats);
            case "line4" -> getLine4(player, stats);

            default -> null;
        };
    }

    private static final java.util.Map<String, String> PATH_DISPLAY = java.util.Map.of(
            "domination",   "§c⚓ Domination",
            "precision",    "§e⌖ Precision",
            "resolve",     "§a⛨ Resolve",
            "sorcery",     "§9ᛝ Sorcery",
            "inspiration", "§3🌅 Inspiration"
    );

    private static final java.util.Map<String, String> CLASS_DISPLAY = java.util.Map.of(
            "fighter",   "§6🗡 Fighter",
            "mage",     "§9☄ Mage",
            "tank",     "§e🛡 Tank",
            "marksman", "§c🏹 Marksman",
            "assassin", "§4➷ Assasin",
            "support",  "§a❤ Support"
    );

    private String getRunePathPlaceholder(Player player) {
        if (player == null) return "§☯ No Primary Rune";
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "§☯ No Primary Rune";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        RunePath path = runeData != null ? runeData.getPrimaryPath() : null;
        if (path == null) return "§7☯ No Primary Rune";
        return PATH_DISPLAY.getOrDefault(path.getId(), "§☯ No Primary Rune");
    }

    private String getClassPlaceholder(Player player) {
        if (player == null) return "§7⚔ No Class Selected";
        PlayerClassType ct = PlayerClass.getPlayerClass(player);
        if (ct == null) return "§7⚔ No Class Selected";
        return CLASS_DISPLAY.getOrDefault(ct.getId(), "§7⚔ No Class Selected");
    }

    private String getLine1(Player player, PlayerStats stats) {
        double ad = stats.getPlayerAD(player);
        double ap = stats.getPlayerAP(player);
        return "§6🗡 §7" + String.format("%-4.0f", ad) + "  §9☄ §7" + String.format("%-4.0f", ap);
    }

    private String getLine2(Player player, PlayerStats stats) {
        double ar = stats.getPlayerAR(player);
        double mr = stats.getPlayerMR(player);
        return "§e🛡 §7" + String.format("%-4.0f", ar) + "  §b⦿ §7" + String.format("%-4.0f", mr);
    }

    private String getLine3(Player player, PlayerStats stats) {
        double as = stats.getPlayerAS(player);
        double ch = stats.getPlayerCH(player);
        double roundedAS = Math.ceil(as * 100) / 100;
        String asFormatted = String.format("%.2f", roundedAS);
        return "§c➺ §7" + String.format("%-6s", asFormatted) + "§7⌛ §7" + String.format("%-4.0f", ch);
    }

    private String getLine4(Player player, PlayerStats stats) {
        double cc = stats.getPlayerCC(player);
        double ms = stats.getPlayerMS(player);
        String ccFormatted = String.format("%.0f", cc) + "%";
        return "§0.§4➷ §7" + String.format("%-4s", ccFormatted) + "  §f👣 §7" + String.format("%-4.0f", ms);
    }
}