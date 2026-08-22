package com.nyxclient.module.modules.world;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;

public class FastbreakModule extends Module {

	// Read by PlayerEntityMixin. See the mixin for why this only ever applies to the local player.
	public static volatile boolean active = false;

	public FastbreakModule() {
		super("Fastbreak", "Overrides your calculated mining speed to break blocks instantly. The server still resolves breaking at its own authoritative rate unless it trusts client-reported timing.", Category.WORLD);
	}

	@Override
	protected void onEnable() {
		active = true;
	}

	@Override
	protected void onDisable() {
		active = false;
	}
}
