package com.nyxclient.module.modules.movement;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import com.nyxclient.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class SpeedModule extends Module {

	private final DoubleSetting multiplier = addSetting(new DoubleSetting("Multiplier", 1.3, 1.0, 3.0));

	public SpeedModule() {
		super("Speed", "Multiplies horizontal movement speed while walking/sprinting on the ground.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;
		if (!player.isOnGround() || player.isSneaking()) return;

		double forward = player.input.movementForward;
		double sideways = player.input.movementSideways;
		if (forward == 0 && sideways == 0) return;

		float yaw = player.getYaw();
		double yawRad = Math.toRadians(yaw);

		double sin = Math.sin(yawRad);
		double cos = Math.cos(yawRad);

		double moveX = sideways * cos - forward * sin;
		double moveZ = forward * cos + sideways * sin;

		double length = Math.sqrt(moveX * moveX + moveZ * moveZ);
		if (length < 1.0e-4) return;

		double factor = (multiplier.get() - 1.0) * 0.2;
		Vec3d velocity = player.getVelocity();
		player.setVelocity(
				velocity.x + (moveX / length) * factor,
				velocity.y,
				velocity.z + (moveZ / length) * factor
		);
	}
}
