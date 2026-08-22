package com.nyxclient;

import com.nyxclient.config.ConfigManager;
import com.nyxclient.gui.ClickGuiScreen;
import com.nyxclient.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NyxClient implements ClientModInitializer {

	public static final String MOD_ID = "nyxclient";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ModuleManager moduleManager = new ModuleManager();

	private static KeyBinding openGuiKey;

	@Override
	public void onInitializeClient() {
		moduleManager.registerAll();

		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.nyxclient.open_gui",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				"category.nyxclient.main"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			moduleManager.tick();

			while (openGuiKey.wasPressed()) {
				if (client.currentScreen == null) {
					client.setScreen(new ClickGuiScreen());
				}
			}
		});

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> ConfigManager.load());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

		LOGGER.info("Nyx Client initialized");
	}
}
