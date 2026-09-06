package dev.ixpu.leaguemechanics.listener;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.item.*;
import dev.ixpu.leaguemechanics.gui.InspectGUI;
import dev.ixpu.leaguemechanics.item.passives.ItemPassive;
import dev.ixpu.leaguemechanics.gui.ClassSelectionGUI;
import dev.ixpu.leaguemechanics.item.passives.ItemPassivesRegistry;
import dev.ixpu.leaguemechanics.item.shop.ItemShopData;
import dev.ixpu.leaguemechanics.gui.ItemShopGUI;
import dev.ixpu.leaguemechanics.manager.DamageManager;
import dev.ixpu.leaguemechanics.manager.ItemPassivesManager;
import dev.ixpu.leaguemechanics.manager.ItemShopManager;
import dev.ixpu.leaguemechanics.manager.ItemStatsManager;
import dev.ixpu.leaguemechanics.manager.KillSourceTracker;
import dev.ixpu.leaguemechanics.manager.RuneManager;
import dev.ixpu.leaguemechanics.manager.CritManager;

import dev.ixpu.leaguemechanics.player.PlayerRuneData;
import dev.ixpu.leaguemechanics.player.PlayerStats;

import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.rune.RuneRegistry;
import dev.ixpu.leaguemechanics.rune.RuneCooldownGate;
import dev.ixpu.leaguemechanics.rune.keystones.domination.HailOfBlades;
import dev.ixpu.leaguemechanics.rune.keystones.resolve.GraspOfTheUndying;
import dev.ixpu.leaguemechanics.rune.keystones.resolve.Guardian;

