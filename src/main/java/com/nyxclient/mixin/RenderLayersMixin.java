package com.nyxclient.mixin;

import com.nyxclient.module.modules.render.XrayModule;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chunk mesh building buckets each block's geometry into a RenderLayer-specific buffer before
 * BlockRenderManager#renderBlock ever runs. Only the translucent layer's buffer is drawn with GL blending
 * enabled - if a non-ore block were left on its normal (solid/cutout) layer, the reduced alpha
 * BlockRenderManagerMixin writes to its vertices would be ignored and it would still render fully opaque.
 */
@Mixin(RenderLayers.class)
public class RenderLayersMixin {

	@Inject(method = "getBlockLayer", at = @At("RETURN"), cancellable = true)
	private static void nyxclient$xrayTranslucent(BlockState state, CallbackInfoReturnable<RenderLayer> cir) {
		if (XrayModule.active && !XrayModule.shouldRender(state)) {
			cir.setReturnValue(RenderLayer.getTranslucent());
		}
	}
}
