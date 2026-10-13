package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.ProjectileHoming;
import dev.ixpu.leaguemechanics.entity.player.PlayerRuneData;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilitiesRegistry;
import dev.ixpu.leaguemechanics.entity.player.abilities.assassin.BattleRoar;
import dev.ixpu.leaguemechanics.entity.player.abilities.assassin.Meditate;
import dev.ixpu.leaguemechanics.entity.player.abilities.fighter.HollowedMist;
import dev.ixpu.leaguemechanics.entity.player.abilities.fighter.Ragnarok;
import dev.ixpu.leaguemechanics.entity.player.abilities.fighter.UndyingRage;
import dev.ixpu.leaguemechanics.entity.player.abilities.marksman.SprayAndPray;
import dev.ixpu.leaguemechanics.entity.player.abilities.mage.PetrifyingGaze;
import dev.ixpu.leaguemechanics.entity.player.abilities.mage.Riftwalk;
import dev.ixpu.leaguemechanics.entity.player.abilities.mage.SanguinePool;
import dev.ixpu.leaguemechanics.entity.player.abilities.marksman.FinalHour;
import dev.ixpu.leaguemechanics.entity.player.abilities.marksman.LambsRespite;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import dev.ixpu.leaguemechanics.manager.ClassAbilitiesManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ClassAbilitiesListener implements Listener {
    private static final long ACTIONBAR_FALLBACK_TICKS = 2L;
    private static final String CANNOT_MOVE = "§cAbility items cannot be moved to another inventory";

    private final LeagueMechanics plugin;
    private final ClassAbilitiesManager skills = ClassAbilitiesManager.getInstance();

    public ClassAbilitiesListener(LeagueMechanics plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickActionBarFallback,
                ACTIONBAR_FALLBACK_TICKS, ACTIONBAR_FALLBACK_TICKS);
    }

    private void tickActionBarFallback() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ClassAbilities skill = skills.getSelectedSkill(player);
            if (skill == null) continue;
            if (skills.getRemainingCooldownSeconds(player, skill) <= 0 && !skills.isEffectActive(player, skill)) continue;
            if (hasEquippedRunes(player)) continue;
            String text = PlayerStats.getOrCreate(player).getActionBarSections(player).trim();
            if (!text.isEmpty()) {
                player.sendActionBar(Component.text(text));
            }
        }
    }

    private boolean hasEquippedRunes(Player player) {
        var runeManager = plugin.getRuneManager();
        if (runeManager == null || !runeManager.hasActiveRunes(player)) return false;
        PlayerRuneData data = runeManager.getPlayerRuneData(player);
        if (data == null) return false;
        for (CooldownHandler rune : data.getAllRunes()) {
            if (rune != null) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (!skills.isSkillItem(event.getItem())) return;

        Player player = event.getPlayer();
        if (action == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType().isInteractable() && !player.isSneaking()) {
                return;
            }
        }

        event.setCancelled(true);
        skills.tryCast(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!skills.isSkillItem(player.getInventory().getItemInMainHand())) return;

        event.setCancelled(true);
        if (event instanceof PlayerInteractAtEntityEvent) return;
        skills.tryCast(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean currentIsSkill = skills.isSkillItem(current);
        boolean cursorIsSkill = skills.isSkillItem(cursor);

        ClickType click = event.getClick();
        Inventory clicked = event.getClickedInventory();
        Inventory playerInv = player.getInventory();
        boolean clickedOther = clicked != null && clicked != playerInv;

        ItemStack hotbarItem = null;
        if (click == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0) {
            hotbarItem = playerInv.getItem(event.getHotbarButton());
        }

        boolean deny = false;

        if (cursorIsSkill && (clickedOther || clicked == null
                || (clicked == playerInv && event.getSlot() == 40))) {
            deny = true;
        }
        if (currentIsSkill && event.isShiftClick()
                && event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
            deny = true;
        }
        if (clickedOther && skills.isSkillItem(hotbarItem)) {
            deny = true;
        }
        if (currentIsSkill && (click == ClickType.DROP || click == ClickType.CONTROL_DROP)) {
            deny = true;
        }
        if (click == ClickType.SWAP_OFFHAND
                && (currentIsSkill || skills.isSkillItem(player.getInventory().getItemInOffHand()))) {
            deny = true;
        }
        if ((cursorIsSkill && current != null && current.getType() == Material.BUNDLE)
                || (currentIsSkill && cursor != null && cursor.getType() == Material.BUNDLE)) {
            deny = true;
        }

        if (deny) {
            event.setCancelled(true);
            player.sendMessage(Component.text(CANNOT_MOVE));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!skills.isSkillItem(event.getOldCursor())) return;

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                player.sendMessage(Component.text(CANNOT_MOVE));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        if (!skills.isSkillItem(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.text("§cAbility items cannot be dropped"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (skills.isSkillItem(event.getMainHandItem()) || skills.isSkillItem(event.getOffHandItem())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("§cAbility items cannot be swapped between hands"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMeditateDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Meditate.reduceDamage(player, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBattleRoarDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        BattleRoar.trackDamage(player, event.getFinalDamage());
    }

    @EventHandler
    public void onMeditateSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        if (ClassAbilitiesRegistry.getInstance().get("meditate") instanceof Meditate meditate
                && meditate.isCharging(event.getPlayer())) {
            meditate.interruptCharge(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInspireArmorDamage(PlayerItemDamageEvent event) {
        if (!AbilityEffectManager.getInstance().isArmorDurabilityProtected(event.getPlayer())) return;
        EquipmentSlot slot = event.getItem().getType().getEquipmentSlot();
        if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFinalHourTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player && FinalHour.isVanished(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUndyingRageDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UndyingRage.protect(player, event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHollowedMistDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (HollowedMist.blocks(player, event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPetrifyingGazeStunnedAttack(EntityDamageByEntityEvent event) {
        Entity source = event.getDamager();
        if (source instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            source = shooter;
        }
        if (PetrifyingGaze.isStunned(source)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRagnarokAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            Ragnarok.refreshOnAttack(attacker);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInvulnerableDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !AbilityEffectManager.getInstance().isInvulnerable(player)) return;
        switch (event.getCause()) {
            case VOID, SUICIDE, KILL -> {
                return;
            }
            default -> event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSanguinePoolInvulnerable(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !SanguinePool.isActive(player)) return;
        switch (event.getCause()) {
            case VOID, SUICIDE, KILL -> {
                return;
            }
            default -> event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSanguinePoolAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && SanguinePool.isActive(attacker)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSanguinePoolLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player shooter && SanguinePool.isActive(shooter)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSanguinePoolBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player shooter && SanguinePool.isActive(shooter)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSanguinePoolThrow(PlayerLaunchProjectileEvent event) {
        if (SanguinePool.isActive(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMeditateAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && AbilityEffectManager.getInstance().isMeditating(attacker)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMeditateLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player shooter
                && AbilityEffectManager.getInstance().isMeditating(shooter)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMeditateBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player shooter && AbilityEffectManager.getInstance().isMeditating(shooter)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMeditateThrow(PlayerLaunchProjectileEvent event) {
        if (AbilityEffectManager.getInstance().isMeditating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSprayAndPrayLaunch(ProjectileLaunchEvent event) {
        SprayAndPray.onLaunch(event.getEntity());
        ProjectileHoming.onLaunch(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSprayAndPrayHit(ProjectileHitEvent event) {
        SprayAndPray.onHit(event.getEntity(), event.getHitEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLambsRespiteDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.LivingEntity entity)) return;
        LambsRespite.protect(entity, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUndyingRageHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (UndyingRage.isActive(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(skills::isSkillItem);
        event.getItemsToKeep().removeIf(skills::isSkillItem);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> skills.ensureItem(player), 2L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SanguinePool.cleanup(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            skills.ensureItem(player);
            skills.refreshModel(player);
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        skills.forgetPlayer(event.getPlayer().getUniqueId());
        AbilityEffectManager.getInstance().purge(event.getPlayer().getUniqueId());
        HollowedMist.cleanup(event.getPlayer().getUniqueId());
        Riftwalk.cleanup(event.getPlayer().getUniqueId());
        SanguinePool.cleanup(event.getPlayer());
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        boolean finalOutcome = status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED
                || status == PlayerResourcePackStatusEvent.Status.DECLINED
                || status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD
                || status == PlayerResourcePackStatusEvent.Status.DISCARDED
                || status == PlayerResourcePackStatusEvent.Status.INVALID_URL;
        if (!finalOutcome) return;
        skills.refreshModel(event.getPlayer());
    }
}