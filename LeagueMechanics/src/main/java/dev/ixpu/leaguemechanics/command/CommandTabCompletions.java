package dev.ixpu.leaguemechanics.command;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RunePath;
import dev.ixpu.leaguemechanics.rune.RuneRegistry;
import dev.ixpu.leaguemechanics.rune.RuneSlot;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandTabCompletions implements org.bukkit.command.TabCompleter {
    private static final List<String> PATHS = List.of(
            "domination", "precision", "inspiration", "resolve", "sorcery"
    );

    private final RuneRegistry runeRegistry;

    public CommandTabCompletions(RuneRegistry runeRegistry) {
        this.runeRegistry = runeRegistry;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (player.hasPermission("leaguemechanics.admin")) {
                completions.add("reload");
                completions.add("shop");
                completions.add("class");
            }
            completions.add("runes");
            completions.add("inspect");
            return filter(completions, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("class")) {
            completions.add("clear");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("runes")) {
            completions.add("select");
            completions.add("info");
            return filter(completions, args[1]);
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("runes") && args[1].equalsIgnoreCase("clear")) {
            return new ArrayList<>();
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("runes") && args[1].equalsIgnoreCase("info")) {
            return new ArrayList<>();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("runes") && args[1].equalsIgnoreCase("select")) {
            completions.add("primary");
            completions.add("shards");
            //completions.add("secondary");
            return filter(completions, args[2]);
        }

        if (args.length >= 4 && args[0].equalsIgnoreCase("runes") && args[1].equalsIgnoreCase("select")) {
            String location = args[2].toLowerCase();
            if (location.equals("primary")) {
                return tabSelectPrimary(player, args);
            } else if (location.equals("secondary")) {
                return tabSelectSecondary(player, args);
            } else if (location.equals("shards")) {
                return tabSelectShards(args);
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("inspect")) {
            return org.bukkit.Bukkit.getOnlinePlayers().stream()
                    .map(org.bukkit.entity.Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private List<String> tabSelectPrimary(Player player, String[] args) {
        if (args.length == 4) {
            return filter(PATHS, args[3]);
        }

        RunePath path = RunePath.fromId(args[3].toLowerCase());
        if (path == null) {
            return new ArrayList<>();
        }

        if (args.length == 5) {
            return filter(getRunesByPathAndSlot(path, RuneSlot.KEYSTONE), args[4]);
        }
        if (args.length == 6) {
            return filter(getRunesByPathAndSlot(path, RuneSlot.PRIMARY_SLOT_1), args[5]);
        }
        if (args.length == 7) {
            return filter(getRunesByPathAndSlot(path, RuneSlot.PRIMARY_SLOT_2), args[6]);
        }
        if (args.length == 8) {
            return filter(getRunesByPathAndSlot(path, RuneSlot.PRIMARY_SLOT_3), args[7]);
        }

        return new ArrayList<>();
    }

    private List<String> tabSelectSecondary(Player player, String[] args) {

        if (args.length == 4) {
            return filter(PATHS, args[3]);
        }

        RunePath path = RunePath.fromId(args[3].toLowerCase());
        if (path == null) {
            return new ArrayList<>();
        }

        if (args.length == 5) {
            return filter(getRunesByPathAndSlot(path, RuneSlot.SECONDARY_SLOT_1), args[4]);
        }

        if (args.length == 6) {
            return filter(getRunesByPathAndSlot(path, RuneSlot.SECONDARY_SLOT_2), args[5]);
        }

        return new ArrayList<>();
    }

    private List<String> getRunesByPathAndSlot(RunePath path, RuneSlot slot) {
        return runeRegistry.getAllRunes().values().stream()
                .filter(rune -> rune.getPath().equals(path) && rune.getSlot().equals(slot))
                .map(CooldownHandler::getId)
                .collect(Collectors.toList());
    }

    private List<String> tabSelectShards(String[] args) {
        List<String> shardOptions = List.of("option-1", "option-2", "option-3");

        if (args.length == 4) {
            return filter(shardOptions, args[3]);
        }
        if (args.length == 5) {
            return filter(shardOptions, args[4]);
        }
        if (args.length == 6) {
            return filter(shardOptions, args[5]);
        }

        return new ArrayList<>();
    }

    private List<String> filter(List<String> suggestions, String input) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}