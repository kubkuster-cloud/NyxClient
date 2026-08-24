package com.nyxclient.module.modules.render;

import com.nyxclient.module.Category;
import com.nyxclient.module.Module;

public class NoHurtCamModule extends Module {

	// Read by GameRendererMixin on the render thread; written from the client tick thread.
	public static volatile boolean active = false;

	public NoHurtCamModule() {
		super("NoHurtCam", "Removes the camera tilt that plays when you take damage, so your aim isn't thrown off mid-fight.", Category.RENDER);
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
