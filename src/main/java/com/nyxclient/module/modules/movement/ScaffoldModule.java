package com.nyxclient.module.modules.movement;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ScaffoldModule extends Module {

	public ScaffoldModule() {
		super("Scaffold", "Places blocks beneath you while walking, bridging over gaps. Requires placeable blocks in your hotbar.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null || client.interactionManager == null) return;
		if (player.isOnGround() == false && player.getVelocity().y > 0.1) return; // don't spam-place while jumping upward

		BlockPos feetPos = BlockPos.ofFloored(player.getX(), player.getY() - 0.1, player.getZ());
		if (!client.world.getBlockState(feetPos).isAir()) return;

		BlockPos foundationPos = feetPos.down();
		if (client.world.getBlockState(foundationPos).isAir()) return; // over a hole deeper than 1, skip

		int slot = findBlockSlot(player);
		if (slot < 0) return;

		int previousSlot = player.getInventory().selectedSlot;
		player.getInventory().setSelectedSlot(slot);

		Vec3d hitPos = new Vec3d(foundationPos.getX() + 0.5, foundationPos.getY() + 1.0, foundationPos.getZ() + 0.5);
		BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.UP, foundationPos, false);

		client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);
		player.swingHand(Hand.MAIN_HAND);

		player.getInventory().setSelectedSlot(previousSlot);
	}

	private int findBlockSlot(ClientPlayerEntity player) {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
				return i;
			}
		}
		return -1;
	}
}
