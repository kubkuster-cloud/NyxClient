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

	// DOWN first: prefer re-placing straight down when ground is already there. The horizontal
	// directions are the fallback that actually bridges a gap, by clicking the side of whichever
	// neighboring block is still solid (typically the block you're stepping off of).
	private static final Direction[] CANDIDATE_DIRECTIONS = {
			Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
	};

	public ScaffoldModule() {
		super("Scaffold", "Places blocks beneath you while walking, bridging over gaps. Requires placeable blocks in your hotbar.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null || client.interactionManager == null) return;
		if (!player.isOnGround() && player.getVelocity().y > 0.1) return; // don't spam-place while jumping upward

		BlockPos feetPos = BlockPos.ofFloored(player.getX(), player.getY() - 0.1, player.getZ());
		if (!client.world.getBlockState(feetPos).isAir()) return; // already standing on something solid

		BlockHitResult hitResult = findPlacementTarget(client, feetPos);
		if (hitResult == null) return; // no solid neighbor to place against

		int slot = findBlockSlot(player);
		if (slot < 0) return;

		int previousSlot = player.getInventory().selectedSlot;
		player.getInventory().setSelectedSlot(slot);

		client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);
		player.swingHand(Hand.MAIN_HAND);

		player.getInventory().setSelectedSlot(previousSlot);
	}

	// Clicking straight down only works once ground already exists there - that's not a gap, there's
	// nothing to bridge. To actually fill a hole, find whichever neighbor of the empty space is still
	// solid and click the face of *that* block facing the hole, so the new block lands at feetPos
	// regardless of what's (not) underneath it.
	private BlockHitResult findPlacementTarget(MinecraftClient client, BlockPos feetPos) {
		for (Direction direction : CANDIDATE_DIRECTIONS) {
			BlockPos neighbor = feetPos.offset(direction);
			if (client.world.getBlockState(neighbor).isAir()) continue;

			Direction clickedFace = direction.getOpposite();
			Vec3d hitPos = Vec3d.ofCenter(neighbor).add(Vec3d.of(clickedFace.getVector()).multiply(0.5));
			return new BlockHitResult(hitPos, clickedFace, neighbor, false);
		}
		return null;
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
