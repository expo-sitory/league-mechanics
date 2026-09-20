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
            }
            completions.add("inspect");
            return filter(completions, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("class")) {
            completions.add("clear");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("runes")) {
            completions.add("info");
            return filter(completions, args[1]);
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("runes") && args[1].equalsIgnoreCase("clear")) {
            return new ArrayList<>();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("inspect")) {
            return org.bukkit.Bukkit.getOnlinePlayers().stream()
                    .map(org.bukkit.entity.Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private List<String> filter(List<String> suggestions, String input) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}