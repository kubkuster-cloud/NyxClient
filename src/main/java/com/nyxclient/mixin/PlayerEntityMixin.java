package com.nyxclient.mixin;

import com.nyxclient.module.modules.world.FastbreakModule;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * getBlockBreakingSpeed() drives both the client's crack-overlay prediction AND (via
 * calcBlockBreakingDelta) the integrated server's own authoritative mining-progress timer in
 * ServerPlayerInteractionManager#continueMining - that timer reads its own ServerPlayerEntity
 * instance, a different object from the client's ClientPlayerEntity, so overriding only the
 * client-side instance never affected how fast blocks actually finished breaking. In singleplayer
 * the integrated server runs in this same JVM, so matching by UUID (instead of requiring
 * ClientPlayerEntity) reaches that ServerPlayerEntity too. Against a real remote server this still
 * can't do anything, since that ServerPlayerEntity never exists in our process at all.
 */
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

	@Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
	private void nyxclient$fastbreak(BlockState block, CallbackInfoReturnable<Float> cir) {
		if (!FastbreakModule.active) return;

		PlayerEntity self = (PlayerEntity) (Object) this;
		ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
		if (localPlayer != null && self.getUuid().equals(localPlayer.getUuid())) {
			cir.setReturnValue(1000.0f);
		}
	}
}
