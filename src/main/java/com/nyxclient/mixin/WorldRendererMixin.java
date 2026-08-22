package com.nyxclient.mixin;

import com.nyxclient.module.modules.render.XrayModule;
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
 * brightness. Xray only skips *drawing* neighboring stone - it never changes actual light levels -
 * so an ore rendered unculled deep inside solid rock still gets the near-zero light of a fully
 * enclosed block and comes out essentially black, indistinguishable from nothing rendering at all.
 * Forcing full brightness for ore blocks while Xray is active is what actually makes them visible.
 */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

	private static final int FULL_BRIGHT = 0xF000F0;

	@Inject(method = "getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I", at = @At("RETURN"), cancellable = true)
	private static void nyxclient$xrayFullBright(BlockRenderView world, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (XrayModule.active && XrayModule.shouldRender(state)) {
			cir.setReturnValue(FULL_BRIGHT);
		}
	}
}
