package com.nyxclient.mixin;

import com.nyxclient.module.modules.combat.AntiCheatModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A position-look packet is how the server tells the client "you are not where you claimed to be" -
 * it is the visible half of every movement check that trips.
 *
 * <p>Injected at TAIL rather than HEAD on purpose: the vanilla method opens with a
 * NetworkThreadUtils.forceMainThread call that aborts and reschedules when the packet arrives on the
 * netty thread, so only code after that point is guaranteed to run on the client thread. Everything
 * AntiCheatModule does in response (reading module state, toggling modules, sending a chat message)
 * needs to be there.
 *
 * <p>Packets carrying relative flags are skipped: their position field is a delta against the
 * player's current position rather than a destination, so it cannot be compared against the last
 * known position without resolving each axis flag first, and setbacks are sent absolute anyway.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

	@Inject(method = "onPlayerPositionLook", at = @At("TAIL"))
	private void nyxclient$detectSetback(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
		if (!packet.relatives().isEmpty()) return;
		AntiCheatModule.notifySetback(packet.change().position());
	}
}
