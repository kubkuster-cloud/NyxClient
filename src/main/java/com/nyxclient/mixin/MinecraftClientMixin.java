package com.nyxclient.mixin;

import com.nyxclient.module.modules.render.FullbrightModule;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ambient occlusion (smooth lighting) shades each vertex from a blend of neighboring cells' light plus a
 * separate "how enclosed is this corner" darkening factor, rather than the rendered block's own position -
 * WorldRendererMixin's override doesn't reliably reach every one of those samples, which is why only
 * blocks on the flat lighting path (grass, other non-full-cube models) were actually turning full bright.
 * Disabling AO entirely while Fullbright is active routes every block through that same flat path, whose
 * single per-face lightmap lookup is exactly what WorldRendererMixin overrides.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	@Inject(method = "isAmbientOcclusionEnabled", at = @At("RETURN"), cancellable = true)
	private static void nyxclient$disableAoForFullbright(CallbackInfoReturnable<Boolean> cir) {
		if (FullbrightModule.active) {
			cir.setReturnValue(false);
		}
	}
}
