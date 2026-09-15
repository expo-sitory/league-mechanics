package dev.ixpu.leaguemechanics.item.passives;

import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class wardens_mail implements ItemPassive {
    private static final double CRIT_DAMAGE_REDUCTION = 0.20;
    private static final long TOGGLE_DURATION_MS = 5000L;

    private final java.util.Map<java.util.UUID, Long> reductionExpiry = new java.util.HashMap<>();

    @Override
    public String getId() {
        return "wardens-mail";
    }

    @Override
    public String getDescription() {
        return "§7ᴜɴɪQᴜᴇ – ʀᴏᴄᴋ sᴏʟɪᴅ: §fReduces incoming critical strike damage by §a20%§f.";
    }

    @Override
    public void onEntityKill(Player player, ItemStack item) {}

    @Override
    public void onTakeDamage(Player victim, Player attacker, double damage, boolean isMagic) {
        if (victim == null) return;
        PlayerStats stats = PlayerStats.getOrCreate(victim);
        stats.modifyCritDamage(-CRIT_DAMAGE_REDUCTION);
        reductionExpiry.put(victim.getUniqueId(), System.currentTimeMillis() + TOGGLE_DURATION_MS);
    }

    @Override
    public void onDealDamage(Player attacker, LivingEntity target, double damage,
                             boolean isPhysical, boolean isMagic) {
        if (attacker == null) return;
        PlayerStats stats = PlayerStats.getOrCreate(attacker);
        stats.modifyCritDamage(-CRIT_DAMAGE_REDUCTION);
        reductionExpiry.put(attacker.getUniqueId(), System.currentTimeMillis() + TOGGLE_DURATION_MS);
    }

    public boolean hasRockSolid(Player player) {
        if (player == null) return false;
        Long expiry = reductionExpiry.get(player.getUniqueId());
        return expiry != null && System.currentTimeMillis() < expiry;
    }
}
