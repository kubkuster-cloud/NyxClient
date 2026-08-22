package com.nyxclient.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * setGlowing(boolean) writes the local field, then re-reads isGlowing() to decide what to push into
 * the networked glow flag that rendering actually checks (MinecraftClient#hasOutline). For any
 * LivingEntity, isGlowing() is overridden to ignore that local field on the client and instead
 * return the flag's *current* (pre-write) value, so the write is silently a no-op for every entity
 * except the vanilla cases (status effects, server-side) that don't go through this setter on the
 * client. Redirecting to isGlowingLocal() makes the setter use the value it was just given.
 */
@Mixin(Entity.class)
public class EntityMixin {

	@Redirect(method = "setGlowing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isGlowing()Z"))
	private boolean nyxclient$useJustSetLocalGlowState(Entity self) {
		return self.isGlowingLocal();
	}
}
