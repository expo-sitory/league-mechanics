package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;

import dev.ixpu.leaguemechanics.player.PlayerRuneData;

import dev.ixpu.leaguemechanics.util.DebugLogger;
import dev.ixpu.leaguemechanics.util.ItemModifier;

import dev.ixpu.leaguemechanics.item.ItemStatsData;
import dev.ixpu.leaguemechanics.item.ItemStatsRegistry;
import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;

import dev.ixpu.leaguemechanics.manager.*;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RuneRegistry;
import dev.ixpu.leaguemechanics.rune.keystones.sorcery.DeathfireTorch;

import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;


import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Sound;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;

public class DeathListener implements Listener {
    private final LeagueMechanics plugin;
    private final RuneManager runeManager;
    private final RuneRegistry runeRegistry;
    private final PlayerInventoryListener playerInventoryListener;
    private final CombatStateManager combatState = CombatStateManager.getInstance();

    private final Map<UUID, List<ItemStack>> pendingLeagueItemRestore = new HashMap<>();

    private static final long MULTIKILL_WINDOW_MS = 10_000L;
    private static final long ASSIST_WINDOW_MS = 10_000L;

    public DeathListener(LeagueMechanics plugin, PlayerStatsListener playerStatsListener) {
        this.plugin = plugin;
        this.runeManager = plugin.getRuneManager();
        this.runeRegistry = plugin.getRuneRegistry();
        this.playerInventoryListener = new PlayerInventoryListener(plugin, playerStatsListener);
    }

    public int findItemSlot(Player player, ItemStack target) {
        return playerInventoryListener.findItemSlot(player, target);
    }

    public void removeFromHotbar(Player player, ItemStack item) {
        playerInventoryListener.removeFromHotbar(player, item);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity killer = event.getEntity().getKiller();
        if (!(killer instanceof Player player)) {
            return;
        }

        if (runeRegistry.getRune("deathfire-torch") instanceof DeathfireTorch deathfire) {
            deathfire.clearBurnForTarget(event.getEntity().getUniqueId());
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            String itemId = ItemModifier.getItemId(item);
            if (itemId == null) {
                continue;
            }

            ItemStatsRegistry itemData = ItemStatsData.getInstance().getItem(itemId);
            if (itemData == null || !itemData.hasPassive()) {
                continue;
            }

            ItemPassive passive = ItemPassivesRegistry.getInstance().getPassive(itemData.getPassiveId());
            if (passive != null) {
                passive.onEntityKill(player, item);
                ItemModifier.syncItemStats(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        Entity originalDamager = null;
        if (player.getLastDamageCause() != null) {
            originalDamager = player.getLastDamageCause().getEntity();
        }

        Player killer = player.getKiller();

        event.deathMessage(net.kyori.adventure.text.Component.empty());

        if (combatState.isDeathProcessed(player.getUniqueId())) {
            return;
        }
        combatState.addProcessedDeath(player.getUniqueId());

        dev.ixpu.leaguemechanics.player.PlayerKDA.getInstance().recordDeath(player);

        fireTakedowns(player);
        CritManager.getInstance().resetFailureStreak(player);

        if (killer == null) {
            killer = KillSourceTracker.getInstance().getAndClearSource(player);
        }
        if (killer != null) {
            combatState.clearLastMobDamager(player.getUniqueId());
            broadcastKillMessage(killer, player);
        } else if (originalDamager instanceof LivingEntity mob && !(originalDamager instanceof Player)) {
            combatState.clearLastMobDamager(player.getUniqueId());
            String mobName = formatMobName(mob);
            String message = "§c[Executed] §c" + player.getName() + " §chas been executed by §c" + mobName;
            Component component = legacyMessage(message);
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(component);
                online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);
            }
        } else {
            LivingEntity trackedMob = combatState.getAndRemoveLastMobDamager(player.getUniqueId());
            if (trackedMob != null) {
                String mobName = formatMobName(trackedMob);
                String message = "§c[Executed] §c" + player.getName() + " §chas been executed by §c" + mobName;
                Component component = legacyMessage(message);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendMessage(component);
                    online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);
                }
            } else {
                DebugLogger.debug(player, "§c[Death] No killer found. originalDamager=" + originalDamager);
            }
        }

        List<Map.Entry<ItemStack, Integer>> leagueItems = new ArrayList<>();
        for (ItemStack drop : event.getDrops()) {
            if (drop != null && !drop.getType().isAir()) {
                String itemId = ItemModifier.getItemId(drop);
                if (itemId != null) {
                    int originalSlot = findItemSlot(player, drop);
                    leagueItems.add(new AbstractMap.SimpleEntry<>(drop.clone(), originalSlot));
                }
            }
        }

