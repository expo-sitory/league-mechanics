package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.debuff.DebuffType;
import dev.ixpu.leaguemechanics.entity.player.PlayerClass;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilitiesRegistry;
import dev.ixpu.leaguemechanics.rune.CooldownHandler;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClassAbilitiesManager {
    public enum SelectResult {
        OK, NO_CLASS, WRONG_CLASS, INVENTORY_FULL, ALREADY_SELECTED
    }

    private static ClassAbilitiesManager instance;

    private final LeagueMechanics plugin;
    private final NamespacedKey selectedKey;
    private final NamespacedKey itemKey;

    private final Map<String, CooldownHandler> abilityCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> effectUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastAttemptTick = new HashMap<>();
    private static final int HOLD_GAP_TICKS = 6;

    private ClassAbilitiesManager(LeagueMechanics plugin) {
        this.plugin = plugin;
        this.selectedKey = new NamespacedKey(plugin, "class_skill");
        this.itemKey = new NamespacedKey(plugin, "class_skill_item");
    }

    public static ClassAbilitiesManager getInstance() {
        if (instance == null) {
            instance = new ClassAbilitiesManager(LeagueMechanics.getInstance());
        }
        return instance;
    }

    public String getSkillId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    public boolean isSkillItem(ItemStack item) {
        return getSkillId(item) != null;
    }

    private String getSelectedSkillId(Player player) {
        return player.getPersistentDataContainer().get(selectedKey, PersistentDataType.STRING);
    }

    public ClassAbilities getSelectedSkill(Player player) {
        String id = getSelectedSkillId(player);
        if (id == null) return null;
        ClassAbilities skill = ClassAbilitiesRegistry.getInstance().get(id);
        if (skill == null) return null;
        PlayerClassType current = PlayerClass.getPlayerClass(player);
        if (current == null || skill.getClassType() != current) return null;
        return skill;
    }

    public SelectResult select(Player player, ClassAbilities skill) {
        PlayerClassType current = PlayerClass.getPlayerClass(player);
        if (current == null) return SelectResult.NO_CLASS;
        if (skill.getClassType() != current) return SelectResult.WRONG_CLASS;

        ClassAbilities selected = getSelectedSkill(player);
        if (selected != null && selected.getId().equals(skill.getId()) && hasSkillItem(player)) {
            return SelectResult.ALREADY_SELECTED;
        }

        PlayerInventory inv = player.getInventory();
        if (!hasSkillItem(player) && inv.firstEmpty() == -1) {
            return SelectResult.INVENTORY_FULL;
        }

        removeSkillItems(player);
        player.getPersistentDataContainer().set(selectedKey, PersistentDataType.STRING, skill.getId());
        giveItem(player, skill);
        return SelectResult.OK;
    }

    public void clearSelection(Player player) {
        player.getPersistentDataContainer().remove(selectedKey);
        removeSkillItems(player);
    }
    public void onClassChanged(Player player, PlayerClassType newType) {
        String id = getSelectedSkillId(player);
        if (id == null) return;
        ClassAbilities skill = ClassAbilitiesRegistry.getInstance().get(id);
        if (skill == null || newType == null || skill.getClassType() != newType) {
            clearSelection(player);
            player.sendMessage(Component.text("§7Your class skill was removed because your class changed."));
        }
    }

    public boolean hasSkillItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSkillItem(item)) return true;
        }
        return false;
    }

    public void removeSkillItems(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isSkillItem(contents[i])) {
                inv.setItem(i, null);
            }
        }
        ItemStack cursor = player.getItemOnCursor();
        if (isSkillItem(cursor)) {
            player.setItemOnCursor(null);
        }
    }

    public ItemStack createItem(Player player, ClassAbilities skill) {
        ItemStack item = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f" + skill.getDisplayName());
            meta.setMaxStackSize(1);

            List<String> lore = new ArrayList<>(skill.getDescription());
            lore.add("");
            lore.add("§7ᴄᴏᴏʟᴅᴏᴡɴ: §f" + formatSeconds(skill.getCooldownSeconds()));
            if (skill.getHungerCost() > 0) {
                lore.add("§7ᴄᴏꜱᴛ: §f" + skill.getHungerCost() + " ꜱᴀᴛᴜʀᴀᴛɪᴏɴ");
            }
            meta.setLore(lore);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(itemKey, PersistentDataType.STRING, skill.getId());
            item.setItemMeta(meta);
        }
        ItemModifier.setItemModel(item, resolveModel(player, skill));
        return item;
    }

    private String resolveModel(Player player, ClassAbilities skill) {
        boolean hasPack = plugin.getConfig().getBoolean("general-settings.assume-client-pack", false)
                || player.getResourcePackStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED;
        return hasPack ? skill.getModel() : skill.getFallbackModel();
    }

    private boolean giveItem(Player player, ClassAbilities skill) {
        PlayerInventory inv = player.getInventory();
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack existing = inv.getItem(i);
            if (existing == null || existing.getType().isAir()) {
                slot = i;
                break;
            }
        }
        if (slot == -1) {
            slot = inv.firstEmpty();
        }
        if (slot == -1) return false;
        inv.setItem(slot, createItem(player, skill));
        return true;
    }

    public void ensureItem(Player player) {
        if (!player.isOnline()) return;
        ClassAbilities skill = getSelectedSkill(player);
        if (skill == null) {
            if (hasSkillItem(player)) removeSkillItems(player);
            return;
        }

        boolean found = false;
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            String id = getSkillId(contents[i]);
            if (id == null) continue;
            if (!found && id.equalsIgnoreCase(skill.getId())) {
                found = true;
            } else {
                inv.setItem(i, null);
            }
        }
        if (!found) {
            giveItem(player, skill);
        }
    }

    public void refreshModel(Player player) {
        ClassAbilities skill = getSelectedSkill(player);
        if (skill == null) return;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSkillItem(item)) {
                ItemModifier.setItemModel(item, resolveModel(player, skill));
            }
        }
    }

    public void tryCast(Player player) {
        int tick = Bukkit.getCurrentTick();
        Integer last = lastAttemptTick.put(player.getUniqueId(), tick);
        if (last != null && last == tick) return;

        ClassAbilities skill = getSelectedSkill(player);
        if (skill == null) {
            player.sendMessage(Component.text("§cThis skill doesn't belong to your class."));
            return;
        }

        if (skill.isChargeable()) {
            if (skill.isCharging(player)) {
                skill.cast(player);
                return;
            }
            if (last != null && tick - last <= HOLD_GAP_TICKS) return;
        }

        DebuffManager debuffs = DebuffManager.getInstance();
        if ((debuffs.hasDebuff(player, DebuffType.STUN) || debuffs.hasDebuff(player, DebuffType.GROUNDED))
                && !skill.canCastWhileCrowdControlled()) {
            player.sendMessage(Component.text("§cYou can't cast " + skill.getDisplayName() + " right now."));
            return;
        }

        if (getRemainingLockSeconds(player, skill) > 0) {
            if (isEffectActive(player, skill)) {
                player.sendMessage(Component.text("§c" + skill.getDisplayName() + " is still active"));
            } else {
                player.sendMessage(Component.text("§c" + skill.getDisplayName() + " is on cooldown §7("
                        + String.format(Locale.ROOT, "%.1f", getRemainingCooldownSeconds(player, skill)) + "s)"));
            }
            return;
        }

        if (!hasEnoughHunger(player, skill)) {
            player.sendMessage(Component.text("§cNot enough hunger to cast " + skill.getDisplayName()
                    + " §7(needs " + skill.getHungerRequired() + ")"));
            return;
        }

        if (!skill.cast(player)) return;
        startCooldown(player, skill);
    }

    private boolean bypassesHunger(Player player) {
        GameMode mode = player.getGameMode();
        return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
    }

    public boolean hasEnoughHunger(Player player, ClassAbilities skill) {
        if (bypassesHunger(player)) return true;
        return player.getFoodLevel() >= skill.getHungerRequired();
    }

    private void consumeHunger(Player player, ClassAbilities skill) {
        int cost = skill.getHungerCost();
        if (cost <= 0 || bypassesHunger(player)) return;
        int level = Math.max(0, player.getFoodLevel() - cost);
        player.setFoodLevel(level);
        if (player.getSaturation() > level) {
            player.setSaturation(level);
        }
    }

    private CooldownHandler cooldownFor(ClassAbilities skill) {
        return abilityCooldowns.computeIfAbsent(skill.getId(), id -> new CooldownHandler(id) {
            @Override
            protected double effectiveCooldownSeconds(Player player) {
                double duration = cooldownSeconds;
                if (duration <= 0) return duration;
                double ch = Math.min(100.0, Math.max(0.0, PlayerStats.getOrCreate(player).getPlayerCH(player)));
                double maxReduction = Math.max(0.0, duration - skill.getMinCooldownSeconds());
                return duration - Math.min(duration * (ch / 100.0), maxReduction);
            }
        });
    }

    public void startCooldown(Player player, ClassAbilities skill) {
        consumeHunger(player, skill);
        long now = System.currentTimeMillis();
        long effectEnd = now;
        if (skill.startsCooldownAfterEffect()) {
            effectEnd += (long) (skill.getEffectSeconds() * 1000.0);
        }

        UUID uuid = player.getUniqueId();
        CooldownHandler cooldown = cooldownFor(skill);
        cooldown.setCooldownSeconds(skill.getCooldownSeconds());
        cooldown.resetCooldown(player, effectEnd);

        if (effectEnd > now) {
            effectUntil.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(skill.getId(), effectEnd);
        } else {
            Map<String, Long> effects = effectUntil.get(uuid);
            if (effects != null) effects.remove(skill.getId());
        }
    }

    public boolean isEffectActive(Player player, ClassAbilities skill) {
        Map<String, Long> map = effectUntil.get(player.getUniqueId());
        if (map == null) return false;
        Long until = map.get(skill.getId());
        if (until == null) return false;
        if (until <= System.currentTimeMillis()) {
            map.remove(skill.getId());
            return false;
        }
        return true;
    }

    public void refreshEffect(Player player, ClassAbilities skill) {
        if (!isEffectActive(player, skill)) return;
        long effectEnd = System.currentTimeMillis() + (long) (skill.getEffectSeconds() * 1000.0);
        effectUntil.get(player.getUniqueId()).put(skill.getId(), effectEnd);
        cooldownFor(skill).resetCooldown(player, effectEnd);
    }

    public double getRemainingLockSeconds(Player player, ClassAbilities skill) {
        return cooldownFor(skill).getRemainingCooldown(player);
    }

    public double getRemainingCooldownSeconds(Player player, ClassAbilities skill) {
        double lock = getRemainingLockSeconds(player, skill);
        if (lock <= 0) return 0;
        if (isEffectActive(player, skill)) return 0;
        return lock;
    }

    public void endEffects(Player player) {
        Map<String, Long> effects = effectUntil.remove(player.getUniqueId());
        if (effects == null) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : effects.entrySet()) {
            if (entry.getValue() <= now) continue;
            CooldownHandler cooldown = abilityCooldowns.get(entry.getKey());
            if (cooldown != null) cooldown.resetCooldown(player);
        }
    }

    public void forgetPlayer(UUID uuid) {
        lastAttemptTick.remove(uuid);
    }

    private static String formatSeconds(double seconds) {
        if (seconds == Math.rint(seconds)) {
            return (long) seconds + "s";
        }
        return String.format(Locale.ROOT, "%.1fs", seconds);
    }
}