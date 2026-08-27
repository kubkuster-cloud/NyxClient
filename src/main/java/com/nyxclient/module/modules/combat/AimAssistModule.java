package com.nyxclient.module.modules.combat;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import com.nyxclient.setting.BoolSetting;
import com.nyxclient.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Nudges the view toward a target instead of snapping to it the way Killaura's "Face target" does: each
 * tick it closes a fraction of the remaining angle, capped at a fixed degrees-per-tick budget, so mouse
 * input stays in control and the camera keeps moving on its own axis. The target is picked by smallest
 * angle to the current view (not smallest distance), which is what keeps it locked on whoever is already
 * roughly under the crosshair rather than yanking to a closer entity off to the side.
 */
public class AimAssistModule extends Module {

	private final DoubleSetting range = addSetting(new DoubleSetting("Range", 4.5, 2.0, 8.0));
	private final DoubleSetting fov = addSetting(new DoubleSetting("FOV", 90.0, 10.0, 180.0));
	private final DoubleSetting strength = addSetting(new DoubleSetting("Strength", 0.35, 0.05, 1.0));
	private final DoubleSetting maxTurn = addSetting(new DoubleSetting("Max turn", 25.0, 1.0, 90.0));
	private final BoolSetting assistPitch = addSetting(new BoolSetting("Vertical assist", true));
	private final BoolSetting onlyWhileAttacking = addSetting(new BoolSetting("Only while attacking", true));
	private final BoolSetting requireVisible = addSetting(new BoolSetting("Require line of sight", true));
	private final BoolSetting targetPlayers = addSetting(new BoolSetting("Target players", true));
	private final BoolSetting targetHostiles = addSetting(new BoolSetting("Target hostile mobs", true));
	private final BoolSetting targetPassives = addSetting(new BoolSetting("Target passive mobs", false));

	public AimAssistModule() {
		super("AimAssist", "Smoothly steers the view toward the entity nearest your crosshair. Intended for singleplayer/private servers you have permission to play on.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) return;

		if (onlyWhileAttacking.get() && !client.options.attackKey.isPressed()) return;

		LivingEntity target = findTarget(client, player);
		if (target == null) return;

		float desiredYaw = yawTo(player, target);
		float desiredPitch = pitchTo(player, target);

		player.setYaw(player.getYaw() + step(MathHelper.wrapDegrees(desiredYaw - player.getYaw())));

		if (assistPitch.get()) {
			float pitch = player.getPitch() + step(desiredPitch - player.getPitch());
			player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
		}
	}

	/** Fraction of the remaining angle to close this tick, capped at the per-tick turn budget. */
	private float step(float delta) {
		float scaled = (float) (delta * strength.get());
		float cap = maxTurn.get().floatValue();
		return MathHelper.clamp(scaled, -cap, cap);
	}

	private LivingEntity findTarget(MinecraftClient client, ClientPlayerEntity player) {
		double r = range.get();
		double maxAngle = fov.get() / 2.0;

		LivingEntity best = null;
		double bestAngle = maxAngle;

		for (Entity entity : client.world.getEntities()) {
			if (!(entity instanceof LivingEntity living)) continue;
			if (living == player || !living.isAlive() || living.isRemoved()) continue;
			if (player.squaredDistanceTo(living) > r * r) continue;

			if (living instanceof PlayerEntity) {
				if (!targetPlayers.get()) continue;
			} else if (living instanceof HostileEntity) {
				if (!targetHostiles.get()) continue;
			} else {
				if (!targetPassives.get()) continue;
			}

			if (requireVisible.get() && !player.canSee(living)) continue;

			double angle = angleTo(player, living);
			if (angle < bestAngle) {
				bestAngle = angle;
				best = living;
			}
		}

		return best;
	}

	/** Angular distance between the current view direction and the direction to the entity, in degrees. */
	private double angleTo(ClientPlayerEntity player, Entity target) {
		double yawDiff = MathHelper.wrapDegrees(yawTo(player, target) - player.getYaw());
		double pitchDiff = pitchTo(player, target) - player.getPitch();
		return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
	}

	private float yawTo(ClientPlayerEntity player, Entity target) {
		double dx = target.getX() - player.getX();
		double dz = target.getZ() - player.getZ();
		return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
	}

	private float pitchTo(ClientPlayerEntity player, Entity target) {
		double dx = target.getX() - player.getX();
		double dy = target.getBodyY(0.5) - player.getEyeY();
		double dz = target.getZ() - player.getZ();
		double horizontalDist = Math.sqrt(dx * dx + dz * dz);
		return (float) -Math.toDegrees(Math.atan2(dy, horizontalDist));
	}
}