        for (Map.Entry<ItemStack, Integer> entry : leagueItems) {
            event.getDrops().remove(entry.getKey());
        }

        if (!leagueItems.isEmpty()) {
            List<ItemStack> toRestore = new ArrayList<>();
            for (Map.Entry<ItemStack, Integer> entry : leagueItems) {
                toRestore.add(entry.getKey());
            }
            pendingLeagueItemRestore.put(player.getUniqueId(), toRestore);
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            String itemId = ItemModifier.getItemId(item);
            if (itemId == null || !itemId.equals("dark-seal")) {
                continue;
            }

            if (ItemPassivesRegistry.getInstance().getPassive("dark-seal") instanceof dev.ixpu.leaguemechanics.item.passives.dark_seal darkSeal) {
                int currentStacks = darkSeal.getStacks(player);
                int newStacks = Math.max(currentStacks - 2, 0);
                ItemPassivesManager.getInstance().setKillCount(player, "dark-seal", newStacks);
                ItemModifier.syncItemStats(item);
            }
        }
    }

    public void fireTakedowns(Player victim) {
        Map<UUID, Long> attackers = combatState.getLastHitTimes(victim.getUniqueId());
        if (attackers == null || attackers.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Player killer = victim.getKiller();
        UUID trackedKiller = combatState.getLastPlayerAttacker(victim.getUniqueId());
        if (trackedKiller != null) {
            Player tracked = Bukkit.getPlayer(trackedKiller);
            if (tracked != null) {
                killer = tracked;
            }
        }
        UUID killerUuid = killer != null ? killer.getUniqueId() : null;
        for (Map.Entry<UUID, Long> entry : attackers.entrySet()) {
            if (now - entry.getValue() > ASSIST_WINDOW_MS) {
                continue;
            }
            Player attacker = Bukkit.getPlayer(entry.getKey());
            if (attacker == null) {
                continue;
            }
            boolean isKill = killerUuid != null && killerUuid.equals(entry.getKey());

            if (isKill) {
                dev.ixpu.leaguemechanics.player.PlayerKDA.getInstance().recordKill(attacker);
            } else {
                dev.ixpu.leaguemechanics.player.PlayerKDA.getInstance().recordAssist(attacker);
            }

            onTakedown(attacker, victim, isKill);
        }
    }

    public void broadcastKillMessage(Player killer, Player victim) {
        UUID killerUuid = killer.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastKill = combatState.getLastKillTime(killerUuid);

        int streak;
        if (lastKill != null && (now - lastKill) <= MULTIKILL_WINDOW_MS) {
            streak = combatState.getKillStreak(killerUuid) + 1;
        } else {
            streak = 1;
        }

        combatState.setLastKillTime(killerUuid, now);
        combatState.setKillStreak(killerUuid, streak);

        String message;
        switch (streak) {
            case 1 -> message = "§c" + victim.getName() + " §chas been slain by §c" + killer.getName();
            case 2 -> message = "§c" + victim.getName() + " §chas been slain by §c" + killer.getName() + " §cfor a §4double kill!";
            case 3 -> message = "§c" + victim.getName() + " §chas been slain by §c" + killer.getName() + " §cfor a §4triple kill!";
            case 4 -> message = "§c" + victim.getName() + " §chas been slain by §c" + killer.getName() + " §cfor a §4quadra kill!";
            default -> message = "§c" + victim.getName() + " §chas been slain by §c" + killer.getName() + " §cfor a §4penta kill!";
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(legacyMessage(message));
            online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }
    }

    public String formatMobName(LivingEntity mob) {
        String customName = mob.getCustomName();
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }
        String typeName = mob.getType().name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : typeName.toCharArray()) {
            if (c == ' ') {
                sb.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public void onTakedown(Player attacker, Player victim, boolean isKill) {
        PlayerRuneData runeData = runeManager.getPlayerRuneData(attacker);
        if (runeData == null) {
            return;
        }
        for (CooldownHandler rune : runeData.getAllRunes()) {
            if (rune == null) {
                continue;
            }
            rune.onTakedown(attacker, victim, isKill);
        }
    }

    public static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    public Component legacyMessage(String legacyText) {
        return LEGACY_SERIALIZER.deserialize(legacyText);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        combatState.clearProcessedDeath(player.getUniqueId());
        combatState.clearLastMobDamager(player.getUniqueId());

        List<ItemStack> cached = pendingLeagueItemRestore.remove(player.getUniqueId());
        if (cached == null || cached.isEmpty()) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            for (ItemStack item : cached) {
                if (item == null || item.getType().isAir()) continue;
                removeFromHotbar(player, item);
            }
        }, 1L);
    }
}