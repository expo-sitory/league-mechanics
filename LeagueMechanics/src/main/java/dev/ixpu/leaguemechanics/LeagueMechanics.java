package dev.ixpu.leaguemechanics;

import dev.ixpu.leaguemechanics.gui.ClassSelectionGUI;

import dev.ixpu.leaguemechanics.placeholder.PlaceholderRegistry;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import dev.ixpu.leaguemechanics.util.RunePersistence;

import dev.ixpu.leaguemechanics.command.CommandHandler;
import dev.ixpu.leaguemechanics.command.CommandTabCompletions;

import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.manager.RuneManager;
import dev.ixpu.leaguemechanics.manager.ItemStatsManager;
import dev.ixpu.leaguemechanics.manager.ItemShopManager;

import dev.ixpu.leaguemechanics.listener.DamageListener;
import dev.ixpu.leaguemechanics.listener.DeathListener;
import dev.ixpu.leaguemechanics.listener.PlayerEventListener;
import dev.ixpu.leaguemechanics.listener.PlayerStatsListener;
import dev.ixpu.leaguemechanics.listener.PlayerInventoryListener;
import dev.ixpu.leaguemechanics.listener.RuneListener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RuneRegistry;

import dev.ixpu.leaguemechanics.rune.keystones.precision.Conqueror;
import dev.ixpu.leaguemechanics.rune.keystones.precision.FleetFootwork;
import dev.ixpu.leaguemechanics.rune.keystones.precision.LethalTempo;
import dev.ixpu.leaguemechanics.rune.keystones.precision.PressTheAttack;

import dev.ixpu.leaguemechanics.rune.keystones.domination.DarkHarvest;
import dev.ixpu.leaguemechanics.rune.keystones.domination.Electrocute;
import dev.ixpu.leaguemechanics.rune.keystones.domination.HailOfBlades;

import dev.ixpu.leaguemechanics.rune.keystones.resolve.GraspOfTheUndying;
import dev.ixpu.leaguemechanics.rune.keystones.resolve.AfterShock;
import dev.ixpu.leaguemechanics.rune.keystones.resolve.Guardian;

import dev.ixpu.leaguemechanics.rune.keystones.sorcery.ArcaneComet;
import dev.ixpu.leaguemechanics.rune.keystones.sorcery.StormRaiderSurge;
import dev.ixpu.leaguemechanics.rune.keystones.sorcery.DeathfireTorch;

import dev.ixpu.leaguemechanics.rune.keystones.inspiration.GlacialAugment;
import dev.ixpu.leaguemechanics.rune.keystones.inspiration.FirstStrike;

import java.util.Objects;


public class LeagueMechanics extends JavaPlugin {
    private static LeagueMechanics instance;
    private RuneRegistry runeRegistry;
    private RuneManager runeManager;
    private ItemStatsManager itemStatsManager;
    private RunePersistence runePersistence;
    private PlayerStatsListener playerStatsListener;
    private PlayerEventListener playerEventListener;
    private PlayerInventoryListener playerInventoryListener;
    private DamageListener damageListener;
    private DeathListener deathListener;
    private RuneListener runeListener;
    private boolean debugMode;

    @Override
    public void onEnable() {
        instance = this;
        runeRegistry = RuneRegistry.getInstance();
        runeManager = new RuneManager(this);
        itemStatsManager = new ItemStatsManager();
        runePersistence = new RunePersistence(this);
        playerStatsListener = new PlayerStatsListener(this);
        playerEventListener = new PlayerEventListener(this, playerStatsListener, damageListener);
        playerInventoryListener = new PlayerInventoryListener(this, playerStatsListener);
        damageListener = new DamageListener(this, playerStatsListener);
        deathListener = new DeathListener(this, playerStatsListener);
        runeListener = new RuneListener(this);


        getLogger().info("League Mechanics is starting...");

        saveDefaultConfig();
        reloadConfig();
        debugMode = getConfig().getBoolean("debug", false);

        ItemModifier.initialize(this);
        registerRunes();
        registerCommands();
        registerRegenTask();
        registerHotbarCleanupTask();
        registerDebuffTicker();

        Bukkit.getPluginManager().registerEvents(damageListener, this);
        Bukkit.getPluginManager().registerEvents(deathListener, this);
        Bukkit.getPluginManager().registerEvents(playerEventListener, this);
        Bukkit.getPluginManager().registerEvents(playerInventoryListener, this);
        Bukkit.getPluginManager().registerEvents(runeListener, this);
        Bukkit.getPluginManager().registerEvents(ItemShopManager.getInstance(), this);

        startRuneTicker();
        registerPlaceholders();

        getLogger().info("League Mechanics has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("League Mechanics is shutting down...");

        if (playerEventListener != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerEventListener.removeAllAttributeModifiers(player);
            }
        }

        dev.ixpu.leaguemechanics.player.PlayerKDA.getInstance().saveAll();

        CooldownHandler glacial = runeRegistry != null ? runeRegistry.getRune("glacial-augment") : null;
        if (glacial instanceof dev.ixpu.leaguemechanics.rune.keystones.inspiration.GlacialAugment glacialAugment) {
            glacialAugment.clearAllSnowBlocks();
        }

        if (runeRegistry != null) {
            runeRegistry.clearRegistry();
        }

        getLogger().info("League Mechanics has been disabled!");
    }

