package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.debuff.DebuffType;
import dev.ixpu.leaguemechanics.entity.player.PlayerClass;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.skill.ClassSkill;
import dev.ixpu.leaguemechanics.skill.ClassSkillRegistry;
import dev.ixpu.leaguemechanics.util.ItemModifier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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

public class ClassSkillManager {
    public enum SelectResult {
        OK, NO_CLASS, WRONG_CLASS, INVENTORY_FULL, ALREADY_SELECTED
    }

    private static ClassSkillManager instance;

    private final LeagueMechanics plugin;
    private final NamespacedKey selectedKey;
    private final NamespacedKey itemKey;

    private final Map<UUID, Map<String, Long>> cooldownUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastAttemptTick = new HashMap<>();

    private ClassSkillManager(LeagueMechanics plugin) {
        this.plugin = plugin;
        this.selectedKey = new NamespacedKey(plugin, "class_skill");
        this.itemKey = new NamespacedKey(plugin, "class_skill_item");
    }

    public static ClassSkillManager getInstance() {
        if (instance == null) {
            instance = new ClassSkillManager(LeagueMechanics.getInstance());
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

    public ClassSkill getSelectedSkill(Player player) {
        String id = getSelectedSkillId(player);
        if (id == null) return null;
        ClassSkill skill = ClassSkillRegistry.getInstance().get(id);
        if (skill == null) return null;
        PlayerClassType current = PlayerClass.getPlayerClass(player);
        if (current == null || skill.getClassType() != current) return null;
        return skill;
    }

    public SelectResult select(Player player, ClassSkill skill) {
        PlayerClassType current = PlayerClass.getPlayerClass(player);
        if (current == null) return SelectResult.NO_CLASS;
        if (skill.getClassType() != current) return SelectResult.WRONG_CLASS;

        ClassSkill selected = getSelectedSkill(player);
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
        ClassSkill skill = ClassSkillRegistry.getInstance().get(id);
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

    public ItemStack createItem(Player player, ClassSkill skill) {
        ItemStack item = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f" + skill.getDisplayName());
            meta.setMaxStackSize(1);

            List<String> lore = new ArrayList<>(skill.getDescription());
            lore.add("");
            lore.add("§7ᴄᴏᴏʟᴅᴏᴡɴ: §f" + formatSeconds(skill.getCooldownSeconds()));
            meta.setLore(lore);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(itemKey, PersistentDataType.STRING, skill.getId());
            item.setItemMeta(meta);
        }
        ItemModifier.setItemModel(item, resolveModel(player, skill));
        return item;
    }

    private String resolveModel(Player player, ClassSkill skill) {
        boolean hasPack = plugin.getConfig().getBoolean("general-settings.assume-client-pack", false)
                || player.getResourcePackStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED;
        return hasPack ? skill.getModel() : skill.getFallbackModel();
    }

    private boolean giveItem(Player player, ClassSkill skill) {
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
        ClassSkill skill = getSelectedSkill(player);
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
        ClassSkill skill = getSelectedSkill(player);
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

        ClassSkill skill = getSelectedSkill(player);
        if (skill == null) {
            player.sendMessage(Component.text("§cThis skill doesn't belong to your class."));
            return;
        }

        DebuffManager debuffs = DebuffManager.getInstance();
        if (debuffs.hasDebuff(player, DebuffType.STUN) || debuffs.hasDebuff(player, DebuffType.GROUNDED)) {
            player.sendMessage(Component.text("§cYou can't cast " + skill.getDisplayName() + " right now."));
            return;
        }

        double remaining = getRemainingCooldownSeconds(player, skill);
        if (remaining > 0) {
            player.sendMessage(Component.text("§c" + skill.getDisplayName() + " is on cooldown §7("
                    + String.format(Locale.ROOT, "%.1f", remaining) + "s)"));
            return;
        }

        if (!skill.cast(player)) return;
        startCooldown(player, skill);
    }

    private void startCooldown(Player player, ClassSkill skill) {
        long until = System.currentTimeMillis() + (long) (skill.getCooldownSeconds() * 1000.0);
        cooldownUntil.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(skill.getId(), until);
    }

    public double getRemainingCooldownSeconds(Player player, ClassSkill skill) {
        Map<String, Long> map = cooldownUntil.get(player.getUniqueId());
        if (map == null) return 0;
        Long until = map.get(skill.getId());
        if (until == null) return 0;
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0) {
            map.remove(skill.getId());
            return 0;
        }
        return remaining / 1000.0;
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
