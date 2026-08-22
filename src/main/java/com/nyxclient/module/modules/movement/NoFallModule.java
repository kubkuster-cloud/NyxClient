package com.nyxclient.module.modules.movement;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class NoFallModule extends Module {

	// Read by ClientPlayerEntityMixin, which is where the actual fall-damage suppression happens:
	// the server computes fall damage from onGround/Y-deltas in the movement packets, not from
	// this client-side fallDistance field, so that's what actually needs spoofing.
	public static volatile boolean active = false;

	public NoFallModule() {
		super("NoFall", "Spoofs onGround=true in outgoing movement packets so the server's own fall-distance tracker never accumulates damage. Only works against servers/anti-cheat that don't cross-check reported ground state against real position.", Category.MOVEMENT);
	}

	@Override
	protected void onEnable() {
		active = true;
	}

	@Override
	protected void onDisable() {
		active = false;
	}

	@Override
	public void onTick() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;
		player.fallDistance = 0.0f;
	}
}
