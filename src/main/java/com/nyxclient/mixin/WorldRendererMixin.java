package com.nyxclient.mixin;

import com.nyxclient.module.modules.render.FullbrightModule;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * getLightmapCoordinates() reads the real world light at a block's position to decide its rendered
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
}
