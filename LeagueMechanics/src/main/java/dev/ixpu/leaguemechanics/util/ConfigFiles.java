package dev.ixpu.leaguemechanics.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public final class ConfigFiles {
    private static final String[] FILES = {"icons.yml", "runes.yml", "abilities.yml", "items.yml", "prices.yml"};

    private ConfigFiles() {}

    public static void saveDefaults(Plugin plugin) {
        for (String name : FILES) {
            if (!new File(plugin.getDataFolder(), name).exists()) {
                plugin.saveResource(name, false);
            }
        }
    }

    public static void merge(Plugin plugin, FileConfiguration target) {
        for (String name : FILES) {
            File file = new File(plugin.getDataFolder(), name);
            if (!file.exists()) continue;
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            for (String key : yaml.getKeys(true)) {
                if (!yaml.isConfigurationSection(key)) {
                    target.set(key, yaml.get(key));
                }
            }
        }
    }
}