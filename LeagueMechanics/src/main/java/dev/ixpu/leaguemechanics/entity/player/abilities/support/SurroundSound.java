package dev.ixpu.leaguemechanics.entity.player.abilities.support;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.entity.player.abilities.AbilityEffectType;
import dev.ixpu.leaguemechanics.entity.player.abilities.ClassAbilities;
import dev.ixpu.leaguemechanics.entity.player.abilities.fighter.UndyingRage;
import dev.ixpu.leaguemechanics.manager.AbilityEffectManager;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class SurroundSound extends ClassAbilities {
    private static final String CFG = "skills.surround-sounds.";

    private static final double DEFAULT_COOLDOWN_SECONDS = 90.0;
    private static final double DEFAULT_MIN_COOLDOWN_SECONDS = 20.0;
    private static final double DEFAULT_RADIUS = 7.0;
    private static final double DEFAULT_ABSORPTION_HEARTS = 10.0;
    private static final double DEFAULT_HEAL_PERCENT = 20.0;
    private static final double DEFAULT_HUNGER_BARS = 10.0;
    private static final double DEFAULT_ARMOR_REPAIR_PERCENT = 40.0;

    private static final int INTRO_TICKS = 10;
    private static final int DESCENT_TICKS = 20;
    private static final int WAVE_TICKS = 14;
    private static final int REFRESH_TICKS = 5;
    private static final int BOUNDARY_TICKS = 4;

    private static final Particle.DustOptions GOLD = new Particle.DustOptions(Color.fromRGB(255, 205, 60), 1.3f);
    private static final Particle.DustOptions PALE = new Particle.DustOptions(Color.fromRGB(255, 244, 190), 1.0f);
    private static final Particle.DustOptions INTRO_WHITE = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.3f);
    private static final Particle.DustOptions WAVE_GREEN = new Particle.DustOptions(Color.fromRGB(30, 200, 70), 1.7f);
    private static final Particle.DustOptions WAVE_LIGHT_GREEN = new Particle.DustOptions(Color.fromRGB(120, 255, 90), 1.4f);
    private static final float[] CHIME_PITCHES = {0.9f, 1.1f, 1.35f, 1.6f};

    private final NamespacedKey absorptionKey;

    public SurroundSound(LeagueMechanics plugin) {
        super(plugin, "surround-sounds", "Surround Sound", PlayerClassType.SUPPORT,
                "passives.surround_sounds",
                "leaguemechanics:surround-sounds",
                "minecraft:note_block");
        this.absorptionKey = new NamespacedKey(plugin, "surround_sound_absorption");
    }

    @Override
    public double getMinCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "min-cooldown-seconds", DEFAULT_MIN_COOLDOWN_SECONDS);
    }

    @Override
    public double getCooldownSeconds() {
        return plugin.getConfig().getDouble(CFG + "cooldown-seconds", DEFAULT_COOLDOWN_SECONDS);
    }

    @Override
    public double getEffectSeconds() {
        return AbilityEffectManager.getInstance().getDurationSeconds(AbilityEffectType.SURROUND_SOUND);
    }

    private double getRadius() {
        return Math.max(1.0, plugin.getConfig().getDouble(CFG + "radius", DEFAULT_RADIUS));
    }

    private double getAbsorptionHearts() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "absorption-hearts", DEFAULT_ABSORPTION_HEARTS));
    }

    private double getHealPercent() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "heal-percent", DEFAULT_HEAL_PERCENT));
    }

    private double getHungerBars() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "hunger-restore-bars", DEFAULT_HUNGER_BARS));
    }

    private double getArmorRepairPercent() {
        return Math.max(0.0, plugin.getConfig().getDouble(CFG + "armor-repair-percent", DEFAULT_ARMOR_REPAIR_PERCENT));
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "§7You raise a §f" + trim(getRadius()) + " block§7 domain for §f" + trim(getEffectSeconds()) + "s§7 that follows you,§7",
                "§7shielding allies inside. Leaving it removes it. When it ends,§7",
                "§7allies regain §f" + trim(getHealPercent()) + "% missing health§7, §f" + trim(getHungerBars()) + " hunger§7, and repair",
                "§f" + trim(getArmorRepairPercent()) + "% armor durability§7."
        );
    }



    @Override
    public boolean cast(Player caster) {
        int ticks = Math.max(1, (int) Math.round(getEffectSeconds() * 20.0));
        new Domain(caster, SupportEffects.groundedCenter(caster), getRadius(), ticks, getAbsorptionHearts()).start();
        return true;
    }

    private void heal(List<Player> targets) {
        double fraction = getHealPercent() / 100.0;
        if (fraction <= 0.0) return;

        for (Player target : targets) {
            if (!target.isOnline() || target.isDead()) continue;
            AttributeInstance maxAttr = target.getAttribute(Attribute.MAX_HEALTH);
            if (maxAttr == null) continue;

            double max = maxAttr.getValue();
            double missing = max - target.getHealth();
            if (missing <= 0 || UndyingRage.isActive(target)) continue;

            double multiplier = PlayerStats.getOrCreate(target).getEffectiveHealingMultiplier(target);
            target.setHealth(Math.min(max, target.getHealth() + missing * fraction * multiplier));

            Location loc = target.getLocation().add(0, 1.0, 0);
            target.getWorld().spawnParticle(Particle.HEART, loc, 6, 0.4, 0.6, 0.4, 0.0);
            target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
        }
    }

    private void restoreHunger(Player player) {
        double bars = getHungerBars();
        int before = player.getFoodLevel();
        if (bars <= 0 || before >= 20) return;

        player.setFoodLevel(Math.min(20, before + (int) Math.round(bars * 2.0)));
        player.getWorld().playSound(player.getLocation().add(0, 1.0, 0), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.2f);
    }

    private void repairArmor(Player player) {
        double fraction = getArmorRepairPercent() / 100.0;
        if (fraction <= 0.0) return;

        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean repaired = false;
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType().isAir()) continue;
            if (!(piece.getItemMeta() instanceof Damageable meta) || meta.getDamage() <= 0) continue;

            int max = meta.hasMaxDamage() ? meta.getMaxDamage() : piece.getType().getMaxDurability();
            if (max <= 0) continue;

            meta.setDamage(Math.max(0, meta.getDamage() - (int) Math.ceil(max * fraction)));
            piece.setItemMeta(meta);
            repaired = true;
        }

        if (repaired) {
            player.getInventory().setArmorContents(armor);
            Location loc = player.getLocation().add(0, 1.0, 0);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 8, 0.4, 0.6, 0.4, 0.0);
            player.getWorld().playSound(loc, Sound.BLOCK_ANVIL_USE, 0.4f, 1.6f);
        }
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private final class Domain extends BukkitRunnable {
        private final Player caster;
        private Location center;
        private World world;
        private final double radius;
        private final int totalTicks;
        private final double hearts;
        private final int descentTicks;
        private final int descentStart;
        private final Set<UUID> inside = new HashSet<>();

        private int elapsed = 0;
        private int waveTick = 0;
        private boolean ended = false;

        Domain(Player caster, Location center, double radius, int totalTicks, double hearts) {
            this.caster = caster;
            this.center = center;
            this.world = center.getWorld();
            this.radius = radius;
            this.totalTicks = totalTicks;
            this.hearts = hearts;
            this.descentTicks = Math.min(DESCENT_TICKS, totalTicks);
            this.descentStart = totalTicks - descentTicks;
        }

        void start() {
            world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.4f);
            world.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f);
            refreshMembers();
            runTaskTimer(plugin, 1L, 1L);
        }

        @Override
        public void run() {
            if (ended) {
                waveTick++;
                drawWave(radius * waveTick / WAVE_TICKS, WAVE_GREEN, WAVE_LIGHT_GREEN, true);
                if (waveTick >= WAVE_TICKS) cancel();
                return;
            }

            if (!caster.isOnline()) {
                abort();
                return;
            }

            center = SupportEffects.groundedCenter(caster);
            world = center.getWorld();

            if (elapsed % REFRESH_TICKS == 0) refreshMembers();
            if (elapsed % BOUNDARY_TICKS == 0) drawBoundary();
            if (elapsed % 20 == 0) chime();
            if (elapsed < INTRO_TICKS) drawWave(radius * (elapsed + 1) / INTRO_TICKS, INTRO_WHITE, INTRO_WHITE, false);
            if (elapsed >= descentStart) drawDescent((elapsed - descentStart + 1.0) / descentTicks);

            elapsed++;
            if (elapsed >= totalTicks) impact();
        }

        private boolean inDomain(Player player) {
            if (!player.isOnline() || player.isDead() || player.getGameMode() == GameMode.SPECTATOR) return false;
            if (!player.getWorld().equals(world)) return false;
            if (!player.equals(caster) && !PartyManager.getInstance().isSameParty(caster, player)) return false;
            return player.getLocation().distanceSquared(center) <= radius * radius;
        }

        private void refreshMembers() {
            Set<UUID> present = new HashSet<>();
            int remainingTicks = Math.max(1, totalTicks - elapsed);

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!inDomain(player)) continue;
                present.add(player.getUniqueId());
                if (inside.add(player.getUniqueId())) {
                    AbilityEffectManager.getInstance().activate(player, AbilityEffectType.SURROUND_SOUND, remainingTicks / 20.0);
                    SupportEffects.grantAbsorption(player, absorptionKey, hearts);
                    Location loc = player.getLocation().add(0, 1.0, 0);
                    world.spawnParticle(Particle.NOTE, loc, 10, 0.5, 0.7, 0.5, 1.0);
                    world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.5f);
                }
            }

            for (Iterator<UUID> it = inside.iterator(); it.hasNext(); ) {
                UUID id = it.next();
                if (present.contains(id)) continue;
                it.remove();
                Player player = Bukkit.getPlayer(id);
                if (player != null) strip(player);
            }
        }

        private void strip(Player player) {
            AbilityEffectManager.getInstance().end(player, AbilityEffectType.SURROUND_SOUND);
            SupportEffects.revokeAbsorption(player, absorptionKey);
        }

        private void abort() {
            cancel();
            for (UUID id : inside) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) strip(player);
            }
            inside.clear();
        }

        private void chime() {
            float pitch = CHIME_PITCHES[(elapsed / 20) % CHIME_PITCHES.length];
            world.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, pitch);
        }

        private void drawBoundary() {
            double cx = center.getX();
            double cy = center.getY();
            double cz = center.getZ();
            double drift = elapsed * 0.04;
            boolean intro = elapsed < INTRO_TICKS;

            int ringPoints = Math.max(24, (int) Math.round(radius * 7));
            for (int i = 0; i < ringPoints; i++) {
                double a = i * 2 * Math.PI / ringPoints + drift;
                Particle.DustOptions dust = intro ? INTRO_WHITE : ((i % 2 == 0) ? GOLD : PALE);
                world.spawnParticle(Particle.DUST, cx + Math.cos(a) * radius, cy + 0.15, cz + Math.sin(a) * radius,
                        1, 0, 0, 0, 0, dust);
            }

            if (intro) return;

            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 0; i < 2; i++) {
                double a = rnd.nextDouble() * 2 * Math.PI;
                double d = Math.sqrt(rnd.nextDouble()) * radius * 0.9;
                world.spawnParticle(Particle.NOTE, cx + Math.cos(a) * d, cy + 0.4 + rnd.nextDouble() * 1.5,
                        cz + Math.sin(a) * d, 0, rnd.nextInt(24) / 24.0, 0, 0, 1.0);
            }
        }

        private void drawDescent(double progress) {
            double height = Math.max(8.0, radius * 1.8);
            int streams = Math.max(8, (int) Math.round(radius * 2));

            for (int i = 0; i < streams; i++) {
                for (int trail = 0; trail < 3; trail++) {
                    double p = progress - trail * 0.04;
                    if (p < 0) continue;
                    double fall = p * p;
                    double horizontal = radius * (1 - fall);
                    double angle = i * 2 * Math.PI / streams + p * Math.PI * 1.2;
                    double x = center.getX() + Math.cos(angle) * horizontal;
                    double y = center.getY() + height * (1 - fall);
                    double z = center.getZ() + Math.sin(angle) * horizontal;
                    if (trail == 0) {
                        world.spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0, 0, 0);
                    } else {
                        world.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, GOLD);
                    }
                }
            }

            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            int column = 1 + (int) (progress * 8);
            for (int i = 0; i < column; i++) {
                world.spawnParticle(Particle.END_ROD, center.getX(), center.getY() + rnd.nextDouble() * height,
                        center.getZ(), 1, 0.1, 0, 0.1, 0);
            }
        }

        private void impact() {
            refreshMembers();
            List<Player> healed = new ArrayList<>();
            for (UUID id : inside) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) healed.add(player);
            }
            heal(healed);
            for (Player player : healed) {
                restoreHunger(player);
                repairArmor(player);
            }
            for (Player player : healed) strip(player);
            inside.clear();
            ended = true;

            Location core = center.clone().add(0, 0.3, 0);
            world.spawnParticle(Particle.END_ROD, core, 45, 0.25, 0.25, 0.25, 0.3);
            world.spawnParticle(Particle.NOTE, core.clone().add(0, 1.0, 0), 14, 0.6, 0.6, 0.6, 1.0);
            world.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.4f);
            world.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 1.5f, 0.6f);
            world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.0f);
        }

        private void drawWave(double r, Particle.DustOptions first, Particle.DustOptions second, boolean end) {
            if (r <= 0) return;
            int points = Math.max(16, (int) Math.round(2 * Math.PI * r * 2.5));
            for (int i = 0; i < points; i++) {
                double a = i * 2 * Math.PI / points;
                double x = center.getX() + Math.cos(a) * r;
                double z = center.getZ() + Math.sin(a) * r;
                world.spawnParticle(Particle.DUST, x, center.getY() + 0.2, z, 1, 0, 0, 0, 0, (i % 2 == 0) ? first : second);
                if (i % 3 == 0) {
                    if (end) {
                        world.spawnParticle(Particle.HAPPY_VILLAGER, x, center.getY() + 0.3, z, 1, 0, 0, 0, 0);
                    } else {
                        world.spawnParticle(Particle.END_ROD, x, center.getY() + 0.25, z, 0, 0, 1, 0, 0.06);
                    }
                }
            }
        }
    }
}