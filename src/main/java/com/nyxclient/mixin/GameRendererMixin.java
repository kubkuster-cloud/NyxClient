package com.nyxclient.mixin;

import com.nyxclient.module.modules.render.NoHurtCamModule;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * tiltViewWhenHurt() rotates the view by an amount derived from the damage-tilt timer, right before the
 * world is drawn. Cancelling the call leaves that rotation off the matrix stack entirely, so the camera
 * stays level - the timer itself keeps ticking untouched, which is what lets the effect come straight
 * back the moment the module is switched off.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
	private void nyxclient$noHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (NoHurtCamModule.active) {
			ci.cancel();
		}
	}
}
