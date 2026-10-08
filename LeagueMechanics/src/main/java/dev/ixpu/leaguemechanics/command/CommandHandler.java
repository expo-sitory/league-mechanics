package dev.ixpu.leaguemechanics.command;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClass;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.buff.BuffType;
import dev.ixpu.leaguemechanics.manager.BuffManager;
import dev.ixpu.leaguemechanics.manager.ItemShopManager;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.manager.ClassAbilitiesManager;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilitiesRegistry;
import dev.ixpu.leaguemechanics.rune.shards.ShardStats;
import dev.ixpu.leaguemechanics.util.RunePersistence;
import dev.ixpu.leaguemechanics.entity.player.PlayerKDA;

import dev.ixpu.leaguemechanics.gui.InspectGUI;
import dev.ixpu.leaguemechanics.gui.ItemShopGUI;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.paths.RunePath;
import dev.ixpu.leaguemechanics.util.RuneRegistry;
import dev.ixpu.leaguemechanics.rune.slots.RuneSlot;
import dev.ixpu.leaguemechanics.rune.shards.RuneShard;

import dev.ixpu.leaguemechanics.manager.ItemStatsManager;
import dev.ixpu.leaguemechanics.manager.RuneManager;

import dev.ixpu.leaguemechanics.listener.PlayerEventListener;
import dev.ixpu.leaguemechanics.listener.PlayerSkillListener;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.item.shop.ItemShopRegistry;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
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

    private boolean isCommandDisabledInWorld(Player player, String subcommand) {
        java.util.List<String> disabledCommands = plugin.getConfig().getStringList("general-settings.disabled-commands-by-world." + player.getWorld().getName());
        return disabledCommands != null && disabledCommands.contains(subcommand);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            return true;
        }

        String subcommand = args[0].toLowerCase();

        if (sender instanceof Player player && isCommandDisabledInWorld(player, subcommand)) {
            sender.sendMessage(Component.text("§cThis command is disabled in this world."));
            return true;
        }

        return switch (subcommand) {
            case "shop" -> handleShop(sender, args);
            case "inspect" -> handleInspect(sender, args);
            case "reload" -> {
                if (!sender.hasPermission("leaguemechanics.admin")) {
                    sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
                    yield true;
                }
                sender.sendMessage(Component.text("§6⟳ Reloading LeagueMechanics..."));
                plugin.reloadPlugin();
                sender.sendMessage(Component.text("§aLeagueMechanics reloaded"));
                yield true;
            }
            case "runes" -> handleRunesCommand(sender, args);
            case "class" -> handleClassCommand(sender, args);
            case "pvp" -> handlePvpCommand(sender, args);
            case "clearkda" -> handleClearKda(sender);
            case "levelup" -> handleLevelUpCommand(sender, args);
            case "setlevel" -> handleSetLevelCommand(sender, args);
            case "givebuff" -> handleGiveBuffCommand(sender, args);
            case "forcechannel" -> handleForceChannelCommand(sender, args);
            case "forcestopchannel" -> handleForceStopChannelCommand(sender, args);
            case "party" -> handlePartyCommand(sender, args);
            case "dash" -> handleDashCommand(sender, args);
            case "ability" -> handleSkillsCommand(sender, args);
            default -> {
                sender.sendMessage(Component.text("§cUnknown subcommand."));
                yield false;
            }
        };
    }

    private boolean handleSkillsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cOnly players can use this command."));
            return true;
        }
        if (!sender.hasPermission("leaguemechanics.user")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 3 || !args[1].equalsIgnoreCase("select")) {
            sender.sendMessage(Component.text("§cUsage: /lm ability select <ability>"));
            return true;
        }

        ClassAbilities skill = ClassAbilitiesRegistry.getInstance().get(args[2]);
        if (skill == null) {
            sender.sendMessage(Component.text("§cUnknown Ability: §e" + args[2]));
            return true;
        }

        switch (ClassAbilitiesManager.getInstance().select(player, skill)) {
            case OK -> player.sendMessage(Component.text("§aAbility set to §e" + skill.getDisplayName()));
            case NO_CLASS -> player.sendMessage(Component.text("§cSelect a class first: /lm class select <class>"));
            case WRONG_CLASS -> player.sendMessage(Component.text("§c" + skill.getDisplayName() + " is a "
                    + skill.getClassType().getDisplayName() + " ability and doesn't belong to your class."));
            case INVENTORY_FULL -> player.sendMessage(Component.text("§cYour inventory is full. Free a slot first."));
            case ALREADY_SELECTED -> player.sendMessage(Component.text("§7You already have §e" + skill.getDisplayName() + " §7selected."));
        }
        return true;
    }

    private boolean handleClassCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leaguemechanics.user") && !sender.hasPermission("player.no-class")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("§cUsage: /lm class select <classname> or /lm class clear [player]"));
            return true;
        }

        if (args[1].equalsIgnoreCase("select")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("§cOnly players can select a class!"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(Component.text("§cUsage: /lm class select <fighter|support|assassin|mage|tank|marksman>"));
                return true;
            }

            try {
                PlayerClassType classType = PlayerClassType.valueOf(args[2].toUpperCase());
                if (PartyManager.getInstance().isClassTakenInParty(player, classType)) {
                    player.sendMessage(Component.text("§cYour party already has §e" + PartyManager.getInstance().describeClassLimit(classType) + "§c. Leave the party first or pick another class."));
                    return true;
                }
                PlayerClass.setPlayerClass(player, classType);
                player.sendMessage(Component.text("§aClass set to §e" + classType.name().toLowerCase()));
                return true;
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Component.text("§cInvalid class. Use: fighter, support, assassin, mage, tank, or marksman"));
                return true;
            }
        } else if (args[1].equalsIgnoreCase("clear")) {
            if (args.length >= 3) {
                String targetName = args[2];
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    sender.sendMessage(Component.text("§cPlayer not found: §e" + targetName));
                    return true;
                }
                PlayerClass.clearPlayerClass(target);
                sender.sendMessage(Component.text("§aCleared class for player §e" + target.getName()));
                return true;
            } else {
                if (sender instanceof Player player) {
                    PlayerClass.clearPlayerClass(player);
                    sender.sendMessage(Component.text("§aYour class has been cleared!"));
                    return true;
                } else {
                    sender.sendMessage(Component.text("§cUsage: /lm class clear <player>"));
                    return true;
                }
            }
        } else {
            sender.sendMessage(Component.text("§cUsage: /lm class select <classname> or /lm class clear [player]"));
            return true;
        }
    }

    private boolean handleShop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leaguemechanics.user")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cOnly players can use the shop command!"));
            return true;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("buy")) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("§cUsage: /lm shop buy <item-id>"));
                return true;
            }

            String itemId = args[2].toLowerCase();
            ItemShopRegistry registry = ItemShopRegistry.getInstance();
            ItemShopRegistry.ShopItem shopItem = registry.getShopItem(itemId);

            if (shopItem == null) {
                sender.sendMessage(Component.text("§cItem not found: §e" + itemId));
                return true;
            }

            ItemShopManager.getInstance().purchaseFromGUI(player, shopItem);
            return true;
        }

        ItemShopGUI.openShop(player);
        return true;
    }

    private boolean handleDashCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cOnly players can use this command."));
            return true;
        }
        if (!sender.hasPermission("leaguemechanics.user")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        boolean enable;
        if (args.length < 2) {
            enable = !PlayerSkillListener.isDashEnabled(player);
        } else if (args[1].equalsIgnoreCase("on")) {
            enable = true;
        } else if (args[1].equalsIgnoreCase("off")) {
            enable = false;
        } else {
            sender.sendMessage(Component.text("§cUsage: /lm dash [on|off]"));
            return true;
        }

        PlayerSkillListener.setDashEnabled(player, enable);
        sender.sendMessage(Component.text(enable ? "§aDash enabled" : "§c✗ Dash disabled"));
        return true;
    }

    private boolean handlePvpCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leaguemechanics.user")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("§cUsage: /lm pvp <on|off>"));
            return true;
        }

        String mode = args[1].toLowerCase();
        Player targetPlayer = null;
        boolean isSelfTarget = false;
        boolean isConsoleSender = !(sender instanceof Player);

        if (args.length >= 3) {
            String targetName = args[2];
            targetPlayer = Bukkit.getPlayerExact(targetName);
            if (targetPlayer == null) {
                sender.sendMessage(Component.text("§cPlayer not found: §e" + targetName));
                return true;
            }

            if (!sender.hasPermission("leaguemechanics.admin")) {
                sender.sendMessage(Component.text("§cYou don't have permission to modify other players' PVP state!"));
                return true;
            }

            isSelfTarget = false;
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("§cYou must specify a player when using from console!"));
                return true;
            }
            targetPlayer = player;
            isSelfTarget = true;
        }

        UUID uuid = targetPlayer.getUniqueId();
        boolean wasEnabledBefore = pvpEnabledPlayers.contains(uuid);
        boolean willBeEnabled = mode.equals("on");

        if (mode.equals("on")) {
            pvpEnabledPlayers.add(uuid);
        } else if (mode.equals("off")) {
            pvpEnabledPlayers.remove(uuid);
        } else {
            sender.sendMessage(Component.text("§cUsage: /lm pvp <on|off>"));
            return true;
        }

        if (isSelfTarget) {
            if (willBeEnabled) {
                sender.sendMessage(Component.text("§c⚠ ᴘᴠᴘ ᴍᴏᴅᴇ ᴇɴᴀʙʟᴇᴅ ⚠"));
            } else {
                sender.sendMessage(Component.text("§a⚠ ᴘᴠᴘ ᴍᴏᴅᴇ ᴅɪꜱᴀʙʟᴇᴅ ⚠"));
            }
        } else {
            if (sender instanceof Player) {
                if (willBeEnabled) {
                    sender.sendMessage(Component.text("§aYou have enabled PVP mode for §e" + targetPlayer.getName()));
                } else {
                    sender.sendMessage(Component.text("§aYou have disabled PVP mode for §e" + targetPlayer.getName()));
                }

                if (willBeEnabled) {
                    targetPlayer.sendMessage(Component.text("§6⚠ ᴘᴠᴘ ᴍᴏᴅᴇ ᴇɴᴀʙʟᴇᴅ ⚠"));
                } else {
                    targetPlayer.sendMessage(Component.text("§a⚠ ᴘᴠᴘ ᴍᴏᴅᴇ ᴅɪꜱᴀʙʟᴇᴅ ⚠"));
                }
            }
        }

        return true;
    }

    private boolean handleClearKda(CommandSender sender) {
        if (!sender.hasPermission("leaguemechanics.admin")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        PlayerKDA.getInstance().clearAllKda();

        PlayerKDA.getInstance().loadAllFromDatabase();

        sender.sendMessage(Component.text("§aAll player KDA data has been cleared!"));
        return true;
    }

    private boolean handleSetLevelCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leaguemechanics.admin")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("§cUsage: /lm setlevel <level> [player]"));
            return true;
        }

        int minLevel = 1;
        int maxLevel = 18;
        int newLevel;
        try {
            newLevel = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("§cInvalid level: §e" + args[1]));
            return true;
        }

        if (newLevel < minLevel || newLevel > maxLevel) {
            sender.sendMessage(Component.text("§cLevel must be between §e" + minLevel + " §cand §e" + maxLevel + "§c."));
            return true;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("§cPlayer not found: §e" + args[2]));
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage(Component.text("§cUsage: /lm setlevel <level> <player>"));
            return true;
        }

        PlayerStats stats = PlayerStats.getOrCreate(target);
        stats.setLeagueLevel(newLevel);
        stats.saveLeagueLevel(target);
        playerEventListener.applyPlayerStats(target);

        sender.sendMessage(Component.text("§aSet level of §e" + target.getName() + " §ato §e" + newLevel));
        if (target != sender) {
            target.sendMessage(Component.text("§aYour level was set to §e" + newLevel));
        }
        return true;
    }

    private boolean handleForceStopChannelCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leaguemechanics.admin")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("§cUsage: /force-stop-channel <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("§cPlayer not found: §e" + args[1]));
            return true;
        }

        ClassAbilitiesRegistry.getInstance().stopChanneling(target);
        sender.sendMessage(Component.text("§e" + target.getName() + " §7is no longer channeling."));
        return true;
    }

    private boolean handleForceChannelCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leaguemechanics.admin")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("§cUsage: /force-channel <player> <seconds>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("§cPlayer not found: §e" + args[1]));
            return true;
        }

        double seconds;
        try {
            seconds = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("§cInvalid seconds: §e" + args[2]));
            return true;
        }
        if (seconds < 0 || Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            sender.sendMessage(Component.text("§cInvalid seconds: §e" + args[2]));
            return true;
        }

        ClassAbilitiesRegistry.getInstance().forceChannel(target, seconds);
        if (seconds == 0) {
            sender.sendMessage(Component.text("§e" + target.getName() + " §7is no longer forced to channel."));
        } else {
            sender.sendMessage(Component.text("§e" + target.getName() + " §7is now channeling for §f" + seconds + "s§7."));
        }
        return true;
    }

    private boolean handleGiveBuffCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leaguemechanics.admin")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("§cUsage: /lm givebuff <buff> [player]"));
            return true;
        }

        BuffType type = BuffManager.fromId(args[1]);
        if (type == null) {
            sender.sendMessage(Component.text("§cUnknown buff: §e" + args[1]));
            return true;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("§cPlayer not found: §e" + args[2]));
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage(Component.text("§cUsage: /lm givebuff <buff> <player>"));
            return true;
        }

        BuffManager buffs = BuffManager.getInstance();
        buffs.giveBuff(target, type);
        String name = buffs.getDisplayName(type);
        int seconds = (int) Math.round(buffs.getDurationSeconds(type));

        sender.sendMessage(Component.text("§aGave §e" + name + " §ato §e" + target.getName() + " §7(" + seconds + "s)"));
        if (target != sender) {
            target.sendMessage(Component.text("§aYou gained §e" + name + " §7(" + seconds + "s)"));
        }
        return true;
    }

    private boolean handlePartyCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cOnly players can use party commands."));
            return true;
        }
        if (!sender.hasPermission("leaguemechanics.user")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        PartyManager parties = PartyManager.getInstance();
        String action = args.length >= 2 ? args[1].toLowerCase() : "";
        switch (action) {
            case "invite" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("§cUsage: /party invite <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("§cPlayer not found: §e" + args[2]));
                    return true;
                }
                parties.invite(player, target);
            }
            case "" -> {
                if (parties.isInParty(player) && !parties.isLeader(player)) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dwopen party-member-menu " + player.getName());
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dwopen party-leader-menu " + player.getName());
                }
            }
            case "accept" -> parties.accept(player);
            case "leave" -> parties.leave(player);
            case "disband" -> parties.disband(player);
            case "toggle" -> parties.toggleInvites(player);
            default -> sender.sendMessage(Component.text("§cUsage: /party"));
        }
        return true;
    }

    private boolean handleLevelUpCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cOnly players can level up!"));
            return true;
        }

        PlayerStats stats = PlayerStats.getOrCreate(player);
        int currentLevel = stats.getLeagueLevel();
        int maxLevel = 18;

        if (currentLevel >= maxLevel) {
            sender.sendMessage(Component.text("§cYou are already at the maximum level (§e" + maxLevel + "§c)!"));
            return true;
        }

        double requiredXP = 100 * Math.pow(1.1, currentLevel);
        int xp = player.getLevel();

        if (xp < requiredXP) {
            double neededXP = requiredXP - xp;
            sender.sendMessage(Component.text("§cYou need §e" + String.format("%.0f", neededXP) + " §cmore XP to reach level §e" + (currentLevel + 1) + "§c!"));
            sender.sendMessage(Component.text("§cCurrent XP: §e" + xp + " §c| Required XP: §e" + String.format("%.0f", requiredXP)));
            return true;
        }

        player.setLevel(xp - (int) requiredXP);
        stats.setLeagueLevel(currentLevel + 1);
        stats.saveLeagueLevel(player);

        sender.sendMessage(Component.text("§aLevel Up! §e[" + (currentLevel + 1) + "]"));

        playerEventListener.applyPlayerStats(player);

        return true;
    }

    public boolean isPvpEnabled(Player attacker, Player target) {
        return pvpEnabledPlayers.contains(attacker.getUniqueId()) &&
                pvpEnabledPlayers.contains(target.getUniqueId());
    }

    private boolean handleRunesCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leaguemechanics.user")) {
            sender.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cOnly players can use runes commands!"));
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
        player.sendMessage(Component.text("§6§lʀᴜɴᴇꜱ ᴄᴏᴍᴍᴀɴᴅꜱ:"));
        player.sendMessage(Component.text("§7  /lm runes select primary §e<path> [keystone] [slot1] [slot2] [slot3]"));
        //player.sendMessage(Component.text("§7  /lm runes select secondary §e<path> [slot1] [slot2]"));
        player.sendMessage(Component.text("§7  /lm runes select shards §e<row-1|row-2|row-3> <option-1|option-2|option-3>"));
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
        if (!player.hasPermission("player.runes-selector")) {
            player.sendMessage(Component.text("§cYou don't have permission to use this command."));
            return true;
        }
        if (args.length < 4 || args.length > 8) {
            player.sendMessage(Component.text("§cUsage: /lm runes select primary <path> [keystone] [slot1] [slot2] [slot3]"));
            return true;
        }

        RunePath path = RunePath.fromId(args[3].toLowerCase());
        if (path == null) {
            player.sendMessage(Component.text("§cInvalid path. Use: domination, precision, inspiration, resolve, or sorcery"));
            return true;
        }

        String pathPermission = "primary-rune-path." + path.getId();
        if (!player.hasPermission(pathPermission)) {
            player.sendMessage(Component.text("§cYou don't have permission to use the " + path.getId() + " path."));
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
        StringBuilder summary = new StringBuilder("§aPrimary path set to §e" + path.getId());

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

//        RunePath path = RunePath.fromId(args[3].toLowerCase());
//        if (path == null) {
//            player.sendMessage(Component.text("§cInvalid path. Use: domination, precision, inspiration, resolve, or sorcery"));
//            return true;
//        }

//        runeManager.setPlayerSecondaryPath(player, path);
//        runePersistence.saveSecondaryPath(player.getUniqueId(), path);

//        RuneSlot[] slotOrder = {RuneSlot.SECONDARY_SLOT_1, RuneSlot.SECONDARY_SLOT_2};
//        StringBuilder summary = new StringBuilder("§aSecondary path set to §e" + path.getId());

//        for (int i = 4; i < args.length; i++) {
//            RuneSlot slot = slotOrder[i - 4];
//            CooldownHandler rune = resolveAndValidateRune(player, args[i], path, slot);
//            if (rune == null) return true;

//            applySecondaryRune(player, slot, rune);
//            runePersistence.saveSecondaryRuneSlot(player.getUniqueId(), slot, rune.getId());

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

    private boolean handleRuneSelectShards(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(Component.text("§cUsage: /lm runes select shards <row> <option> or /lm runes select shards cycle <row>"));
            return true;
        }

        if (args[3].equalsIgnoreCase("cycle")) {
            if (args.length < 5) {
                player.sendMessage(Component.text("§cUsage: /lm runes select shards cycle <row>"));
                return true;
            }
            return handleCycleRuneShards(player, args[4]);
        }

        if (args.length != 5) {
            player.sendMessage(Component.text("§cUsage: /lm runes select shards <row> <option>"));
            return true;
        }

        int row;
        int option;
        try {
            row = Integer.parseInt(args[3]);
            option = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("§cRow and option must be numbers (1-3)"));
            return true;
        }

        if (row < 1 || row > 3 || option < 1 || option > 3) {
            player.sendMessage(Component.text("§cRow and option must be between 1 and 3"));
            return true;
        }

        String optionId = "option-" + option;
        RuneShard selectedShard = RuneShard.fromRowAndOption(row, optionId);
        if (selectedShard == null) {
            player.sendMessage(Component.text("§cInvalid combination: row " + row + ", option " + option));
            return true;
        }

        RuneShard defaultRow1 = Arrays.stream(RuneShard.values())
                .filter(s -> s.getRowNumber() == 1)
                .findFirst()
                .orElse(null);
        RuneShard defaultRow2 = Arrays.stream(RuneShard.values())
                .filter(s -> s.getRowNumber() == 2)
                .findFirst()
                .orElse(null);
        RuneShard defaultRow3 = Arrays.stream(RuneShard.values())
                .filter(s -> s.getRowNumber() == 3)
                .findFirst()
                .orElse(null);

        ShardStats shards = PlayerStats.getOrCreate(player).getRuneShards(player);

        RuneShard finalRow1 = (row == 1) ? selectedShard : shards.getRow1();
        RuneShard finalRow2 = (row == 2) ? selectedShard : shards.getRow2();
        RuneShard finalRow3 = (row == 3) ? selectedShard : shards.getRow3();

        if (finalRow1 == null) finalRow1 = RuneShard.ROW1_ADAP;
        if (finalRow2 == null) finalRow2 = RuneShard.ROW2_ADAP;
        if (finalRow3 == null) finalRow3 = RuneShard.ROW3_HP;


        shards.selectShards(finalRow1, finalRow2, finalRow3);

        runePersistence.saveRuneShards(player.getUniqueId(), finalRow1.name(), finalRow2.name(), finalRow3.name());

        playerEventListener.applyPlayerStats(player);
        return true;
    }

    private boolean handleCycleRuneShards(Player player, String rowStr) {
        int row;
        try {
            row = Integer.parseInt(rowStr);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("§cRow must be a number (1-3)"));
            return true;
        }

        if (row < 1 || row > 3) {
            player.sendMessage(Component.text("§cRow must be between 1 and 3"));
            return true;
        }

        ShardStats shards = PlayerStats.getOrCreate(player).getRuneShards(player);
        RuneShard current = row == 1 ? shards.getRow1() : (row == 2 ? shards.getRow2() : shards.getRow3());

        int nextOption = getNextShardOption(row, current);

        String optionId = "option-" + nextOption;
        RuneShard selectedShard = RuneShard.fromRowAndOption(row, optionId);

        if (selectedShard == null) {
            player.sendMessage(Component.text("§cCouldn't cycle shard"));
            return true;
        }

        RuneShard finalRow1 = row == 1 ? selectedShard : shards.getRow1();
        RuneShard finalRow2 = row == 2 ? selectedShard : shards.getRow2();
        RuneShard finalRow3 = row == 3 ? selectedShard : shards.getRow3();

        if (finalRow1 == null) finalRow1 = RuneShard.ROW1_ADAP;
        if (finalRow2 == null) finalRow2 = RuneShard.ROW2_ADAP;
        if (finalRow3 == null) finalRow3 = RuneShard.ROW3_HP;

        shards.selectShards(finalRow1, finalRow2, finalRow3);
        runePersistence.saveRuneShards(player.getUniqueId(), finalRow1.name(), finalRow2.name(), finalRow3.name());
        playerEventListener.applyPlayerStats(player);

        return true;
    }

    private int getNextShardOption(int row, RuneShard current) {
        if (row == 1) {
            if (current == RuneShard.ROW1_ADAP) return 2;
            if (current == RuneShard.ROW1_AS) return 3;
            return 1;
        } else if (row == 2) {
            if (current == RuneShard.ROW2_ADAP) return 2;
            if (current == RuneShard.ROW2_MS) return 3;
            return 1;
        } else {
            if (current == RuneShard.ROW3_HP) return 2;
            if (current == RuneShard.ROW3_TN) return 3;
            return 1;
        }
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
        player.sendMessage(Component.text("§aAll runes cleared"));
        return true;
    }

    private boolean handleRunesInfo(Player player) {
        dev.ixpu.leaguemechanics.entity.player.PlayerRuneData data = runeManager.getPlayerRuneData(player);
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
        player.sendMessage(Component.text("§7  Primary Path:  §e" + primary + " §7— Keystone: §e" + keystone));
        player.sendMessage(Component.text("§7    Slot 1: §e" + p1 + " §7| Slot 2: §e" + p2 + " §7| Slot 3: §e" + p3));
        player.sendMessage(Component.text("§7  Secondary Path: §e" + secondary));
        player.sendMessage(Component.text("§7    Slot 1: §e" + s1 + " §7| Slot 2: §e" + s2));
        player.sendMessage(Component.text("§7  Shards: §e" + shard1Display + " §7| §e" + shard2Display + " §7| §e" + shard3Display));
        return true;
    }

    private boolean handleInspect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cOnly players can use the inspect command!"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("§cUsage: /lm inspect <player>"));
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(Component.text("§cPlayer not found: §e" + targetName));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(Component.text("§cYou cannot inspect yourself! Use /lm inspect <other-player>"));
            return true;
        }

        InspectGUI.openInspect(player, target);
        sender.sendMessage(Component.text("§6⟳ Inspecting §e" + target.getName()));

        return true;
    }
}