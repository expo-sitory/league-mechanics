package dev.ixpu.leaguemechanics.placeholder;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClass;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneShard;
import dev.ixpu.leaguemechanics.rune.shards.ShardStats;
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

            case "level" -> String.valueOf(stats.getLeagueLevel());
            case "rune_path" -> getRunePathPlaceholder(player);
            case "class"      -> getClassPlaceholder(player);

            case "kda" -> dev.ixpu.leaguemechanics.entity.player.PlayerKDA.getInstance().getFormattedKDA(player.getUniqueId());

            case "line1" -> getLine1(player, stats);
            case "line2" -> getLine2(player, stats);
            case "line3" -> getLine3(player, stats);
            case "line4" -> getLine4(player, stats);

            case "shard-1" -> {
                RuneShard shard1 = stats.getRuneShards(player).getRow1();
                yield shard1 != null ? shard1.getDisplay() : "None";
            }
            case "shard-2" -> {
                RuneShard shard2 = stats.getRuneShards(player).getRow2();
                yield shard2 != null ? shard2.getDisplay() : "None";
            }
            case "shard-3" -> {
                RuneShard shard3 = stats.getRuneShards(player).getRow3();
                yield shard3 != null ? shard3.getDisplay() : "None";
            }

            case "shard-1_stats" -> getShard1StatsPlaceholder(player, stats);
            case "shard-2_stats" -> getShard2StatsPlaceholder(player, stats);
            case "shard-3_stats" -> getShard3StatsPlaceholder(player, stats);

            default -> null;
        };
    }

    private String getShard1StatsPlaceholder(Player player, PlayerStats stats) {
        RuneShard shard = stats.getRuneShards(player).getRow1();
        ShardStats shardStats = stats.getRuneShards(player);

        if (shard == null) {
            return "None";
        }

        return switch (shard) {
            case ROW1_ADAP -> {
                double value = shardStats.getAdOrAp(player);
                double af = stats.getPlayerAF(player);
                String type = af <= 0.7 ? "AD" : "AP";
                yield String.format("+%.1f %s", value, type);
            }
            case ROW1_AS -> {
                int value = shardStats.getAttackSpeed();
                yield value > 0 ? String.format("+%d Attack Speed", value) : "None";
            }
            case ROW1_CH -> {
                int value = shardStats.getCooldownHaste();
                yield value > 0 ? String.format("+%d Cooldown Haste", value) : "None";
            }
            default -> "None";
        };
    }

    private String getShard2StatsPlaceholder(Player player, PlayerStats stats) {
        RuneShard shard = stats.getRuneShards(player).getRow2();
        ShardStats shardStats = stats.getRuneShards(player);

        if (shard == null) {
            return "None";
        }

        return switch (shard) {
            case ROW2_ADAP -> {
                double value = shardStats.getAdOrAp(player);
                double af = stats.getPlayerAF(player);
                String type = af <= 0.7 ? "AD" : "AP";
                yield String.format("+%.1f %s", value, type);
            }
            case ROW2_MS -> {
                int value = shardStats.getMovementSpeed();
                yield value > 0 ? String.format("+%d Movement Speed", value) : "None";
            }
            case ROW2_HR -> {
                int value = shardStats.getHealthRegen();
                yield value > 0 ? String.format("+%d Health Regen", value) : "None";
            }
            default -> "None";
        };
    }

    private String getShard3StatsPlaceholder(Player player, PlayerStats stats) {
        RuneShard shard = stats.getRuneShards(player).getRow3();
        ShardStats shardStats = stats.getRuneShards(player);

        if (shard == null) {
            return "None";
        }

        return switch (shard) {
            case ROW3_HP -> {
                int value = shardStats.getHealth();
                yield value > 0 ? String.format("+%d Health", value) : "None";
            }
            case ROW3_TN -> {
                int value = shardStats.getTenacity();
                yield value > 0 ? String.format("+%d Tenacity", value) : "None";
            }
            case ROW3_HR -> {
                int value = shardStats.getHealthRegen();
                yield value > 0 ? String.format("+%d Health Regen", value) : "None";
            }
            default -> "None";
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