package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.manager.DebuffManager;
import dev.ixpu.leaguemechanics.rune.DebuffType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class executioners_calling implements ItemPassive {
    private static final int GRIEVOUS_DURATION_TICKS = 60;

    @Override
    public String getId() {
        return "executioners-calling";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ɢʀɪᴇᴠᴏᴜs ᴡᴏᴜɴᴅs: §fDealing physical damage to a target\n§finflicts them with §cɢʀɪᴇᴠᴏᴜs ᴡᴏᴜɴᴅs §ffor 3 seconds.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onDealDamage(Player attacker, LivingEntity target, double damage,
                             boolean isPhysical, boolean isMagic) {
        if (target instanceof Player targetPlayer && isMagic) {
            DebuffManager.getInstance().applyDebuff(targetPlayer, DebuffType.GRIEVOUS_WOUNDS, GRIEVOUS_DURATION_TICKS);
        }
    }
}
