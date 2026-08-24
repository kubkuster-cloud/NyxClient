package com.nyxclient.module.modules.world;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class AutoToolModule extends Module {

	public AutoToolModule() {
		super("AutoTool", "Switches your hotbar to the fastest suitable tool for whatever block you're currently mining.", Category.WORLD);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null || client.options == null) return;
		if (!client.options.attackKey.isPressed()) return;
		if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

		BlockState state = client.world.getBlockState(hit.getBlockPos());

		int bestSlot = -1;
		float bestSpeed = -1f;
		for (int i = 0; i < 9; i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!stack.isSuitableFor(state)) continue;

			float speed = stack.getMiningSpeedMultiplier(state);
			if (speed > bestSpeed) {
				bestSpeed = speed;
				bestSlot = i;
			}
		}

		if (bestSlot >= 0 && bestSlot != player.getInventory().selectedSlot) {
			player.getInventory().setSelectedSlot(bestSlot);
		}
	}
}
