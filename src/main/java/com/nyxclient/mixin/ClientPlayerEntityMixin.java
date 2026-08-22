package com.nyxclient.mixin;

import com.nyxclient.module.modules.movement.NoFallModule;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * sendMovementPackets() builds every outgoing PlayerMoveC2SPacket variant from this.isOnGround() at
 * call time. The server derives fall damage from the onGround flag and Y-deltas across these packets,
 * not from any client-reported fallDistance field, so this is the only place spoofing it has effect.
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

	@Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isOnGround()Z"))
	private boolean nyxclient$spoofOnGround(ClientPlayerEntity self) {
		return NoFallModule.active || self.isOnGround();
	}
}
