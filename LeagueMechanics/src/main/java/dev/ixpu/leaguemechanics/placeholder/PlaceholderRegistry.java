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

import java.util.Arrays;
import java.util.stream.Collectors;

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

            case "primary_path" -> getPrimaryPath(player);
            
            case "primary_path_id" -> getPrimaryPathID(player);
            case "primary_path_id_inactive-1" -> getInactivePrimaryPath(player, 1);
            case "primary_path_id_inactive-2" -> getInactivePrimaryPath(player, 2);
            case "primary_path_id_inactive-3" -> getInactivePrimaryPath(player, 3);
            case "primary_path_id_inactive-4" -> getInactivePrimaryPath(player, 4);
            case "primary_path_id_inactive-5" -> getInactivePrimaryPath(player, 5);
            
            case "primary_path_mm" -> getPrimaryPathMM(player);
            case "primary_path_mm_inactive-1" -> getInactivePrimaryPathMM(player, 1);
            case "primary_path_mm_inactive-2" -> getInactivePrimaryPathMM(player, 2);
            case "primary_path_mm_inactive-3" -> getInactivePrimaryPathMM(player, 3);
            case "primary_path_mm_inactive-4" -> getInactivePrimaryPathMM(player, 4);
            case "primary_path_mm_inactive-5" -> getInactivePrimaryPathMM(player, 5);

            case "secondary_path" -> getSecondaryPath(player);
            case "secondary_path_mm" -> getSecondaryPathMM(player);
            
            case "keystone" -> getKeystone(player);
            case "keystone_mm" -> getKeystoneMM(player);

            case "primary_slot-1" -> getPrimarySlot1(player);
            case "primary_slot-2" -> getPrimarySlot2(player);
            case "primary_slot-3" -> getPrimarySlot3(player);
            
            case "primary_slot-1_mm" -> getPrimarySlot1MM(player);
            case "primary_slot-2_mm" -> getPrimarySlot2MM(player);
            case "primary_slot-3_mm" -> getPrimarySlot3MM(player);

            case "secondary_slot-1" -> getSecondarySlot1(player);
            case "secondary_slot-2" -> getSecondarySlot2(player);

            case "secondary_slot-1_mm" -> getSecondarySlot1MM(player);
            case "secondary_slot-2_mm" -> getSecondarySlot2MM(player);

            case "shard-1_id" -> String.valueOf(getShardOption(1, PlayerStats.getOrCreate(player).getRuneShards(player).getRow1()));
            case "shard-2_id" -> String.valueOf(getShardOption(2, PlayerStats.getOrCreate(player).getRuneShards(player).getRow2()));
            case "shard-3_id" -> String.valueOf(getShardOption(3, PlayerStats.getOrCreate(player).getRuneShards(player).getRow3()));

            case "shard-1_mm" -> getShard1Stats(player, stats);
            case "shard-2_mm" -> getShard2Stats(player, stats);
            case "shard-3_mm" -> getShard3Stats(player, stats);

            case "shard-1_id_inactive-1" -> getInactiveShardId(player, 1, 1);
            case "shard-1_id_inactive-2" -> getInactiveShardId(player, 1, 2);
            case "shard-2_id_inactive-1" -> getInactiveShardId(player, 2, 1);
            case "shard-2_id_inactive-2" -> getInactiveShardId(player, 2, 2);
            case "shard-3_id_inactive-1" -> getInactiveShardId(player, 3, 1);
            case "shard-3_id_inactive-2" -> getInactiveShardId(player, 3, 2);

            case "shard-1_mm_inactive-1" -> getInactiveShardStats(player, 1, 1);
            case "shard-1_mm_inactive-2" -> getInactiveShardStats(player, 1, 2);
            case "shard-2_mm_inactive-1" -> getInactiveShardStats(player, 2, 1);
            case "shard-2_mm_inactive-2" -> getInactiveShardStats(player, 2, 2);
            case "shard-3_mm_inactive-1" -> getInactiveShardStats(player, 3, 1);
            case "shard-3_mm_inactive-2" -> getInactiveShardStats(player, 3, 2);

            case "class" -> getClass(player);
            case "class_mm" -> getClassMM(player);
            
            case "level" -> String.valueOf(stats.getLeagueLevel());
            case "kda" -> dev.ixpu.leaguemechanics.entity.player.PlayerKDA.getInstance().getFormattedKDA(player.getUniqueId());
            
            case "line1" -> getLine1(player, stats);
            case "line2" -> getLine2(player, stats);
            case "line3" -> getLine3(player, stats);
            case "line4" -> getLine4(player, stats);
            
            default -> null;
        };
    }

    private String getPrimaryPath(Player player) {
        if (player == null) return "§☯ No Primary Rune";
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "§☯ No Primary Rune";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        RunePath path = runeData != null ? runeData.getPrimaryPath() : null;
        if (path == null) return "§7☯ No Primary Rune";
        return PATH_DISPLAY.getOrDefault(path.getId(), "§☯ No Primary Rune");
    }

    private String getPrimaryPathID(Player player) {
        if (player == null) return "none";
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "none";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        RunePath path = runeData != null ? runeData.getPrimaryPath() : null;
        if (path == null) return "none";
        return path.getId();
    }

    private String getInactivePrimaryPath(Player player, int index) {
        if (player == null) return "none";
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "none";

        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        String activePath = runeData != null && runeData.getPrimaryPath() != null
                ? runeData.getPrimaryPath().getId()
                : null;

        String[] allPaths = {"precision", "domination", "sorcery", "resolve", "inspiration"};
        java.util.List<String> inactivePaths = new java.util.ArrayList<>();

        for (String path : allPaths) {
            if (!path.equals(activePath)) {
                inactivePaths.add(path);
            }
        }

        if (index < 1 || index > inactivePaths.size()) {
            return "none";
        }

        return inactivePaths.get(index - 1);
    }

    private String getPrimaryPathMM(Player player) {
        if (player == null) return "☯ No Primary Rune";
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "☯ No Primary Rune";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        RunePath path = runeData != null ? runeData.getPrimaryPath() : null;
        if (path == null) return "☯ No Primary Rune";
        return PATH_DISPLAY_MINIMESSAGE.getOrDefault(path.getId(), "☯ No Primary Rune");
    }

    private String getInactivePrimaryPathMM(Player player, int index) {
        if (player == null) return "None";
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "None";

        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        String activePath = runeData != null && runeData.getPrimaryPath() != null
                ? runeData.getPrimaryPath().getId()
                : null;

        String[] allPaths = {"precision", "domination", "sorcery", "resolve", "inspiration"};
        java.util.List<String> inactivePaths = new java.util.ArrayList<>();

        for (String path : allPaths) {
            if (!path.equals(activePath)) {
                inactivePaths.add(path);
            }
        }

        if (index < 1 || index > inactivePaths.size()) {
            return "None";
        }

        String pathId = inactivePaths.get(index - 1);
        return PATH_DISPLAY_MINIMESSAGE.getOrDefault(pathId, "None");
    }

    
    private String getSecondaryPath(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "§7Under Development";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getSecondaryPath() == null) return "§7Under Development";
        return runeData.getSecondaryPath().getId();
    }

    private String getSecondaryPathMM(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "Under Development";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getSecondaryPath() == null) return "Under Development";
        return runeData.getSecondaryPath().getId();
    }

    private String getKeystone(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "None";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getKeystoneRune() == null) return "None";
        return formatRuneId(runeData.getKeystoneRune().getId());
    }

    private String getKeystoneMM(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "None";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getKeystoneRune() == null) return "None";

        String keystoneId = runeData.getKeystoneRune().getId();
        String keystoneName = formatRuneId(keystoneId);
        String icon = KEYSTONE_ICONS.getOrDefault(keystoneId, "");

        RunePath path = runeData.getKeystoneRune().getPath();
        String color = path != null ? KEYSTONE_COLORS.getOrDefault(path.getId(), "<white>") : "<white>";

        return color + icon + " " + keystoneName;
    }

    private String getPrimarySlot1(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "§7Under Development";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getPrimarySlot1Rune() == null) return "§7Under Development";
        return runeData.getPrimarySlot1Rune().getId();
    }
    private String getPrimarySlot2(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "§7Under Development";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getPrimarySlot2Rune() == null) return "§7Under Development";
        return runeData.getPrimarySlot2Rune().getId();
    }
    private String getPrimarySlot3(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "§7Under Development";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getPrimarySlot3Rune() == null) return "§7Under Development";
        return runeData.getPrimarySlot3Rune().getId();
    }

    private String getPrimarySlot1MM(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "Under Development";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getPrimarySlot1Rune() == null) return "Under Development";
        return formatRuneId(runeData.getPrimarySlot1Rune().getId());
    }
    private String getPrimarySlot2MM(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "Under Development";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getPrimarySlot2Rune() == null) return "Under Development";
        return formatRuneId(runeData.getPrimarySlot2Rune().getId());
    }
    private String getPrimarySlot3MM(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "Under Development";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getPrimarySlot3Rune() == null) return "Under Development";
        return formatRuneId(runeData.getPrimarySlot3Rune().getId());
    }

    private String getSecondarySlot1(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "§7Under Development";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getSecondarySlot1Rune() == null) return "§7Under Development";
        return runeData.getSecondarySlot1Rune().getId();
    }
    private String getSecondarySlot2(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "§7Under Development";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getSecondarySlot2Rune() == null) return "§7Under Development";
        return runeData.getSecondarySlot2Rune().getId();
    }

    private String getSecondarySlot1MM(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "Under Development";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getSecondarySlot1Rune() == null) return "Under Development";
        return formatRuneId(runeData.getSecondarySlot1Rune().getId());
    }
    private String getSecondarySlot2MM(Player player) {
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "Under Development";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        if (runeData == null || runeData.getSecondarySlot2Rune() == null) return "Under Development";
        return formatRuneId(runeData.getSecondarySlot2Rune().getId());
    }

    private String getShard1Stats(Player player, PlayerStats stats) {
        RuneShard shard = stats.getRuneShards(player).getRow1();
        if (shard == null) {
            return "None";
        }
        return switch (shard) {
            case ROW1_ADAP -> "<gold>🗡 +15 </gold><white>/ </white><blue>☄ +19 </blue><white>Adaptive Force</white>";
            case ROW1_AS -> "<red>➺ +10</red><white> Attack Speed</white>";
            case ROW1_CH -> "<gray>⌛ +18</gray><white> Cooldown Haste</white>";
            default -> "None";
        };
    }
    private String getShard2Stats(Player player, PlayerStats stats) {
        RuneShard shard = stats.getRuneShards(player).getRow2();
        if (shard == null) {
            return "None";
        }
        return switch (shard) {
            case ROW2_ADAP -> "<gold>🗡 +15 </gold><white>/ </white><blue>☄ +19 </blue><white>Adaptive Force</white>";
            case ROW2_MS -> "<white>👣 +15 Movement Speed</white>";
            case ROW2_HR -> "<dark_green>❣ +2</dark_green> <white>Health Regen</white>";
            default -> "None";
        };
    }
    private String getShard3Stats(Player player, PlayerStats stats) {
        RuneShard shard = stats.getRuneShards(player).getRow3();
        if (shard == null) {
            return "None";
        }
        return switch (shard) {
            case ROW3_HP -> "<green>❤ +20</green><white> Health</white>";
            case ROW3_TN -> "<aqua>⏩ +30</aqua><white> Tenacity</white>";
            case ROW3_HR -> "<dark_green>❣ +2</dark_green> <white>Health Regen</white>";
            default -> "None";
        };
    }

    private int getShardOption(int row, RuneShard current) {
        if (row == 1) {
            if (current == RuneShard.ROW1_ADAP) return 1;
            if (current == RuneShard.ROW1_AS) return 2;
            if (current == RuneShard.ROW1_CH) return 3;
            return 1;
        } else if (row == 2) {
            if (current == RuneShard.ROW2_ADAP) return 1;
            if (current == RuneShard.ROW2_MS) return 2;
            if (current == RuneShard.ROW2_HR) return 3;
            return 1;
        } else {
            if (current == RuneShard.ROW3_HP) return 1;
            if (current == RuneShard.ROW3_TN) return 2;
            if (current == RuneShard.ROW3_HR) return 3;
            return 1;
        }
    }

    private String getInactiveShardId(Player player, int row, int index) {
        ShardStats shards = PlayerStats.getOrCreate(player).getRuneShards(player);
        RuneShard active = row == 1 ? shards.getRow1() : (row == 2 ? shards.getRow2() : shards.getRow3());

        RuneShard[] rowShards = row == 1 ?
                new RuneShard[]{RuneShard.ROW1_ADAP, RuneShard.ROW1_AS, RuneShard.ROW1_CH} :
                (row == 2 ?
                        new RuneShard[]{RuneShard.ROW2_ADAP, RuneShard.ROW2_MS, RuneShard.ROW2_HR} :
                        new RuneShard[]{RuneShard.ROW3_HP, RuneShard.ROW3_TN, RuneShard.ROW3_HR});

        java.util.List<RuneShard> inactive = new java.util.ArrayList<>();
        for (RuneShard shard : rowShards) {
            if (!shard.equals(active)) {
                inactive.add(shard);
            }
        }

        if (index < 1 || index > inactive.size()) {
            return "none";
        }

        RuneShard inactiveShard = inactive.get(index - 1);
        return String.valueOf(getShardOption(row, inactiveShard));
    }


    private String getInactiveShardStats(Player player, int row, int index) {
        ShardStats shards = PlayerStats.getOrCreate(player).getRuneShards(player);
        RuneShard active = row == 1 ? shards.getRow1() : (row == 2 ? shards.getRow2() : shards.getRow3());

        RuneShard[] rowShards = row == 1 ?
                new RuneShard[]{RuneShard.ROW1_ADAP, RuneShard.ROW1_AS, RuneShard.ROW1_CH} :
                (row == 2 ?
                        new RuneShard[]{RuneShard.ROW2_ADAP, RuneShard.ROW2_MS, RuneShard.ROW2_HR} :
                        new RuneShard[]{RuneShard.ROW3_HP, RuneShard.ROW3_TN, RuneShard.ROW3_HR});

        java.util.List<RuneShard> inactive = new java.util.ArrayList<>();
        for (RuneShard shard : rowShards) {
            if (!shard.equals(active)) {
                inactive.add(shard);
            }
        }

        if (index < 1 || index > inactive.size()) {
            return "None";
        }

        RuneShard shard = inactive.get(index - 1);

        if (row == 1) {
            return switch (shard) {
                case ROW1_ADAP -> "<gold>🗡 +15 </gold><white>/ </white><blue>☄ +19 </blue><white>Adaptive Force</white>";
                case ROW1_AS -> "<red>➺ +10</red><white> Attack Speed</white>";
                case ROW1_CH -> "<gray>⌛ +18</gray><white> Cooldown Haste</white>";
                default -> "None";
            };
        } else if (row == 2) {
            return switch (shard) {
                case ROW2_ADAP -> "<gold>🗡 +15 </gold><white>/ </white><blue>☄ +19 </blue><white>Adaptive Force</white>";
                case ROW2_MS -> "<white>👣 +15 Movement Speed</white>";
                case ROW2_HR -> "<dark_green>❣ +2</dark_green> <white>Health Regen</white>";
                default -> "None";
            };
        } else {
            return switch (shard) {
                case ROW3_HP -> "<green>❤ +20</green><white> Health</white>";
                case ROW3_TN -> "<aqua>⏩ +30</aqua><white> Tenacity</white>";
                case ROW3_HR -> "<dark_green>❣ +2</dark_green><white> Health Regen</white>";
                default -> "None";
            };
        }
    }


    private String getClass(Player player) {
        if (player == null) return "§7⚔ No Class Selected";
        PlayerClassType ct = PlayerClass.getPlayerClass(player);
        if (ct == null) return "§7⚔ No Class Selected";
        return CLASS_DISPLAY.getOrDefault(ct.getId(), "§7⚔ No Class Selected");
    }
    private String getClassMM(Player player) {
        if (player == null) return "⚔ No Class Selected";
        PlayerClassType ct = PlayerClass.getPlayerClass(player);
        if (ct == null) return "⚔ No Class Selected";
        return CLASS_DISPLAY_MINIMESSAGE.getOrDefault(ct.getId(), "⚔ No Class Selected");
    }


    private String formatRuneId(String id) {
        if (id == null || id.equals("None")) return "None";
        return Arrays.stream(id.split("-"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static final java.util.Map<String, String> KEYSTONE_COLORS = java.util.Map.of(
            "domination", "<dark_red>",
            "precision", "<gold>",
            "resolve", "<dark_green>",
            "inspiration", "<dark_aqua>",
            "sorcery", "<dark_blue>"
    );

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

    private static final java.util.Map<String, String> PATH_DISPLAY_MINIMESSAGE = java.util.Map.of(
            "domination",   "<red>⚓ Domination</red>",
            "precision",    "<yellow>⌖ Precision</yellow>",
            "resolve",     "<green>⛨ Resolve</green>",
            "sorcery",     "<blue>ᛝ Sorcery</blue>",
            "inspiration", "<aqua>🌅 Inspiration</aqua>"
    );

    private static final java.util.Map<String, String> CLASS_DISPLAY_MINIMESSAGE = java.util.Map.of(
            "fighter",   "<gold>🗡 Fighter</gold>",
            "mage",     "<blue>☄ Mage</blue>",
            "tank",     "<yellow>🛡 Tank</yellow>",
            "marksman", "<red>🏹 Marksman</red>",
            "assassin", "<dark_red>➷ Assasin</dark_red>",
            "support",  "<green>❤ Support</green>"
    );

    private static final java.util.Map<String, String> KEYSTONE_ICONS = java.util.Map.ofEntries(
            java.util.Map.entry("dark-harvest", "👻"),
            java.util.Map.entry("electrocute", "⚡"),
            java.util.Map.entry("hail-of-blades", "❛❟❛"),
            java.util.Map.entry("first-strike", "✎"),
            java.util.Map.entry("glacial-augment", "❄"),
            java.util.Map.entry("conqueror", "🪓"),
            java.util.Map.entry("fleet-footwork", "👣"),
            java.util.Map.entry("lethal-tempo", "⚚"),
            java.util.Map.entry("press-the-attack", "✳"),
            java.util.Map.entry("after-shock", "🌀"),
            java.util.Map.entry("grasp-of-the-undying", "🥊"),
            java.util.Map.entry("guardian", "❖"),
            java.util.Map.entry("arcane-comet", "🌠"),
            java.util.Map.entry("deathfire-torch", "🔥"),
            java.util.Map.entry("storm-raider-surge", "👾")
    );


    private String getLine1(Player player, PlayerStats stats) {
        double ad = stats.getPlayerAD(player) * stats.getDamageBalancer();
        double ap = stats.getPlayerAP(player) * stats.getDamageBalancer();
        return "§6🗡 §7" + String.format("%-4.0f", ad) + "  §9☄ §7" + String.format("%-4.0f", ap);
    }

    private String getLine2(Player player, PlayerStats stats) {
        double ar = stats.getPlayerAR(player) / stats.getResistanceBalancer();
        double mr = stats.getPlayerMR(player) / stats.getResistanceBalancer();
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