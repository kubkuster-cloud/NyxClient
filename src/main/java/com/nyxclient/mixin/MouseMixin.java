package com.nyxclient.mixin;

import com.nyxclient.module.modules.render.FreecamModule;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Freecam has to steal the mouse before it reaches the player, not undo the rotation afterwards:
 * the player's yaw and pitch are sent to the server from the same tick that applies them, so a
 * player rotated first and straightened out later is a player the server watched spin.
 */
@Mixin(Mouse.class)
public class MouseMixin {

	@Redirect(
			method = "updateMouse",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
			)
	)
	private void nyxclient$steerFreecam(ClientPlayerEntity player, double cursorDeltaX, double cursorDeltaY) {
		if (FreecamModule.active) {
			FreecamModule.steer(cursorDeltaX, cursorDeltaY);
		} else {
			player.changeLookDirection(cursorDeltaX, cursorDeltaY);
		}
	}
}
