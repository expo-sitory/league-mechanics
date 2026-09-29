package dev.ixpu.leaguemechanics.listener;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.skills.Dash;
import dev.ixpu.leaguemechanics.manager.CombatStateManager;
import org.bukkit.GameMode;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DashListener implements Listener {

    public static final Dash dashCooldown = new Dash();

    private static final double DASH_STRENGTH = 0.5;
    private static final double DASH_VERTICAL_BOOST = 0.15;
    private static final long DASH_FALL_IMMUNITY_MS = 3000L;

    private final LeagueMechanics plugin;
    private final CombatStateManager combatState = CombatStateManager.getInstance();

    private final Map<UUID, Boolean> wasJumping = new ConcurrentHashMap<>();

    public DashListener(LeagueMechanics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        Input input = event.getInput();
        UUID uuid = player.getUniqueId();

        boolean jumpHeldBefore = wasJumping.getOrDefault(uuid, false);
        wasJumping.put(uuid, input.isJump());

        if (!input.isJump() || jumpHeldBefore) {
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (player.isOnGround() || player.isFlying() || player.isGliding()) {
            return;
        }
        if (dashCooldown.isOnCooldown(player)) {
            return;
        }

        dashCooldown.startCooldown(player);

        Vector direction = resolveDashDirection(player, input);
        Vector velocity = direction.multiply(DASH_STRENGTH);
        velocity.setY(DASH_VERTICAL_BOOST);
        player.setVelocity(velocity);

        combatState.markDashFallImmunity(uuid, DASH_FALL_IMMUNITY_MS);

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.6f);
        player.getWorld().spawnParticle(Particle.CLOUD, loc, 12, 0.3, 0.1, 0.3, 0.02);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        wasJumping.remove(event.getPlayer().getUniqueId());
        dashCooldown.cleanup(event.getPlayer());
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
            direction = new Vector(forwardX, 0, forwardZ);
        }
        return direction.normalize();
    }
}