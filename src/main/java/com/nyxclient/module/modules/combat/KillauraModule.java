package com.nyxclient.module.modules.combat;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import com.nyxclient.setting.BoolSetting;
import com.nyxclient.setting.DoubleSetting;
import com.nyxclient.setting.IntSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

public class KillauraModule extends Module {

	private final DoubleSetting range = addSetting(new DoubleSetting("Range", 4.0, 2.0, 6.0));
	private final IntSetting cooldownTicks = addSetting(new IntSetting("Cooldown", 10, 1, 40));
	private final BoolSetting attackPlayers = addSetting(new BoolSetting("Attack players", true));
	private final BoolSetting attackHostiles = addSetting(new BoolSetting("Attack hostile mobs", true));
	private final BoolSetting attackPassives = addSetting(new BoolSetting("Attack passive mobs", false));
	private final BoolSetting rotate = addSetting(new BoolSetting("Face target", true));

	private int cooldown = 0;

	public KillauraModule() {
		super("Killaura", "Automatically attacks the nearest valid entity in range. Intended for singleplayer/private servers you have permission to play on.", Category.COMBAT);
	}

	@Override
	protected void onEnable() {
		cooldown = 0;
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null || client.interactionManager == null) return;

		if (cooldown > 0) {
			cooldown--;
			return;
		}

		LivingEntity target = findTarget(client, player);
		if (target == null) return;

		if (rotate.get()) {
			faceEntity(player, target);
		}

		client.interactionManager.attackEntity(player, target);
		player.swingHand(Hand.MAIN_HAND);
		cooldown = cooldownTicks.get();
	}

	private LivingEntity findTarget(MinecraftClient client, ClientPlayerEntity player) {
		double r = range.get();
		Box searchBox = player.getBoundingBox().expand(r);

		LivingEntity closest = null;
		double closestDistSq = r * r;

		for (Entity entity : client.world.getEntities()) {
			if (!(entity instanceof LivingEntity living)) continue;
			if (living == player || !living.isAlive() || living.isRemoved()) continue;
			if (!searchBox.intersects(living.getBoundingBox())) continue;

			if (living instanceof PlayerEntity) {
				if (!attackPlayers.get()) continue;
			} else if (living instanceof HostileEntity) {
				if (!attackHostiles.get()) continue;
			} else {
				if (!attackPassives.get()) continue;
			}

			double distSq = player.squaredDistanceTo(living);
			if (distSq < closestDistSq) {
				closestDistSq = distSq;
				closest = living;
			}
		}

		return closest;
	}

	private void faceEntity(ClientPlayerEntity player, Entity target) {
		double dx = target.getX() - player.getX();
		double dy = (target.getBodyY(0.5)) - player.getEyeY();
		double dz = target.getZ() - player.getZ();

		double horizontalDist = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDist));

		player.setYaw(yaw);
		player.setPitch(pitch);
	}
}
