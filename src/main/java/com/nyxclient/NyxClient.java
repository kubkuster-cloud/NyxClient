package com.nyxclient;

import com.nyxclient.config.ConfigManager;
import com.nyxclient.gui.ClickGuiScreen;
import com.nyxclient.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NyxClient implements ClientModInitializer {

	public static final String MOD_ID = "nyxclient";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ModuleManager moduleManager = new ModuleManager();

	private static final int GUI_KEY_DEFAULT = GLFW.GLFW_KEY_P;

	// The default this keybinding shipped with before GUI_KEY_DEFAULT moved to P. Used only for
	// configs written before the default was recorded at all - see migrateGuiKeyDefault.
	private static final int LEGACY_GUI_KEY_DEFAULT = GLFW.GLFW_KEY_RIGHT_SHIFT;

	private static KeyBinding openGuiKey;

	@Override
	public void onInitializeClient() {
		moduleManager.registerAll();

		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.nyxclient.open_gui",
				InputUtil.Type.KEYSYM,
				GUI_KEY_DEFAULT,
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

		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			ConfigManager.load();
			migrateGuiKeyDefault(client);
			releaseVanillaConflict(client);
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

		LOGGER.info("Nyx Client initialized");
	}

	/**
	 * Minecraft persists every registered keybinding into options.txt, so an install that ran an
	 * earlier build still carries the key that was default back then - changing GUI_KEY_DEFAULT
	 * alone moves nothing for anyone who has already launched the game once.
	 *
	 * <p>The binding is therefore pulled forward at startup, but only when it is still sitting on
	 * the previous default, which is what tells "never touched it" apart from "deliberately chose
	 * that key" - the latter is left alone. The default in force is then recorded in the config, so
	 * this stays correct for any future move of GUI_KEY_DEFAULT rather than just this one.
	 */
	private static void migrateGuiKeyDefault(MinecraftClient client) {
		Integer recorded = ConfigManager.getRecordedGuiKeyDefault();
		int previousDefault = recorded != null ? recorded : LEGACY_GUI_KEY_DEFAULT;
		if (previousDefault == GUI_KEY_DEFAULT) {
			return;
		}

		InputUtil.Key previousKey = InputUtil.Type.KEYSYM.createFromCode(previousDefault);
		if (KeyBindingHelper.getBoundKeyOf(openGuiKey).equals(previousKey)) {
			openGuiKey.setBoundKey(openGuiKey.getDefaultKey());
			KeyBinding.updateKeysByCode();
			if (client.options != null) {
				client.options.write();
			}
			LOGGER.info("Moved the GUI keybinding off the old default onto {}",
					openGuiKey.getBoundKeyLocalizedText().getString());
		}

		ConfigManager.recordGuiKeyDefault(GUI_KEY_DEFAULT);
	}

	/**
	 * P is vanilla's Social Interactions key, and both bindings would fire on the same press - each
	 * KeyBinding counts its own presses, so the social screen would open right on top of the ClickGUI.
	 * Vanilla's binding gives way instead: it is unbound at startup whenever it still shares a key with
	 * ours, which also keeps working if the GUI key is later rebound onto some other vanilla action.
	 */
	private static void releaseVanillaConflict(MinecraftClient client) {
		if (client.options == null) {
			return;
		}

		KeyBinding social = client.options.socialInteractionsKey;
		if (!openGuiKey.isUnbound() && social.equals(openGuiKey)) {
			social.setBoundKey(InputUtil.UNKNOWN_KEY);
			KeyBinding.updateKeysByCode();
			LOGGER.info("Unbound vanilla Social Interactions - {} now opens the Nyx Client GUI",
					openGuiKey.getBoundKeyLocalizedText().getString());
		}
	}
}
