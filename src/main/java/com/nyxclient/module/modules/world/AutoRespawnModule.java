package com.nyxclient.module.modules.world;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import com.nyxclient.setting.IntSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoRespawnModule extends Module {

	private final IntSetting delayTicks = addSetting(new IntSetting("Delay", 10, 0, 100));

	private int waited;

	public AutoRespawnModule() {
		super("AutoRespawn", "Respawns you automatically instead of leaving you on the death screen.", Category.WORLD);
	}

	@Override
	protected void onEnable() {
		waited = 0;
	}

	@Override
	public void onTick() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) return;

		if (!(client.currentScreen instanceof DeathScreen)) {
			waited = 0;
			return;
		}

		// A short delay leaves the death message readable, and avoids firing a respawn request before
		// the server has finished setting up the death screen it just sent.
		if (waited++ < delayTicks.get()) return;

		waited = 0;
		client.player.requestRespawn();
		client.setScreen(null);
	}
}
