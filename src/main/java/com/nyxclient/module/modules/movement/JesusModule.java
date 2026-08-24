package com.nyxclient.module.modules.movement;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

public class JesusModule extends Module {

	public JesusModule() {
		super("Jesus", "Keeps you on top of water instead of sinking into it, as if it were solid ground. Sneak to sink through on purpose. Only takes effect where the server accepts client-reported position/velocity.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		ClientWorld world = client.world;
		if (player == null || world == null) return;
		if (player.isOnGround() || player.isSneaking()) return; // real ground wins; sneaking lets you dive through

		BlockPos feetPos = BlockPos.ofFloored(player.getX(), player.getY(), player.getZ());
		FluidState fluidState = world.getFluidState(feetPos);
		if (!fluidState.isIn(FluidTags.WATER)) return;

		double surfaceY = feetPos.getY() + 1.0;
		if (player.getY() < surfaceY) {
			player.setPosition(player.getX(), surfaceY, player.getZ());
		}
		if (player.getVelocity().y < 0) {
			player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z);
		}
		player.fallDistance = 0;
	}
}