    private void startRuneTicker() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (runeManager != null) {
                runeManager.tickAllPlayerRunes();
            }
        }, 0L, 1L);
    }

    public static LeagueMechanics getInstance() {
        return instance;
    }


    public static boolean playerHasVerdantBarrier(Player player) {
        if (player == null) return false;
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) continue;
            if ("verdant-barrier".equals(dev.ixpu.leaguemechanics.util.ItemModifier.getItemId(inv))) {
                return true;
            }
        }
        return false;
    }

    public RuneRegistry getRuneRegistry() {
        return runeRegistry;
    }

    public RuneManager getRuneManager() {
        return runeManager;
    }

    public ItemStatsManager getStatsManager() {
        return itemStatsManager;
    }

    public PlayerEventListener getPlayerEventListener() {
        return playerEventListener;
    }

    public DamageListener getDamageListener() {
        return damageListener;
    }

    public RunePersistence getRunePersistence() {
        return runePersistence;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void reloadPlugin() {
        getLogger().info("Reloading LeagueMechanics configuration...");

        reloadConfig();
        debugMode = getConfig().getBoolean("debug", false);

        if (runeRegistry != null) {
            runeRegistry.clearRegistry();
        }
        registerRunes();

        for (Player player : Bukkit.getOnlinePlayers()) {
            runeManager.loadPlayerRunes(player);
            dev.ixpu.leaguemechanics.player.PlayerClass.loadPlayerClass(player);
        }

        getLogger().info("LeagueMechanics reload completed!");
    }

    private void registerCommands() {
        CommandHandler commandExecutor = new CommandHandler(this, itemStatsManager, runeManager, runePersistence, playerEventListener);
        CommandTabCompletions tabCompleter = new CommandTabCompletions(runeRegistry);
        Objects.requireNonNull(getCommand("leaguemechanics")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("leaguemechanics")).setTabCompleter(tabCompleter);
        getLogger().info("Commands registered!");
    }

    public void registerRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStatsManager itemStatsManager = getStatsManager();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    double healthRegen = itemStatsManager.getItemHR(player);
                    double saturationRegen = itemStatsManager.getItemSR(player);

                    dev.ixpu.leaguemechanics.player.PlayerStats ps = dev.ixpu.leaguemechanics.player.PlayerStats.getOrCreate(player);
                    double effectiveHR = ps.getEffectiveHealthRegen(player, healthRegen);

                    if (effectiveHR > 0) {
                        double newHealth = Math.min(player.getHealth() + effectiveHR, player.getMaxHealth());
                        player.setHealth(newHealth);
                    }

                    if (saturationRegen > 0) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 1, (int) saturationRegen, false, false));
                    }
                    if (playerHasVerdantBarrier(player)) {
                        dev.ixpu.leaguemechanics.item.passives.verdant_barrier passive =
                                (dev.ixpu.leaguemechanics.item.passives.verdant_barrier)
                                        dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry.getInstance().getPassive("verdant-barrier");
                        if (passive != null && !passive.isOnCooldown(player)) {
                            passive.grantAbsorptionHeart(player);
                        }
                    } else {
                        dev.ixpu.leaguemechanics.item.passives.verdant_barrier passive =
                                (dev.ixpu.leaguemechanics.item.passives.verdant_barrier)
                                        dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry.getInstance().getPassive("verdant-barrier");
                        if (passive != null) {
                            passive.resetHearts(player);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 100);
    }

    public void registerHotbarCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!ClassSelectionGUI.hasPlayerSelectedClass(player)) {
                        org.bukkit.inventory.InventoryView view = player.getOpenInventory();
                        if (view.getType() == org.bukkit.event.inventory.InventoryType.CRAFTING
                                || view.getType() == org.bukkit.event.inventory.InventoryType.CREATIVE) {
                            ClassSelectionGUI.openForPlayer(player);
                        }
                    }

                    ItemStack[] inventoryContents = player.getInventory().getContents();
                    for (int i = 0; i < 9; i++) {
                        ItemStack item = inventoryContents[i];
                        if (item != null && !item.getType().isAir() && ItemModifier.getItemId(item) != null) {
                            playerEventListener.removeFromHotbar(player, item);
                            player.getInventory().setItem(i, null);
                        }
                    }
                    if (inventoryContents[40] != null && !inventoryContents[40].getType().isAir() && ItemModifier.getItemId(inventoryContents[40]) != null) {
                        playerEventListener.removeFromHotbar(player, inventoryContents[40]);
                        player.getInventory().setItem(40, null);
                    }
                }
            }
        }.runTaskTimer(this, 0, 0L);
    }

    public void registerDebuffTicker() {
        DebuffManager.getInstance();
        dev.ixpu.leaguemechanics.rune.DebuffTicker ticker = new dev.ixpu.leaguemechanics.rune.DebuffTicker();
        new BukkitRunnable() {
            @Override
            public void run() {
                DebuffManager.getInstance().tickAll();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ticker.tick(player, null);
                }
            }
        }.runTaskTimer(this, 0, 1L);
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderRegistry(this).register();
            getLogger().info("PlaceholderAPI placeholders registered!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }
    }

    private void registerRunes() {
        FileConfiguration config = getConfig();

        PressTheAttack pressTheAttack = new PressTheAttack(config, playerEventListener);
        loadRuneCooldown(pressTheAttack, "runes.keystones.precision.press-the-attack.cooldown");
        runeRegistry.registerRune(pressTheAttack);

        LethalTempo lethalTempo = new LethalTempo(config, playerEventListener);
        loadRuneCooldown(lethalTempo, "runes.keystones.precision.lethal-tempo.cooldown");
        runeRegistry.registerRune(lethalTempo);

        Conqueror conqueror = new Conqueror(config, playerEventListener);
        loadRuneCooldown(conqueror, "runes.keystones.precision.conqueror.cooldown");
        runeRegistry.registerRune(conqueror);

        FleetFootwork fleetFootwork = new FleetFootwork(config);
        loadRuneCooldown(fleetFootwork, "runes.keystones.precision.fleet-footwork.cooldown");
        runeRegistry.registerRune(fleetFootwork);

        Electrocute electrocute = new Electrocute(config, playerEventListener);
        loadRuneCooldown(electrocute, "runes.keystones.domination.electrocute.cooldown");
        runeRegistry.registerRune(electrocute);

        DarkHarvest darkHarvest = new DarkHarvest(config, playerEventListener);
        loadRuneCooldown(darkHarvest, "runes.keystones.domination.dark-harvest.cooldown");
        runeRegistry.registerRune(darkHarvest);

        HailOfBlades hailOfBlades = new HailOfBlades(config, playerEventListener);
        loadRuneCooldown(hailOfBlades, "runes.keystones.domination.hail-of-blades.cooldown");
        runeRegistry.registerRune(hailOfBlades);

        GraspOfTheUndying grasp = new GraspOfTheUndying(config, playerEventListener);
        loadRuneCooldown(grasp, "runes.keystones.resolve.grasp-of-the-undying.cooldown");
        runeRegistry.registerRune(grasp);

        AfterShock aftershock = new AfterShock(config, playerEventListener);
        loadRuneCooldown(aftershock, "runes.keystones.resolve.aftershock.cooldown");
        runeRegistry.registerRune(aftershock);

        Guardian guardian = new Guardian(config);
        loadRuneCooldown(guardian, "runes.keystones.resolve.guardian.cooldown");
        runeRegistry.registerRune(guardian);

        ArcaneComet arcaneComet = new ArcaneComet(config, this, playerEventListener);
        loadRuneCooldown(arcaneComet, "runes.keystones.sorcery.arcane-comet.cooldown");
        runeRegistry.registerRune(arcaneComet);

        StormRaiderSurge stormRaiderSurge = new StormRaiderSurge(config, playerEventListener);
        loadRuneCooldown(stormRaiderSurge, "runes.keystones.sorcery.storm-raider-surge.cooldown");
        runeRegistry.registerRune(stormRaiderSurge);

        DeathfireTorch deathfireTorch = new DeathfireTorch(config, playerEventListener);
        runeRegistry.registerRune(deathfireTorch);

        GlacialAugment glacialAugment = new GlacialAugment(config, this);
        loadRuneCooldown(glacialAugment, "runes.keystones.inspiration.glacial-augment.cooldown");
        runeRegistry.registerRune(glacialAugment);

        FirstStrike firstStrike = new FirstStrike(config, this, playerEventListener);
        loadRuneCooldown(firstStrike, "runes.keystones.inspiration.first-strike.cooldown");
        runeRegistry.registerRune(firstStrike);

        getLogger().info(() -> "Registered " + runeRegistry.getAllRunes().size() + " runes");
    }

    private void loadRuneCooldown(CooldownHandler rune, String configPath) {
        if (getConfig().contains(configPath)) {
            double cooldown = getConfig().getDouble(configPath);
            rune.setCooldownSeconds(cooldown);
            getLogger().info(() -> "Loaded cooldown for " + rune.getId() + ": " + cooldown + "s");
        }
    }
}