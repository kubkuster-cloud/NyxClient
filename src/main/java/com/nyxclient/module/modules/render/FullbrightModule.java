package com.nyxclient.module.modules.render;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import net.minecraft.client.MinecraftClient;

public class FullbrightModule extends Module {

	// Read by WorldRendererMixin during chunk mesh building and lighting lookups. Volatile: written
	// from the client tick thread, read from the render thread.
	public static volatile boolean active = false;

	public FullbrightModule() {
		super("Fullbright", "Forces every block to render at full brightness, so caves and night look as bright as day. Existing chunks are reloaded on toggle; this only affects your local render, not the actual world.", Category.RENDER);
	}

	@Override
	protected void onEnable() {
		active = true;
		reloadWorldRenderer();
	}

	@Override
	protected void onDisable() {
		active = false;
		reloadWorldRenderer();
	}

	private void reloadWorldRenderer() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.worldRenderer != null) {
			client.worldRenderer.reload();
		}
	}
}
