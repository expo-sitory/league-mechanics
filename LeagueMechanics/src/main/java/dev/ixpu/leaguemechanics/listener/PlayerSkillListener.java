package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.skills.Dash;
import dev.ixpu.leaguemechanics.entity.player.skills.DoubleJump;
import dev.ixpu.leaguemechanics.manager.CombatStateManager;
import org.bukkit.GameMode;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSkillListener implements Listener {

    public static final Dash dashCooldown = new Dash();
    public static final DoubleJump doubleJumpCooldown = new DoubleJump();

    private static final NamespacedKey DASH_DISABLED_KEY = new NamespacedKey("leaguemechanics", "dash_disabled");

    public static boolean isDashEnabled(Player player) {
        return !player.getPersistentDataContainer().has(DASH_DISABLED_KEY, PersistentDataType.BYTE);
    }

    public static void setDashEnabled(Player player, boolean enabled) {
        if (enabled) {
            player.getPersistentDataContainer().remove(DASH_DISABLED_KEY);
        } else {
            player.getPersistentDataContainer().set(DASH_DISABLED_KEY, PersistentDataType.BYTE, (byte) 1);
        }
    }

    private static final double DASH_STRENGTH = 0.7;
    private static final double DASH_VERTICAL_BOOST = 0.15;
    private static final long DASH_FALL_IMMUNITY_MS = 2000L;

    private static final float LOOK_UP_PITCH = -60.0f;
    private static final double DOUBLE_JUMP_STRENGTH = 0.8;
    private static final long DOUBLE_JUMP_FALL_IMMUNITY_MS = 2000L;

    private static final int HUNGER_COST = 2;

    private final LeagueMechanics plugin;
    private final CombatStateManager combatState = CombatStateManager.getInstance();

    private final Map<UUID, Boolean> wasSneaking = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> wasJumping = new ConcurrentHashMap<>();

    public PlayerSkillListener(LeagueMechanics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        Input input = event.getInput();
        UUID uuid = player.getUniqueId();

        boolean sneakHeldBefore = wasSneaking.getOrDefault(uuid, false);
        boolean jumpHeldBefore = wasJumping.getOrDefault(uuid, false);
        wasSneaking.put(uuid, input.isSneak());
        wasJumping.put(uuid, input.isJump());

        boolean sneakPressed = input.isSneak() && !sneakHeldBefore;
        boolean jumpPressed = input.isJump() && !jumpHeldBefore;

        if (!sneakPressed && !jumpPressed) {
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (player.isOnGround() || player.isFlying() || player.isGliding()) {
            return;
        }

        if (jumpPressed && player.getLocation().getPitch() <= LOOK_UP_PITCH) {
            performDoubleJump(player);
        }

        if (!sneakPressed) {
            return;
        }
        if (!isDashEnabled(player)) {
            return;
        }

        Vector direction = resolveDashDirection(player, input);
        if (direction == null) {
            return;
        }

        if (dashCooldown.isOnCooldown(player)) {
            return;
        }
        if (player.getFoodLevel() < HUNGER_COST) {
            return;
        }

        dashCooldown.startCooldown(player);
        player.setFoodLevel(player.getFoodLevel() - HUNGER_COST);

        Vector velocity = direction.multiply(DASH_STRENGTH);
        velocity.setY(DASH_VERTICAL_BOOST);
        player.setVelocity(velocity);

        combatState.markDashFallImmunity(uuid, DASH_FALL_IMMUNITY_MS);

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.6f);
        player.getWorld().spawnParticle(Particle.CLOUD, loc, 12, 0.3, 0.1, 0.3, 0.02);
    }

    private void performDoubleJump(Player player) {
        if (doubleJumpCooldown.isOnCooldown(player)) {
            return;
        }
        if (player.getFoodLevel() < HUNGER_COST) {
            return;
        }

        doubleJumpCooldown.startCooldown(player);
        player.setFoodLevel(player.getFoodLevel() - HUNGER_COST);

        Vector velocity = player.getVelocity();
        velocity.setY(DOUBLE_JUMP_STRENGTH);
        player.setVelocity(velocity);

        combatState.markDashFallImmunity(player.getUniqueId(), DOUBLE_JUMP_FALL_IMMUNITY_MS);

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.9f);
        player.getWorld().spawnParticle(Particle.CLOUD, loc, 16, 0.3, 0.1, 0.3, 0.03);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        wasSneaking.remove(event.getPlayer().getUniqueId());
        wasJumping.remove(event.getPlayer().getUniqueId());
        dashCooldown.cleanup(event.getPlayer());
        doubleJumpCooldown.cleanup(event.getPlayer());
    }

    private Vector resolveDashDirection(Player player, Input input) {
        double yawRad = Math.toRadians(player.getLocation().getYaw());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = -forwardZ;
        double rightZ = forwardX;

        double moveX = 0.0;
        double moveZ = 0.0;
        if (input.isForward()) {
            moveX += forwardX;
            moveZ += forwardZ;
        }
        if (input.isBackward()) {
            moveX -= forwardX;
            moveZ -= forwardZ;
        }
        if (input.isRight()) {
            moveX += rightX;
            moveZ += rightZ;
        }
        if (input.isLeft()) {
            moveX -= rightX;
            moveZ -= rightZ;
        }

        Vector direction = new Vector(moveX, 0, moveZ);
        if (direction.lengthSquared() < 0.0001) {
            return null;
        }
        return direction.normalize();
    }
}