package com.nyxclient.module.modules.render;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class XrayModule extends Module {

	// Read by BlockRenderManagerMixin during chunk mesh building. Volatile: written from the
	// client tick thread, read from the render thread during chunk (re)build.
	public static volatile boolean active = false;

	public XrayModule() {
		super("Xray", "Makes ores render fully opaque and lit while every other block renders translucent, so ores stand out through stone. Existing chunks are reloaded on toggle; this only affects your local render, not the actual world.", Category.RENDER);
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

	public static boolean shouldRender(BlockState state) {
		Identifier id = Registries.BLOCK.getId(state.getBlock());
		String path = id.getPath();
		return path.contains("_ore") || path.equals("ancient_debris");
	}
}
