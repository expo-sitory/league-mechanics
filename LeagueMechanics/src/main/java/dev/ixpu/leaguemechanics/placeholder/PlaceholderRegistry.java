package dev.ixpu.leaguemechanics.placeholder;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClass;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneShard;
import dev.ixpu.leaguemechanics.rune.shards.ShardStats;
import dev.ixpu.leaguemechanics.util.Icons;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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


    private final Map<UUID, Integer> lastHealthBars = new HashMap<>();
    private final Map<UUID, Long> healthBlinkStartTime = new HashMap<>();

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
            case "item_count" -> String.valueOf(plugin.getStatsManager().countLeagueItems(player));
            case "kda" -> dev.ixpu.leaguemechanics.entity.player.PlayerKDA.getInstance().getFormattedKDA(player.getUniqueId());

            case "line1" -> getLine1(player, stats);
            case "line2" -> getLine2(player, stats);
            case "line3" -> getLine3(player, stats);
            case "line4" -> getLine4(player, stats);

            case "league_item_1" -> getLeagueItemIcon(player, 1);
            case "league_item_2" -> getLeagueItemIcon(player, 2);
            case "league_item_3" -> getLeagueItemIcon(player, 3);
            case "league_item_4" -> getLeagueItemIcon(player, 4);
            case "league_item_5" -> getLeagueItemIcon(player, 5);
            case "league_item_6" -> getLeagueItemIcon(player, 6);
            case "league_items" -> String.join("", collectLeagueItemIcons(player));

            case "health_absorption_bar" -> {
                UUID uuid = player.getUniqueId();
                int maxHealth = (int) player.getAttribute(Attribute.MAX_HEALTH).getValue();
                int currentHealth = (int) player.getHealth();
                double absorption = player.getAbsorptionAmount();

                int barLength = 10;
                int absorptionHearts = Math.min(barLength - 1, (int) Math.ceil((absorption / (double) maxHealth) * barLength));
                int maxHealthBars = barLength - absorptionHearts;
                int currentHealthBars = Math.max(1, Math.min(maxHealthBars, (int) ((currentHealth / (double) maxHealth) * barLength)));

                Integer prevHealthBars = lastHealthBars.getOrDefault(uuid, currentHealthBars);
                if (currentHealthBars < prevHealthBars) {
                    healthBlinkStartTime.put(uuid, System.currentTimeMillis());
                }
                lastHealthBars.put(uuid, currentHealthBars);

                boolean isBlinking = false;
                long now = System.currentTimeMillis();
                Long blinkStart = healthBlinkStartTime.get(uuid);

                if (blinkStart != null) {
                    if (now - blinkStart < 700) {
                        isBlinking = true;
                    } else {
                        healthBlinkStartTime.remove(uuid);
                    }
                }

                int lostBars = Math.max(0, prevHealthBars - currentHealthBars);
                lostBars = Math.min(lostBars, barLength - currentHealthBars - absorptionHearts);
                int emptyBars = Math.max(0, barLength - currentHealthBars - absorptionHearts - lostBars);

                StringBuilder bar = new StringBuilder();
                for (int i = 0; i < currentHealthBars; i++) bar.append("&c█");
                if (isBlinking) {
                    for (int i = 0; i < lostBars; i++) bar.append("&4█");
                } else {
                    for (int i = 0; i < lostBars; i++) bar.append("&8█");
                }
                for (int i = 0; i < absorptionHearts; i++) bar.append("&f█");
                for (int i = 0; i < emptyBars; i++) bar.append("&8█");

                yield bar.toString();
            }
            default -> getRegionIcon(params);
        };
    }

    private java.util.List<String> collectLeagueItemIcons(Player player) {
        java.util.List<String> icons = new java.util.ArrayList<>();
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (icons.size() >= 6) break;
            if (item == null || item.getType().isAir()) continue;
            String itemId = ItemModifier.getItemId(item);
            if (itemId == null) continue;
            icons.add(Icons.get("items." + itemId.replace(".", "-")));
        }
        return icons;
    }

    private String getLeagueItemIcon(Player player, int slot) {
        java.util.List<String> icons = collectLeagueItemIcons(player);
        return slot <= icons.size() ? icons.get(slot - 1) : "";
    }


    private String getRegionIcon(String params) {
        String name = params.toLowerCase();
        String icon = Icons.get("regions." + name.replace("-", "_"));
        if (icon.isEmpty()) {
            icon = Icons.get("items." + name.replace(".", "-").replace("_", "-"));
        }
        return icon.isEmpty() ? null : icon;
    }

    private String getPrimaryPath(Player player) {
        if (player == null) return "§☯ No Primary Rune";
        LeagueMechanics lm = LeagueMechanics.getInstance();
        if (lm == null || lm.getRuneManager() == null) return "§☯ No Primary Rune";
        var runeData = lm.getRuneManager().getPlayerRuneData(player);
        RunePath path = runeData != null ? runeData.getPrimaryPath() : null;
        if (path == null) return "§7☯ No Primary Rune";
        return display(PATH_DISPLAY, "paths", path.getId(), "§☯ No Primary Rune");
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
        return display(PATH_DISPLAY_MINIMESSAGE, "paths", path.getId(), "☯ No Primary Rune");
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
        return display(PATH_DISPLAY_MINIMESSAGE, "paths", pathId, "None");
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
        String icon = icon("keystones." + keystoneId);

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
            case ROW1_ADAP -> icon("stats.ad") + " <white>+15 / " + icon("stats.ap") + " <white>+19 daptive Force";
            case ROW1_AS -> icon("stats.as") + " <white>+10 Attack Speed";
            case ROW1_CH -> icon("stats.ch") + " <white>+18 Cooldown Haste";
            default -> "None";
        };
    }
    private String getShard2Stats(Player player, PlayerStats stats) {
        RuneShard shard = stats.getRuneShards(player).getRow2();
        if (shard == null) {
            return "None";
        }
        return switch (shard) {
            case ROW2_ADAP -> icon("stats.ad") + " <white>+15 / " + icon("stats.ap") + " <white>+19 Adaptive Force";
            case ROW2_MS -> icon("stats.ms") + " <white>+15 Movement Speed";
            case ROW2_HR -> icon("stats.hr") + " <white>+2 Health Regen";
            default -> "None";
        };
    }
    private String getShard3Stats(Player player, PlayerStats stats) {
        RuneShard shard = stats.getRuneShards(player).getRow3();
        if (shard == null) {
            return "None";
        }
        return switch (shard) {
            case ROW3_HP -> icon("stats.hp") + " <white>+20 Health";
            case ROW3_TN -> icon("stats.tn") + " <white>+30 Tenacity";
            case ROW3_HR -> icon("stats.hr") + " <white>+2 Health Regen";
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
                case ROW1_ADAP -> icon("stats.ad") + " +15 / " + icon("stats.ap") + " <white>+19 daptive Force";
                case ROW1_AS -> icon("stats.as") + " <white>+10 Attack Speed";
                case ROW1_CH -> icon("stats.ch") + " <white>+18 Cooldown Haste";
                default -> "None";
            };
        } else if (row == 2) {
            return switch (shard) {
                case ROW2_ADAP -> icon("stats.ad") + " <white>+15 / " + icon("stats.ap") + " <white>+19 Adaptive Force";
                case ROW2_MS -> icon("stats.ms") + " <white>+15 Movement Speed";
                case ROW2_HR -> icon("stats.hr") + " <white>+2 Health Regen";
                default -> "None";
            };
        } else {
            return switch (shard) {
                case ROW3_HP -> icon("stats.hp") + " <white>+20 Health";
                case ROW3_TN -> icon("stats.tn") + " <white>+30 Tenacity";
                case ROW3_HR -> icon("stats.hr") + " <white>+2 Health Regen";
                default -> "None";
            };
        }
    }


    private String getClass(Player player) {
        if (player == null) return "§7⚔ No Class Selected";
        PlayerClassType ct = PlayerClass.getPlayerClass(player);
        if (ct == null) return "§7⚔ No Class Selected";
        return display(CLASS_DISPLAY, "classes", ct.getId(), "§7⚔ No Class Selected");
    }
    private String getClassMM(Player player) {
        if (player == null) return "⚔ No Class Selected";
        PlayerClassType ct = PlayerClass.getPlayerClass(player);
        if (ct == null) return "⚔ No Class Selected";
        return display(CLASS_DISPLAY_MINIMESSAGE, "classes", ct.getId(), "⚔ No Class Selected");
    }


    private static String icon(String key) {
        String glyph = Icons.get(key);
        if (glyph.isEmpty()) return "";
        return "<white>" + glyph + "</white>";
    }

    private String formatRuneId(String id) {
        if (id == null || id.equals("None")) return "None";
        return Arrays.stream(id.split("-"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static final java.util.Map<String, String> KEYSTONE_COLORS = java.util.Map.of(
            "domination", "<red>",
            "precision", "<yellow>",
            "resolve", "<green>",
            "inspiration", "<aqua>",
            "sorcery", "<blue>"
    );

    private static final java.util.Map<String, String> PATH_DISPLAY = java.util.Map.of(
            "domination",   "§f{icon} §4Domination",
            "precision",    "§f{icon} §6Precision",
            "resolve",     "§f{icon} §2Resolve",
            "sorcery",     "§f{icon} §1Sorcery",
            "inspiration", "§f{icon} §3Inspiration"
    );

    private static final java.util.Map<String, String> CLASS_DISPLAY = java.util.Map.of(
            "fighter",   "§f{icon} §7Fighter",
            "mage",     "§f{icon} §7Mage",
            "tank",     "§f{icon} §7Tank",
            "marksman", "§f{icon} §7Marksman",
            "assassin", "§f{icon} §7Assasin",
            "support",  "§f{icon} §7Support"
    );

    private static final java.util.Map<String, String> PATH_DISPLAY_MINIMESSAGE = java.util.Map.of(
            "domination",   "{font}<dark_red> Domination</dark_red>",
            "precision",    "{font}<gold> Precision</gold>",
            "resolve",     "{font}<dark_green> Resolve</dark_green>",
            "sorcery",     "{font}<dark_blue> Sorcery</dark_blue>",
            "inspiration", "{font}<dark_aqua> Inspiration</dark_aqua>"
    );

    private static final java.util.Map<String, String> CLASS_DISPLAY_MINIMESSAGE = java.util.Map.of(
            "fighter",   "{font}<gray> Fighter</gray>",
            "mage",     "{font}<gray> Mage</gray>",
            "tank",     "{font}<gray> Tank</gray>",
            "marksman", "{icon}<gray> Marksman</gray>",
            "assassin", "{font}<gray> Assasin</gray>",
            "support",  "{font}<gray> Support</gray>"
    );

    private static String display(java.util.Map<String, String> map, String group, String id, String fallback) {
        String template = map.get(id);
        if (template == null) return fallback;
        String key = group + "." + id;
        return template.replace("{icon}", Icons.get(key)).replace("{font}", icon(key));
    }

    private String getLine1(Player player, PlayerStats stats) {
        double ad = stats.getPlayerAD(player);
        double ap = stats.getPlayerAP(player);
        return "§f" + Icons.get("stats.ad") + " §7" + String.format("%-4.0f", ad) + "  §f" + Icons.get("stats.ap") + " §7" + String.format("%-4.0f", ap);
    }

    private String getLine2(Player player, PlayerStats stats) {
        double ar = stats.getPlayerAR(player);
        double mr = stats.getPlayerMR(player);
        return "§f" + Icons.get("stats.ar") + " §7" + String.format("%-4.0f", ar) + "  §f" + Icons.get("stats.mr") + " §7" + String.format("%-4.0f", mr);
    }

    private String getLine3(Player player, PlayerStats stats) {
        double as = stats.getPlayerAS(player);
        double ch = stats.getPlayerCH(player);
        double roundedAS = Math.ceil(as * 100) / 100;
        String asFormatted = String.format("%.2f", roundedAS);
        return "§f" + Icons.get("stats.as") + " §7" + String.format("%-6s", asFormatted) + "§f" + Icons.get("stats.ch") + " §7" + String.format("%-4.0f", ch);
    }

    private String getLine4(Player player, PlayerStats stats) {
        double cc = stats.getPlayerCC(player);
        double ms = stats.getPlayerMS(player);
        String ccFormatted = String.format("%.0f", cc) + "%";
        return "§0.§f" + Icons.get("stats.cc") + " §7" + String.format("%-4s", ccFormatted) + "  §f" + Icons.get("stats.ms") + " §7" + String.format("%-4.0f", ms);
    }
}