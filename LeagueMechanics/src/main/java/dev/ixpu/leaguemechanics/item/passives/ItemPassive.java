package dev.ixpu.leaguemechanics.item.passives;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ItemPassive {
    String getId();
    String getDescription();
    void onEntityKill(Player player, ItemStack item);

    default void onDealDamage(Player attacker, LivingEntity target, double damage,
                              boolean isPhysical, boolean isMagic) {}
    default void onTakeDamage(Player victim, Player attacker, double damage, boolean isMagic) {}
}
