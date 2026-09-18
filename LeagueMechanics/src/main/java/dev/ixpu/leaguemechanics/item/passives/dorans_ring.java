package dev.ixpu.leaguemechanics.item.passives;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class dorans_ring implements ItemPassive {
    private static final int ONHIT_AD_BONUS = 10;

    @Override
    public String getId() {
        return "dorans-ring";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ʜᴇʟᴘɪɴɢ ʜᴀɴᴅ: §fAttacks deal §6" + ONHIT_AD_BONUS + " bonus\n§6physical damage §eon-hit §fagainst §eentities§r.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {
    }

    public int getOnHitAdBonus() {
        return ONHIT_AD_BONUS;
    }
}