package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.rune.DebuffType;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class fated_ashes implements ItemPassive {
    private static final int INFLAME_DURATION_TICKS = 60;

    @Override
    public String getId() {
        return "fated-ashes";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ɪɴғʟᴀᴍᴇ: §fDamaging a target with a Fire Aspect or Flame\n§fenchanted weapon triggers a §92.5 magic damage §fDoT over 3 seconds.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onDealDamage(Player attacker, LivingEntity target, double damage,
                             boolean isPhysical, boolean isMagic) {
        if (target == null) return;
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType().isAir()) return;

        boolean hasFireEnchant = weapon.containsEnchantment(Enchantment.FIRE_ASPECT)
                || weapon.containsEnchantment(Enchantment.FLAME);
        if (!hasFireEnchant) return;

        if (target instanceof Player targetPlayer) {
            DebuffManager.getInstance().applyDebuff(targetPlayer, DebuffType.INFLAME, INFLAME_DURATION_TICKS);
        }
    }
}
