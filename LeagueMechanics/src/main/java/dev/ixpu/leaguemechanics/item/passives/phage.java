package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.player.PlayerStats;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class phage implements ItemPassive {
    private static final double RAGE_MS_BONUS = 15.0;
    private static final long RAGE_DURATION_MS = 2000L;
    private static final long RAGE_DURATION_TICKS = 40L;

    @Override
    public String getId() {
        return "phage";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ʀᴀɢᴇ: §fAttacking a target grants §a+15 movement\n§aspeed §ffor §e2 seconds§f, §fwhich refreshes on each hit.";
    }

}
