package com.nyxclient.mixin;

import com.nyxclient.module.modules.render.FreecamModule;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blanked after the vanilla tick rather than instead of it, so the keys are still read - the
 * ClickGUI's bind polling and FreecamModule both go on seeing them - while the player they would
 * normally move is left standing still.
 */
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private void nyxclient$freezePlayerForFreecam(CallbackInfo ci) {
		if (!FreecamModule.active) return;

		Input self = (Input) (Object) this;
		self.playerInput = PlayerInput.DEFAULT;
		self.movementForward = 0.0F;
		self.movementSideways = 0.0F;
	}
}
