package dev.ixpu.leaguemechanics.command;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.rune.shards.ShardStats;
import dev.ixpu.leaguemechanics.util.RunePersistence;
import dev.ixpu.leaguemechanics.player.PlayerKDA;

import dev.ixpu.leaguemechanics.gui.InspectGUI;
import dev.ixpu.leaguemechanics.gui.ItemShopGUI;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneRegistry;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import dev.ixpu.leaguemechanics.rune.RuneShard;

import dev.ixpu.leaguemechanics.manager.ItemStatsManager;
import dev.ixpu.leaguemechanics.manager.RuneManager;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;
import dev.ixpu.leaguemechanics.player.PlayerStats;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandHandler implements CommandExecutor {
    private final LeagueMechanics plugin;
    private final RuneManager runeManager;
    private final RunePersistence runePersistence;
    private final PlayerEventListener playerEventListener;
    private final Set<UUID> pvpEnabledPlayers = new HashSet<>();

    public CommandHandler(LeagueMechanics plugin, ItemStatsManager itemStatsManager, RuneManager runeManager, RunePersistence runePersistence, PlayerEventListener playerEventListener) {
        this.plugin = plugin;
        this.runeManager = runeManager;
        this.runePersistence = runePersistence;
        this.playerEventListener = playerEventListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("§cUsage: /lm <shop|class|runes|reload|inspect|pvp>"));
            return true;
        }

        String subcommand = args[0].toLowerCase();

        return switch (subcommand) {
            case "shop" -> handleShop(player);
            case "inspect" -> handleInspect(player, args);
            case "reload" -> {
                if (!player.hasPermission("leaguemechanics.admin")) {
                    player.sendMessage(Component.text("§cYou don't have permission to use this command."));
                    yield true;
                }
                player.sendMessage(Component.text("§6⟳ Reloading LeagueMechanics..."));
                plugin.reloadPlugin();
                player.sendMessage(Component.text("§a✓ LeagueMechanics reloaded"));
                yield true;
            }
            case "runes" -> handleRunesCommand(player, args);
            case "class" -> handleClassCommand(player, args);
            case "pvp" -> handlePvpCommand(player, args);
            case "clearkda" -> handleClearKda(player);
            default -> {
                player.sendMessage(Component.text("§cUnknown subcommand."));
                yield false;
            }
        };
    }

    private boolean handleClassCommand(Player player, String[] args) {
        if (!player.hasPermission("leaguemechanics.user")) {
            player.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("clear")) {
            player.sendMessage(Component.text("§cUsage: §e/lm class clear"));
            return true;
        }

        dev.ixpu.leaguemechanics.player.PlayerClass.clearPlayerClass(player);
        player.sendMessage(Component.text("§a✓ Class cleared!"));
        return true;
    }

    private boolean handlePvpCommand(Player player, String[] args) {
        if (!player.hasPermission("leaguemechanics.user")) {
            player.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("§cUsage: /lm pvp <on|off>"));
            return true;
        }

        String mode = args[1].toLowerCase();
        UUID uuid = player.getUniqueId();

        if (mode.equals("on")) {
            pvpEnabledPlayers.add(uuid);
            player.sendMessage(Component.text("§a✓ PVP mode enabled!"));
        } else if (mode.equals("off")) {
            pvpEnabledPlayers.remove(uuid);
            player.sendMessage(Component.text("§a✓ PVP mode disabled!"));
        } else {
            player.sendMessage(Component.text("§cUsage: /lm pvp <on|off>"));
            return true;
        }

        return true;
    }

    private boolean handleClearKda(Player player) {
        if (!player.hasPermission("leaguemechanics.admin")) {
            player.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        // Clear all KDA data
        PlayerKDA.getInstance().clearAllKda();

        // Reload KDA data from database to clear the cache
        PlayerKDA.getInstance().loadAllFromDatabase();

        player.sendMessage(Component.text("§a✓ All player KDA data has been cleared!"));
        return true;
    }

    public boolean isPvpEnabled(Player attacker, Player target) {
        return pvpEnabledPlayers.contains(attacker.getUniqueId()) &&
               pvpEnabledPlayers.contains(target.getUniqueId());
    }

    private boolean handleRunesCommand(Player player, String[] args) {
        if (!player.hasPermission("leaguemechanics.user")) {
            player.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sendRunesUsage(player);
            return true;
        }

        String runesSubcommand = args[1].toLowerCase();
        return switch (runesSubcommand) {
            case "select"   -> handleRuneSelect(player, args);
            case "clear"    -> handleRunesClear(player, args);
            case "info"     -> handleRunesInfo(player);
            default -> {
                sendRunesUsage(player);
                yield true;
            }
        };
    }

    private void sendRunesUsage(Player player) {
        player.sendMessage(Component.text("§6§lRunes Commands:"));
        player.sendMessage(Component.text("§7  /lm runes select primary §e<path> [keystone] [slot1] [slot2] [slot3]"));
        //player.sendMessage(Component.text("§7  /lm runes select secondary §e<path> [slot1] [slot2]"));
        player.sendMessage(Component.text("§7  /lm runes select shards §e<row1-option> <row2-option> <row3-option>"));
        player.sendMessage(Component.text("§7  /lm runes clear §8— §fclear all runes"));
        player.sendMessage(Component.text("§7  /lm runes info §8— §fshow currently equipped runes"));
    }

    private boolean handleRuneSelect(Player player, String[] args) {
        if (args.length < 3) {
            sendRunesUsage(player);
            return true;
        }

        String location = args[2].toLowerCase();
        if (location.equals("primary")) {
            return handleRuneSelectPrimary(player, args);
        } else if (location.equals("secondary")) {
            return handleRuneSelectSecondary(player, args);
        } else if (location.equals("shards")) {
            return handleRuneSelectShards(player, args);
        } else {
            sendRunesUsage(player);
            return true;
        }
    }

    private boolean handleRuneSelectPrimary(Player player, String[] args) {
        if (args.length < 4 || args.length > 8) {
            player.sendMessage(Component.text("§cUsage: /lm runes select primary <path> [keystone] [slot1] [slot2] [slot3]"));
            return true;
        }

        RunePath path = RunePath.fromId(args[3].toLowerCase());
        if (path == null) {
            player.sendMessage(Component.text("§cInvalid path. Use: domination, precision, inspiration, resolve, or sorcery"));
            return true;
        }

        runeManager.setPlayerPrimaryPath(player, path);
        runePersistence.savePrimaryPath(player.getUniqueId(), path);

        RuneSlot[] slotOrder = {
                RuneSlot.KEYSTONE,
                RuneSlot.PRIMARY_SLOT_1,
                RuneSlot.PRIMARY_SLOT_2,
                RuneSlot.PRIMARY_SLOT_3
        };
        StringBuilder summary = new StringBuilder("§a✓ Primary path set to §e" + path.getId());

        for (int i = 4; i < args.length; i++) {
            RuneSlot slot = slotOrder[i - 4];
            CooldownHandler rune = resolveAndValidateRune(player, args[i], path, slot);
            if (rune == null) return true;

            applyPrimaryRune(player, slot, rune);
            runePersistence.savePrimaryRuneSlot(player.getUniqueId(), slot, rune.getId());

            summary.append(" §7| ").append(slot.getId()).append(": §e").append(rune.getId());
        }
        player.sendMessage(Component.text(summary.toString()));
        return true;
    }

    private boolean handleRuneSelectSecondary(Player player, String[] args) {
//        if (args.length < 4 || args.length > 6) {
//            player.sendMessage(Component.text("§cUsage: /lm runes select secondary <path> [slot1] [slot2]"));
//            return true;
//        }
//
//        RunePath path = RunePath.fromId(args[3].toLowerCase());
//        if (path == null) {
//            player.sendMessage(Component.text("§cInvalid path. Use: domination, precision, inspiration, resolve, or sorcery"));
//            return true;
//        }
//
//        runeManager.setPlayerSecondaryPath(player, path);
//        runePersistence.saveSecondaryPath(player.getUniqueId(), path);
//
//        RuneSlot[] slotOrder = {RuneSlot.SECONDARY_SLOT_1, RuneSlot.SECONDARY_SLOT_2};
//        StringBuilder summary = new StringBuilder("§a✓ Secondary path set to §e" + path.getId());
//
//        for (int i = 4; i < args.length; i++) {
//            RuneSlot slot = slotOrder[i - 4];
//            CooldownHandler rune = resolveAndValidateRune(player, args[i], path, slot);
//            if (rune == null) return true;
//
//            applySecondaryRune(player, slot, rune);
//            runePersistence.saveSecondaryRuneSlot(player.getUniqueId(), slot, rune.getId());
//
//            summary.append(" §7| ").append(slot.getId()).append(": §e").append(rune.getId());
//        }
//        player.sendMessage(Component.text(summary.toString()));
        player.sendMessage(Component.text("Secondary and Primary rune slots under development"));
        return true;
    }

    private void applyPrimaryRune(Player player, RuneSlot slot, CooldownHandler rune) {
        switch (slot) {
            case KEYSTONE -> runeManager.setPlayerKeystoneRune(player, rune);
            case PRIMARY_SLOT_1 -> runeManager.setPlayerPrimarySlot1Rune(player, rune);
            case PRIMARY_SLOT_2 -> runeManager.setPlayerPrimarySlot2Rune(player, rune);
            case PRIMARY_SLOT_3 -> runeManager.setPlayerPrimarySlot3Rune(player, rune);
            default -> throw new IllegalArgumentException("Not a primary slot: " + slot);
        }
    }

    private void applySecondaryRune(Player player, RuneSlot slot, CooldownHandler rune) {
        switch (slot) {
            case SECONDARY_SLOT_1 -> runeManager.setPlayerSecondarySlot1Rune(player, rune);
            case SECONDARY_SLOT_2 -> runeManager.setPlayerSecondarySlot2Rune(player, rune);
            default -> throw new IllegalArgumentException("Not a secondary slot: " + slot);
        }
    }

    private void applyShardRune(Player player, RuneSlot slot, CooldownHandler rune) {
        switch (slot) {
            case SHARD_SLOT_1 -> runeManager.setPlayerShardSlot1Rune(player, rune);
            case SHARD_SLOT_2 -> runeManager.setPlayerShardSlot2Rune(player, rune);
            case SHARD_SLOT_3 -> runeManager.setPlayerShardSlot3Rune(player, rune);
            default -> throw new IllegalArgumentException("Not a shard slot: " + slot);
        }
    }

    private boolean handleRuneSelectShards(Player player, String[] args) {
        if (args.length != 6) {
            player.sendMessage(Component.text("§cUsage: /lm runes select shards <row1-option> <row2-option> <row3-option>"));
            return true;
        }

        String row1Option = args[3];
        String row2Option = args[4];
        String row3Option = args[5];

        RuneShard row1 = RuneShard.fromRowAndOption(1, row1Option);
        RuneShard row2 = RuneShard.fromRowAndOption(2, row2Option);
        RuneShard row3 = RuneShard.fromRowAndOption(3, row3Option);

        if (row1 == null || row2 == null || row3 == null) {
            player.sendMessage(Component.text("§cInvalid shard selection. Use option-1, option-2, or option-3"));
            return true;
        }

        ShardStats shards = PlayerStats.getOrCreate(player).getRuneShards(player);
        shards.selectShards(row1, row2, row3);

        runePersistence.saveRuneShards(player.getUniqueId(), row1.name(), row2.name(), row3.name());

        player.sendMessage(Component.text("§aRune Shards selected:"));
        player.sendMessage(Component.text("§7Row 1: §b" + row1.getDisplay()));
        player.sendMessage(Component.text("§7Row 2: §b" + row2.getDisplay()));
        player.sendMessage(Component.text("§7Row 3: §b" + row3.getDisplay()));

        playerEventListener.applyPlayerStats(player);
        return true;
    }

    private CooldownHandler resolveAndValidateRune(Player player, String runeId, RunePath path, RuneSlot slot) {
        String normalized = runeId.toLowerCase();
        CooldownHandler rune = RuneRegistry.getInstance().getRune(normalized);
        if (rune == null) {
            player.sendMessage(Component.text("§cRune not found: §e" + normalized));
            return null;
        }
        if (!rune.getPath().equals(path)) {
            player.sendMessage(Component.text("§c" + normalized + " is not in the " + path.getId() + " path."));
            return null;
        }
        if (!rune.getSlot().equals(slot)) {
            player.sendMessage(Component.text("§c" + normalized + " is not a " + slot.getId() + " slot rune"));
            return null;
        }
        String pathPermission = "primary-rune-path." + path.getId();
        if (!player.hasPermission(pathPermission)) {
            player.sendMessage(Component.text("§cYou don't have permission to use the " + path.getId() + " path."));
            return null;
        }
        String permissionKey = slot == RuneSlot.KEYSTONE
                ? "rune-keystone." + normalized
                : "rune." + normalized;
        if (!player.hasPermission(permissionKey)) {
            player.sendMessage(Component.text("§cYou don't have permission to select " + normalized + "."));
            return null;
        }
        return rune;
    }

    private boolean handleRunesClear(Player player, String[] args) {
        runePersistence.clearAllRunes(player.getUniqueId());
        runeManager.clearPlayerRunes(player);
        player.sendMessage(Component.text("§a✓ All runes cleared"));
        return true;
    }

    private boolean handleRunesInfo(Player player) {
        dev.ixpu.leaguemechanics.player.PlayerRuneData data = runeManager.getPlayerRuneData(player);
        if (data == null) {
            player.sendMessage(Component.text("§cNo runes loaded. Try rejoining."));
            return true;
        }

        String primary = data.getPrimaryPath() != null ? data.getPrimaryPath().getId() : "§7none";
        String secondary = data.getSecondaryPath() != null ? data.getSecondaryPath().getId() : "§7none";
        String keystone = data.getKeystoneRune() != null ? data.getKeystoneRune().getId() : "§7none";
        String p1 = data.getPrimarySlot1Rune() != null ? data.getPrimarySlot1Rune().getId() : "§7none";
        String p2 = data.getPrimarySlot2Rune() != null ? data.getPrimarySlot2Rune().getId() : "§7none";
        String p3 = data.getPrimarySlot3Rune() != null ? data.getPrimarySlot3Rune().getId() : "§7none";
        String s1 = data.getSecondarySlot1Rune() != null ? data.getSecondarySlot1Rune().getId() : "§7none";
        String s2 = data.getSecondarySlot2Rune() != null ? data.getSecondarySlot2Rune().getId() : "§7none";

        ShardStats shardStats = PlayerStats.getOrCreate(player).getRuneShards(player);
        RuneShard selectedShard1 = shardStats.getRow1();
        RuneShard selectedShard2 = shardStats.getRow2();
        RuneShard selectedShard3 = shardStats.getRow3();

        String shard1Display = selectedShard1 != null ? selectedShard1.getDisplay() : "§7none";
        String shard2Display = selectedShard2 != null ? selectedShard2.getDisplay() : "§7none";
        String shard3Display = selectedShard3 != null ? selectedShard3.getDisplay() : "§7none";

        player.sendMessage(Component.text("§6ᴍʏ ᴀᴄᴛɪᴠᴇ ʀᴜɴᴇꜱ:"));
        player.sendMessage(Component.text("§7  Primary:   §e" + primary + " §7— keystone: §e" + keystone));
        player.sendMessage(Component.text("§7   slot 1: §e" + p1 + " §7| slot 2: §e" + p2 + " §7| slot 3: §e" + p3));
        player.sendMessage(Component.text("§7  Secondary: §e" + secondary));
        player.sendMessage(Component.text("§7   slot 1: §e" + s1 + " §7| slot 2: §e" + s2));
        player.sendMessage(Component.text("§7    Shards: §e" + shard1Display + " §7| §e" + shard2Display + " §7| §e" + shard3Display));
        return true;
    }

    
    private boolean handleShop(Player player) {
        if (!player.hasPermission("leaguemechanics.user")) {
            player.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }
        ItemShopGUI.openShop(player);
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean handleInspect(Player player, String[] args) {
        if (!player.hasPermission("leaguemechanics.user")) {
            player.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("§cUsage: /lm inspect <player>"));
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(Component.text("§cPlayer not found: §e" + targetName));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("§cYou cannot inspect yourself! Use /lm inspect <other-player>"));
            return true;
        }

        InspectGUI.openInspect(player, target);
        player.sendMessage(Component.text("§6⟳ Inspecting §e" + target.getName()));

        return true;
    }
}