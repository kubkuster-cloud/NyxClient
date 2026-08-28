package com.nyxclient.mixin;

import com.nyxclient.module.modules.render.FreecamModule;
import com.nyxclient.module.modules.render.FullbrightModule;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Two unrelated hooks that happen to share a target class.
 *
 * <p>getLightmapCoordinates() reads the real world light at a block's position to decide its rendered
 * brightness - both the flat lighting path and the smooth/ambient-occlusion path funnel every light
 * sample they take (a block's own light, and each neighbor sampled while blending AO) through this
 * exact overload, so overriding it here is enough to fully brighten a block regardless of which path
 * rendered it.
 */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

	private static final int FULL_BRIGHT = 0xF000F0;

	@Inject(method = "getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I", at = @At("RETURN"), cancellable = true)
	private static void nyxclient$fullbright(BlockRenderView world, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (FullbrightModule.active) {
			cir.setReturnValue(FULL_BRIGHT);
		}
	}

	/**
	 * Vanilla drops your own player from the render list whenever it isn't what the camera is looking
	 * through - the rule that keeps your body out of your own eyes in first person. Freecam trips it:
	 * the camera looks through a marker, so the body it just flew away from is culled and there is
	 * nothing left standing where you were. Added back at the end, where it cannot duplicate an entry
	 * that same rule guarantees was never made.
	 */
	@Inject(method = "getEntitiesToRender", at = @At("TAIL"))
	private void nyxclient$keepPlayerVisibleDuringFreecam(Camera camera, Frustum frustum, List<Entity> output, CallbackInfoReturnable<Boolean> cir) {
		if (!FreecamModule.active) return;

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player != null) {
			output.add(player);
		}
	}
}