import dev.ixpu.leaguemechanics.rune.keystones.sorcery.DeathfireTorch;
import dev.ixpu.leaguemechanics.util.DebugLogger;
import dev.ixpu.leaguemechanics.util.ItemModifier;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import net.kyori.adventure.text.Component;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Bukkit;
import org.bukkit.util.Vector;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class PlayerEventListener implements Listener, RuneCooldownGate {
    private static final String LEAGUE_HP_MODIFIER = "league_hp";
    private static final String LEAGUE_MS_MODIFIER = "league_ms";

    private final Map<UUID, UUID> hpModifierIds = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> msModifierIds = new ConcurrentHashMap<>();

    private final LeagueMechanics plugin;
    private final RuneManager runeManager;
    private final RuneRegistry runeRegistry;
    private final ItemStatsManager itemStatsManager;

    private final Map<UUID, Long> attackCooldown = new ConcurrentHashMap<>();
    private final Set<UUID> letRunesThroughMap = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Map<UUID, Long>> lastHitTimes = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastPlayerAttacker = new ConcurrentHashMap<>();
    private final Map<UUID, List<ItemStack>> pendingLeagueItemRestore = new HashMap<>();
    private final Map<UUID, Long> lastKillTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> killStreak = new ConcurrentHashMap<>();
    private final Map<UUID, LivingEntity> lastMobDamager = new ConcurrentHashMap<>();
    private final Set<UUID> processedDeaths = ConcurrentHashMap.newKeySet();
    private final Map<UUID, List<Integer>> pendingTaskIds = new ConcurrentHashMap<>();
    private static final long MULTIKILL_WINDOW_MS = 10_000L;
    private static final long ASSIST_WINDOW_MS = 10_000L;

    public PlayerEventListener(LeagueMechanics plugin) {
        this.plugin = plugin;
        this.runeManager = plugin.getRuneManager();
        this.runeRegistry = plugin.getRuneRegistry();
        this.itemStatsManager = plugin.getStatsManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        runeManager.loadPlayerRunes(player);
        dev.ixpu.leaguemechanics.player.PlayerClass.loadPlayerClass(player);
        applyPlayerStats(player);
        CooldownHandler glacial = runeRegistry.getRune("glacial-augment");
        if (glacial instanceof dev.ixpu.leaguemechanics.rune.keystones.inspiration.GlacialAugment glacialAugment) {
            glacialAugment.reapplyDebuffsForRejoin(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (runeRegistry.getRune("grasp-of-the-undying") instanceof GraspOfTheUndying grasp) {
            grasp.resetAbsorption(player);
        }
        runeManager.unloadPlayerRunes(player);
        dev.ixpu.leaguemechanics.player.PlayerClass.unloadPlayer(uuid);
        lastHitTimes.remove(uuid);
        lastKillTime.remove(uuid);
        killStreak.remove(uuid);
        lastMobDamager.remove(uuid);
        processedDeaths.remove(uuid);
        dev.ixpu.leaguemechanics.player.PlayerKDA.getInstance().saveForPlayer(uuid);

        if (ItemPassivesRegistry.getInstance().getPassive("dark-seal") instanceof dev.ixpu.leaguemechanics.item.passives.dark_seal darkSeal) {
            darkSeal.clearStacks(player);
        }

        PlayerStats.invalidateCache(uuid);
        itemStatsManager.invalidateCache(uuid);
        CritManager.getInstance().removePlayer(player);
        removeAllAttributeModifiers(player);
        cancelPendingTasks(uuid);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (handleGUIClick(event, player)) {
            return;
        }

        if (isLeagueItemTransfer(event)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("§cLeague items cannot be transferred to another inventory"));
            return;
        }

        ItemShopGUI.updateShopDisplay(player);

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (preventBundleInsert(currentItem, cursor)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("§cLeague items cannot be inserted into bundles"));
            return;
        }

        if (!checkItemLimit(event, player, currentItem)) {
            return;
        }

        if (!checkItemGroupRestriction(event, player, cursor)) {
            return;
        }

        syncItemStatsOnMove(cursor, currentItem);

        UUID uuid = player.getUniqueId();
        PlayerStats.invalidateCache(uuid);
        itemStatsManager.invalidateCache(uuid);

        scheduleTask(uuid, () -> applyPlayerStats(player), 1L);
    }

    private boolean handleGUIClick(InventoryClickEvent event, Player player) {
        String title = event.getView().getTitle();

        if (title.equals(ItemShopGUI.getInventoryTitle())) {
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                event.setCancelled(true);
                int slot = event.getRawSlot();
                if (slot >= 0 && slot < event.getInventory().getSize()) {
                    ItemShopGUI.getInstance().handleClick(player, slot);
                }
                return true;
            }
        }

        if (title.equals(InspectGUI.getInventoryTitle())) {
            event.setCancelled(true);
            return true;
        }

        if (title.equals(ClassSelectionGUI.getInventoryTitle())) {
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                event.setCancelled(true);
                int slot = event.getRawSlot();
                if (slot >= 0 && slot < event.getInventory().getSize()) {
                    ClassSelectionGUI.getInstance().handleClick(player, slot);
                }
                return true;
            }
        }

        return false;
    }

    private boolean checkItemLimit(InventoryClickEvent event, Player player, ItemStack currentItem) {
        if (currentItem == null || currentItem.getType().isAir() || event.getClickedInventory() == player.getInventory()) {
            return true;
        }

        String itemId = ItemModifier.getItemId(currentItem);
        if (itemId != null) {
            ItemStatsManager statsManager = plugin.getStatsManager();
            if (statsManager.countLeagueItems(player) > 5) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cLeague Items Count: 6/6"));
                return false;
            }
        }
        return true;
    }

    private boolean checkItemGroupRestriction(InventoryClickEvent event, Player player, ItemStack cursor) {
        if (cursor.getType().isAir() || event.getClickedInventory() == player.getInventory()) {
            return true;
        }

        String cursorItemId = ItemModifier.getItemId(cursor);
        if (cursorItemId == null) {
            return true;
        }

        ItemShopData shopData = ItemShopData.getInstance();
        String itemGroup = shopData.getGroup(cursorItemId);
        if (itemGroup == null) {
            return true;
        }

        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) {
                continue;
            }
            String invItemId = ItemModifier.getItemId(inv);
            if (invItemId == null || invItemId.equals(cursorItemId)) {
                continue;
            }
            String ownerGroup = shopData.getGroup(invItemId);
            if (itemGroup.equals(ownerGroup)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cYou can only apply one (1) " + itemGroup + " item to your build."));
                return false;
            }
        }
        return true;
    }

    private void syncItemStatsOnMove(ItemStack cursor, ItemStack currentItem) {
        if (!cursor.getType().isAir()) {
            ItemModifier.syncItemStats(cursor);
        }
        if (currentItem != null && !currentItem.getType().isAir()) {
            ItemModifier.syncItemStats(currentItem);
        }
    }

    private boolean isLeagueItemTransfer(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return false;
        }
        if (event.getClickedInventory() == null) {
            return false;
        }
        boolean topIsPlayer = event.getView().getTopInventory() == player.getInventory();
        boolean clickedIsPlayer = event.getClickedInventory() == player.getInventory();
        if (!clickedIsPlayer) {
            ItemStack cursor = event.getCursor();
            if (!cursor.getType().isAir() && ItemModifier.getItemId(cursor) != null) {
                return true;
            }
            if (event.getClick().isKeyboardClick() && !topIsPlayer) {
                int hotbar = event.getHotbarButton();
                if (hotbar >= 0) {
                    ItemStack held = player.getInventory().getItem(hotbar);
                    if (held != null && !held.getType().isAir() && ItemModifier.getItemId(held) != null) {
                        return true;
                    }
                }
            }
        }
        if (clickedIsPlayer && !topIsPlayer && event.isShiftClick()) {
            ItemStack current = event.getCurrentItem();
            return current != null && !current.getType().isAir() && ItemModifier.getItemId(current) != null;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (event.getView().getTitle().equals(ItemShopGUI.getInventoryTitle())) {
                ItemShopGUI.getInstance().cleanup(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (ItemModifier.getItemId(event.getMainHandItem()) != null
                || ItemModifier.getItemId(event.getOffHandItem()) != null) {
            event.setCancelled(true);
            player.sendMessage(Component.text("§cLeague items cannot be swapped between hands"));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getView().getTitle().equals(ItemShopGUI.getInventoryTitle())) {
            return;
        }
        if (event.getView().getTitle().equals(InspectGUI.getInventoryTitle())) {
            event.setCancelled(true);
            return;
        }
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir() || ItemModifier.getItemId(cursor) == null) {
            return;
        }
        org.bukkit.inventory.InventoryView view = event.getView();
        org.bukkit.inventory.Inventory top = view.getTopInventory();
        int topSize = top.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot >= topSize) {
                continue;
            }
            event.setCancelled(true);
            player.sendMessage(Component.text("§cLeague items cannot be transferred to another inventory"));
            return;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryMoveItem(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
        if (ItemModifier.getItemId(event.getItem()) == null) {
            return;
        }
        if (event.getSource() instanceof org.bukkit.inventory.PlayerInventory) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.BUNDLE) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (ItemModifier.getItemId(mainHand) != null || ItemModifier.getItemId(offHand) != null) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cLeague items cannot be inserted into bundles"));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        PlayerStats stats = PlayerStats.getOrCreate(event.getPlayer());
        stats.setTemporaryASModification(event.isSneaking() ? 1.0 : 0.0);
        applyPlayerStats(event.getPlayer());
    }

    @EventHandler
    public void onLightningDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerRuneData runeData = runeManager.getPlayerRuneData(player);
        if (runeData == null) {
            return;
        }
        for (CooldownHandler rune : runeData.getAllRunes()) {
            if (rune == null) {
                continue;
            }
            rune.onBlockBreak(player, 1);
        }
    }

    @EventHandler
    public void onProjectileDamage(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onAttackDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        event.setDamage(0);
    }

    @EventHandler
    public void onProjectileDraw(PlayerLaunchProjectileEvent event) {
        Player shooter = event.getPlayer();
        if (isAnyHotbarOnCooldown(shooter)) {
            event.setCancelled(true);
        }
        if (isPlayerOnAttackCooldown(shooter)) {
            event.setCancelled(true);
            return;
        }
        setAttackCooldown(shooter);
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) {
            return;
        }
        if (isPlayerOnAttackCooldown(shooter)) {
            event.setCancelled(true);
            return;
        }
        if (isAnyHotbarOnCooldown(shooter)) {
            event.setCancelled(true);
        }
        letRunesThroughMap.remove(shooter.getUniqueId());
        setAttackCooldown(shooter);
    }

    @EventHandler
    public void onArmSwing(PlayerArmSwingEvent event) {
        Player attacker = event.getPlayer();
        if (isPlayerOnAttackCooldown(attacker)) {
            event.setCancelled(true);
            return;
        }
        if (isAnyHotbarOnCooldown(attacker)) {
            event.setCancelled(true);
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }
        if (event.getEntity() instanceof ThrownPotion) {
            return;
        }
        if (event.getHitEntity() == null || !(event.getHitEntity() instanceof LivingEntity target)) {
            return;
        }

        letRunesThroughMap.add(shooter.getUniqueId());

        if (event.getEntity() instanceof Arrow) {
            ItemStack bow = shooter.getInventory().getItemInMainHand();

            if (bow.containsEnchantment(Enchantment.FLAME)) {
                target.setFireTicks(8 * 20);
            }

            if (!target.getUniqueId().equals(shooter.getUniqueId())) {
                Vector direction = target.getLocation().toVector().subtract(shooter.getLocation().toVector()).normalize();
                double knockbackPower = 0.5;

                if (bow.containsEnchantment(Enchantment.KNOCKBACK)) {
                    int knockbackLevel = bow.getEnchantmentLevel(Enchantment.KNOCKBACK);
                    knockbackPower = knockbackLevel * 0.5;
                }

                target.setVelocity(direction.multiply(knockbackPower));
            }
        }
        if (target instanceof Creature creature) {
            creature.setTarget(shooter);
        }

        shooter.getWorld().playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);

        if (target instanceof Player targetPlayer) {
            recordHit(shooter, targetPlayer);
        }

        PlayerRuneData runeData = runeManager.getPlayerRuneData(shooter);
        if (runeData != null) {
            for (CooldownHandler rune : runeData.getAllRunes()) {
                if (rune == null) {
                    continue;
                }
                rune.onProjectileHit(shooter, target);
            }
        }

        event.getEntity().remove();
        damageEvent(shooter, target, "Projectile Hit Event");
        letRunesThroughMap.remove(shooter.getUniqueId());
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (isPlayerOnAttackCooldown(attacker)) {
            event.setCancelled(true);
            return;
        }
        if (isAnyHotbarOnCooldown(attacker)) {
            event.setCancelled(true);
            return;
        }
        if (target instanceof Creature creature) {
            creature.setTarget(attacker);
        }
        letRunesThroughMap.add(attacker.getUniqueId());

        PlayerRuneData runeData = runeManager.getPlayerRuneData(attacker);
        if (runeData != null) {
            for (CooldownHandler rune : runeData.getAllRunes()) {
                if (rune == null) {
                    continue;
                }
                rune.onAttack(attacker, target);
            }
        }

        if (target instanceof Player targetPlayer) {
            PlayerRuneData targetRuneData = runeManager.getPlayerRuneData(targetPlayer);
            if (targetRuneData != null) {
                CooldownHandler targetKeystoneRune = targetRuneData.getKeystoneRune();
                if (targetKeystoneRune instanceof Guardian guardian) {
                    guardian.onTakeDamage(targetPlayer);
                }
            }
            recordHit(attacker, targetPlayer);
        }

        damageEvent(attacker, target, "Melee Hit Event");
        setAttackCooldown(attacker);
        letRunesThroughMap.remove(attacker.getUniqueId());
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PlayerRuneData runeData = runeManager.getPlayerRuneData(player);
        if (runeData == null) {
            return;
        }

        double damage = event.getDamage();
        boolean blockedByShield = player.isBlocking();

        for (CooldownHandler rune : runeData.getAllRunes()) {
            if (rune == null) {
                continue;
            }
            rune.onPlayerDamage(player, damage);
            if (blockedByShield) {
                rune.onShieldBlock(player);
            }
            if (rune instanceof GraspOfTheUndying grasp) {
                grasp.activateGraspOfTheUndying(player, null);
            }
            if (rune instanceof HailOfBlades hailOfBlades) {
                hailOfBlades.activateHailofBlades(player, null);
            }
        }
        if (!(event.getDamager() instanceof Player)) {
            Player attacker = null;
            if (event.getDamager() instanceof Projectile projectile
                    && projectile.getShooter() instanceof Player shooter) {
                attacker = shooter;
            }
            if (!(event.getDamager() instanceof Projectile) && event.getDamager() instanceof LivingEntity mob && !(mob instanceof Player)) {
                lastMobDamager.put(player.getUniqueId(), mob);
            }
            boolean isMagic = event.getCause() == EntityDamageEvent.DamageCause.MAGIC
                    || event.getCause() == EntityDamageEvent.DamageCause.POISON
                    || event.getCause() == EntityDamageEvent.DamageCause.WITHER;
            for (ItemStack inv : player.getInventory().getContents()) {
                if (inv == null || inv.getType().isAir()) continue;
                ItemPassive passive = getEquippedPassive(inv);
                if (passive != null) {
                    passive.onTakeDamage(player, attacker, damage, isMagic);
                }
            }
        }
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

    @EventHandler
    public void onHealthRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.EATING ||
                event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PlayerRuneData runeData = runeManager.getPlayerRuneData(player);
        if (runeData == null) {
            return;
        }

        for (CooldownHandler rune : runeData.getAllRunes()) {
            if (rune == null) {
                continue;
            }
            rune.onPotionEffectGain(player, event.getNewEffect());
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack drop = event.getItemDrop().getItemStack();

        if (!isLeagueItem(drop)) {
            scheduleTask(player.getUniqueId(), () -> applyPlayerStats(player), 1L);
            return;
        }

        boolean shopOpen = isShopOpen(player);

        if (shopOpen) {
            org.bukkit.entity.Item itemEntity = event.getItemDrop();
            UUID playerId = player.getUniqueId();
            scheduleTask(playerId, () -> {
                if (itemEntity.isValid() && !itemEntity.isDead()) {
                    itemEntity.remove();
                }
            });
        } else {
            event.setCancelled(true);
            player.sendMessage(Component.text("§cYou cannot sell League items outside the shop"));
            return;
        }

        ItemShopManager.getInstance().consumeSellXp(player, drop);

        ItemShopGUI.updateShopDisplay(player);
    }

    private boolean isShopOpen(Player player) {
        org.bukkit.inventory.InventoryView view = player.getOpenInventory();
        return ItemShopGUI.getInventoryTitle().equals(view.getTitle());
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

        if (processedDeaths.contains(player.getUniqueId())) {
            return;
        }
        processedDeaths.add(player.getUniqueId());

        dev.ixpu.leaguemechanics.player.PlayerKDA.getInstance().recordDeath(player);

        fireTakedowns(player);
        CritManager.getInstance().resetFailureStreak(player);

        if (killer == null) {
            killer = KillSourceTracker.getInstance().getAndClearSource(player);
        }
        if (killer != null) {
            broadcastKillMessage(killer, player);
        } else if (originalDamager instanceof LivingEntity mob && !(originalDamager instanceof Player)) {
            String mobName = formatMobName(mob);
            String message = "§c[Executed] §c" + player.getName() + " §chas been executed by §c" + mobName;
            Component component = legacyMessage(message);
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(component);
            }
        } else {
            LivingEntity trackedMob = lastMobDamager.remove(player.getUniqueId());
            if (trackedMob != null) {
                String mobName = formatMobName(trackedMob);
                String message = "§c[Executed] §c" + player.getName() + " §chas been executed by §c" + mobName;
                Component component = legacyMessage(message);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendMessage(component);
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        processedDeaths.remove(player.getUniqueId());
        lastMobDamager.remove(player.getUniqueId());
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

    public void removeFromHotbar(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        for (int i = 9; i <= 35; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || slot.getType().isAir()) {
                player.getInventory().setItem(i, item);
                return;
            }
        }
    }

    private int findItemSlot(Player player, ItemStack target) {
        if (target == null || target.getType().isAir()) {
            return -1;
        }
        String targetId = ItemModifier.getItemId(target);
        if (targetId == null) {
            return -1;
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack inv = contents[i];
            if (inv != null && !inv.getType().isAir() && targetId.equals(ItemModifier.getItemId(inv))) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("removal")
    public void removeAllAttributeModifiers(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();

        UUID hpId = hpModifierIds.remove(playerId);
        if (hpId != null) {
            var hpAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (hpAttr != null) {
                hpAttr.removeModifier(hpId);
            }
        }
        UUID msId = msModifierIds.remove(playerId);
        if (msId != null) {
            var msAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (msAttr != null) {
                msAttr.removeModifier(msId);
            }
        }
    }

    public void applyPlayerStats(Player player) {
        syncItemStats(player);
        applyHealthModifier(player);
        applyMovementSpeedModifier(player);
    }

    private void syncItemStats(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        ItemModifier.syncItemStats(mainHand);

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (!offHand.getType().isAir()) {
            ItemModifier.syncItemStats(offHand);
        }

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && !armor.getType().isAir()) {
                ItemModifier.syncItemStats(armor);
            }
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                ItemModifier.syncItemStats(item);
            }
        }
    }

    @SuppressWarnings("removal")
    private void applyHealthModifier(Player player) {
        ItemStatsManager statsManager = plugin.getStatsManager();
        if (statsManager == null) return;

        double classBaseHP = dev.ixpu.leaguemechanics.player.PlayerClass.getPlayerClassBaseHP(player);
        double itemBonusHP = statsManager.getItemHP(player);
        double bonusHP = classBaseHP + itemBonusHP;
        UUID playerId = player.getUniqueId();
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            UUID existingId = hpModifierIds.remove(playerId);
            if (existingId != null) {
                attr.removeModifier(existingId);
            }
        }

        if (bonusHP > 0) {
            UUID newId = UUID.randomUUID();
            hpModifierIds.put(playerId, newId);
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).addModifier(
                    new AttributeModifier(newId, LEAGUE_HP_MODIFIER, bonusHP, AttributeModifier.Operation.ADD_NUMBER)
            );
        }

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    @SuppressWarnings("removal")
    private void applyMovementSpeedModifier(Player player) {
        ItemStatsManager statsManager = plugin.getStatsManager();
        if (statsManager == null) return;

        PlayerStats playerStats = PlayerStats.getOrCreate(player);
        double itemMS = statsManager.getItemMS(player);
        double runeMS = playerStats.getTemporaryMSModification();
        double crouchPenalty = playerStats.getTemporaryASModification() > 0 ? 3.0 : 0.0;
        double bonusMS = itemMS + runeMS - crouchPenalty;
        UUID playerId = player.getUniqueId();

        var attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null) {
            UUID existingId = msModifierIds.remove(playerId);
            if (existingId != null) {
                attr.removeModifier(existingId);
            }
        }

        if (bonusMS != 0.0) {
            double speedBonus = 0.1 * (bonusMS / 100.0);
            UUID newId = UUID.randomUUID();
            msModifierIds.put(playerId, newId);
            Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).addModifier(
                    new AttributeModifier(newId, LEAGUE_MS_MODIFIER, speedBonus, AttributeModifier.Operation.ADD_NUMBER)
            );
        }
    }

    private void setAttackCooldown(Player player) {
        PlayerStats stats = PlayerStats.getOrCreate(player);

        double playerAS = stats.getPlayerAS(player);

        double cooldownTicks = 20.0 / playerAS;
        int cooldownInt = (int) Math.max(1, Math.ceil(cooldownTicks));

        UUID uuid = player.getUniqueId();
        long cooldownMs = (long) (cooldownTicks * 50);
        attackCooldown.put(uuid, System.currentTimeMillis() + cooldownMs);

        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && !item.getType().isAir()) {
                player.setCooldown(item.getType(), cooldownInt);
            }
        }
    }

    public boolean letRunesThrough(Player player) {
        return letRunesThroughMap.contains(player.getUniqueId());
    }

    public boolean isAnyHotbarOnCooldown(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && !item.getType().isAir()) {
                if (player.getCooldown(item.getType()) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isPlayerOnAttackCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        Long cooldownEnd = attackCooldown.get(uuid);
        if (cooldownEnd == null) {
            return false;
        }
        if (System.currentTimeMillis() >= cooldownEnd) {
            attackCooldown.remove(uuid);
            return false;
        }
        return true;
    }

    public long getAttackCooldownRemaining(Player player) {
        UUID uuid = player.getUniqueId();
        if (!attackCooldown.containsKey(uuid)) {
            return 0;
        }

        long cooldownEnd = attackCooldown.get(uuid);
        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    private void damageEvent(Player player, LivingEntity target, String type) {
        if (target instanceof Player targetPlayer) {
            KillSourceTracker.getInstance().setSource(targetPlayer, player);
        }

        DamageManager damage = new DamageManager(itemStatsManager);
        PlayerStats stats = PlayerStats.getOrCreate(player);

        double attackerAD = stats.getPlayerAD(player);
        double attackerAP = stats.getPlayerAP(player);
        double targetAR = damage.getTargetAR(target);
        double targetMR = damage.getTargetMR(target);

        double statsDamage = damage.DamageCalculation(player, target, 0, 0, 0);

        boolean didCrit = false;
        if (statsDamage > 0) {
            double critChance = Math.round(damage.getPlayerCritChance(player) * 10.0) / 10.0;
            if (critChance > 0 && DamageManager.criticalChance(player, critChance)) {
                statsDamage *= DamageManager.getCritDamageMultiplier(player);
                didCrit = true;
            }
        }
        if (didCrit) {
            PlayerRuneData critRuneData = runeManager.getPlayerRuneData(player);
            if (critRuneData != null) {
                for (CooldownHandler rune : critRuneData.getAllRunes()) {
                    if (rune != null) {
                        rune.onCrit(player);
                    }
                }
            }
        }

        boolean isMagic = damage.isMagicDamage();
        boolean isPhysical = !isMagic;
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv == null || inv.getType().isAir()) continue;
            ItemPassive passive = getEquippedPassive(inv);
            if (passive != null) {
                passive.onDealDamage(player, target, statsDamage, isPhysical, isMagic);
            }
        }

        if (target instanceof Player targetPlayer) {
            for (ItemStack inv : targetPlayer.getInventory().getContents()) {
                if (inv == null || inv.getType().isAir()) continue;
                ItemPassive passive = getEquippedPassive(inv);
                if (passive != null) {
                    passive.onTakeDamage(targetPlayer, player, statsDamage, isMagic);
                }
            }
        }

        double baseLifeStealPercent = itemStatsManager.getItemLS(player);
        double effectiveLifeSteal = stats.getEffectiveLifeSteal(player, baseLifeStealPercent);
        if (effectiveLifeSteal > 0 && statsDamage > 0) {
            double healingMultiplier = stats.getEffectiveHealingMultiplier(player);
            double healthRestored = statsDamage * (effectiveLifeSteal / 100.0) * healingMultiplier;
            if (healthRestored > 0) {
                double playerHealth = player.getHealth() + healthRestored;
                playerHealth = Math.min(playerHealth, player.getMaxHealth());
                player.setHealth(playerHealth);
            }
        }

        double newHealth = target.getHealth();
        if (target instanceof Player targetPlayer) {
            double absorption = targetPlayer.getAbsorptionAmount();
            if (statsDamage > absorption) {
                statsDamage -= absorption;
                targetPlayer.setAbsorptionAmount(0);
            } else {
                targetPlayer.setAbsorptionAmount(absorption - statsDamage);
                statsDamage = 0;
            }
        }

        newHealth = Math.clamp(newHealth - statsDamage, 0, target.getMaxHealth());

        for (ItemStack armor : target.getEquipment().getArmorContents()) {
            if (armor != null && !armor.getType().isAir()) {
                armor.damage((short) 1, target);
            }
        }

        DebugLogger.debug(player, "§7----------- §f[ §dDEBUG MODE §f] §7-----------");
        DebugLogger.debug(player, "§aTrigger Type: " + type);
        DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] Total AD = §d" + Math.ceil(attackerAD * 100) / 100.0);
        DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] Total AP = §d" + Math.ceil(attackerAP * 100) / 100.0);
        DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] Crit Chance = §d" + Math.ceil(damage.getPlayerCritChance(player) * 100) / 100.0 + "%");
        DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] Crit Streak = §d" + CritManager.getInstance().getCritStreak(player));
        DebugLogger.debug(player, "§7[Debug] §f[§dTarget§f] Total AR = §d" + Math.ceil(targetAR * 100) / 100.0);
        DebugLogger.debug(player, "§7[Debug] §f[§dTarget§f] Total MR = §d" + Math.ceil(targetMR * 100) / 100.0);

        DebugLogger.debug(player, "§7[Debug] §f[§dAttacker§f] Stats Damage = §d" + Math.ceil(statsDamage * 100) / 100.0);
        DebugLogger.debug(player, "§7[Debug] §f[§dTarget§f] Target New HP = §d" + Math.ceil(newHealth * 100) / 100.0);

        target.damage(0.001);
        if (newHealth <= 0) {
            target.setHealth(0);
            target.damage(1000, player);
        } else {
            target.setHealth(newHealth);
        }
    }

    private boolean preventBundleInsert(ItemStack currentItem, ItemStack cursor) {
        if (currentItem != null && !currentItem.getType().isAir()) {
            if (currentItem.getType() == Material.BUNDLE) {
                if (cursor != null && !cursor.getType().isAir() && ItemModifier.getItemId(cursor) != null) {
                    return true;
                }
            }
        }
        if (cursor != null && !cursor.getType().isAir() && cursor.getType() == Material.BUNDLE) {
            return currentItem != null && !currentItem.getType().isAir() && ItemModifier.getItemId(currentItem) != null;
        }

        return false;
    }

    private ItemPassive getEquippedPassive(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        String itemId = ItemModifier.getItemId(item);
        if (itemId == null) return null;
        ItemStatsRegistry data = ItemStatsData.getInstance().getItem(itemId);
        if (data == null || !data.hasPassive()) return null;
        return ItemPassivesRegistry.getInstance().getPassive(data.getPassiveId());
    }

    private boolean isLeagueItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return ItemModifier.getItemId(item) != null;
    }

    private void recordHit(Player attacker, Player victim) {
        if (attacker == null || victim == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        lastHitTimes
            .computeIfAbsent(victim.getUniqueId(), k -> new HashMap<>())
            .put(attacker.getUniqueId(), System.currentTimeMillis());
        lastPlayerAttacker.put(victim.getUniqueId(), attacker.getUniqueId());
    }

    private void fireTakedowns(Player victim) {
        Map<UUID, Long> attackers = lastHitTimes.remove(victim.getUniqueId());
        if (attackers == null || attackers.isEmpty()) {
            lastPlayerAttacker.remove(victim.getUniqueId());
            return;
        }
        long now = System.currentTimeMillis();
        Player killer = victim.getKiller();
        UUID trackedKiller = lastPlayerAttacker.remove(victim.getUniqueId());
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

    private void broadcastKillMessage(Player killer, Player victim) {
        UUID killerUuid = killer.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastKill = lastKillTime.get(killerUuid);

        int streak;
        if (lastKill != null && (now - lastKill) <= MULTIKILL_WINDOW_MS) {
            streak = killStreak.getOrDefault(killerUuid, 1) + 1;
        } else {
            streak = 1;
        }

        lastKillTime.put(killerUuid, now);
        killStreak.put(killerUuid, streak);

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

    private String formatMobName(LivingEntity mob) {
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

    private int scheduleTask(UUID playerId, Runnable task, long delay) {
        int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, task, delay).getTaskId();
        pendingTaskIds.computeIfAbsent(playerId, k -> new ArrayList<>()).add(taskId);
        return taskId;
    }

    private int scheduleTask(UUID playerId, Runnable task) {
        int taskId = plugin.getServer().getScheduler().runTask(plugin, task).getTaskId();
        pendingTaskIds.computeIfAbsent(playerId, k -> new ArrayList<>()).add(taskId);
        return taskId;
    }

    private void cancelPendingTasks(UUID playerId) {
        List<Integer> taskIds = pendingTaskIds.remove(playerId);
        if (taskIds != null) {
            for (int taskId : taskIds) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
        }
    }

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Component legacyMessage(String legacyText) {
        String mmText = legacyText
            .replace("§0", "<black>")
            .replace("§1", "<dark_blue>")
            .replace("§2", "<dark_green>")
            .replace("§3", "<dark_aqua>")
            .replace("§4", "<dark_red>")
            .replace("§5", "<dark_purple>")
            .replace("§6", "<gold>")
            .replace("§7", "<gray>")
            .replace("§8", "<dark_gray>")
            .replace("§9", "<blue>")
            .replace("§a", "<green>")
            .replace("§b", "<aqua>")
            .replace("§c", "<red>")
            .replace("§d", "<light_purple>")
            .replace("§e", "<yellow>")
            .replace("§f", "<white>")
            .replace("§k", "<obfuscated>")
            .replace("§l", "<bold>")
            .replace("§m", "<strikethrough>")
            .replace("§n", "<underlined>")
            .replace("§o", "<italic>")
            .replace("§r", "<reset>");
        return MINI_MESSAGE.deserialize(mmText);
    }
}
