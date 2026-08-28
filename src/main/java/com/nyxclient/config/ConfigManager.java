package com.nyxclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.nyxclient.NyxClient;
import com.nyxclient.module.Module;
import com.nyxclient.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigManager {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("nyxclient.json");

	private static final String MODULES_KEY = "modules";
	private static final String GUI_KEY_DEFAULT_KEY = "guiKeyDefault";

	// Which default the GUI keybinding had the last time this config was written, so NyxClient can
	// tell "user is still on the old default" from "user deliberately picked that key" when the
	// default moves. Null until a config written by this version exists.
	private static Integer recordedGuiKeyDefault;

	public static Integer getRecordedGuiKeyDefault() {
		return recordedGuiKeyDefault;
	}

	public static void recordGuiKeyDefault(int keyCode) {
		recordedGuiKeyDefault = keyCode;
		save();
	}

	public static void save() {
		Map<String, ModuleData> out = new LinkedHashMap<>();

		for (Module module : NyxClient.moduleManager.getModules()) {
			ModuleData data = new ModuleData();
			data.enabled = module.isEnabled();
			data.keyCode = module.getKeyCode();
			data.settings = new LinkedHashMap<>();
			for (Setting<?> setting : module.getSettings()) {
				data.settings.put(setting.getName(), setting.serialize());
			}
			out.put(module.getName(), data);
		}

		JsonObject root = new JsonObject();
		if (recordedGuiKeyDefault != null) {
			root.addProperty(GUI_KEY_DEFAULT_KEY, recordedGuiKeyDefault);
		}
		root.add(MODULES_KEY, GSON.toJsonTree(out));

		try {
			Files.writeString(CONFIG_PATH, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException e) {
			NyxClient.LOGGER.error("Failed to save config", e);
		}
	}

	public static void load() {
		if (!Files.exists(CONFIG_PATH)) return;

		try {
			String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
			JsonObject root = JsonParser.parseString(json).getAsJsonObject();

			// Configs written before client-level keys existed are the module map itself, with a
			// module name at every top-level key - no module is called "modules", so its presence
			// is what distinguishes the two layouts.
			JsonObject modules = root;
			if (root.has(MODULES_KEY) && root.get(MODULES_KEY).isJsonObject()) {
				modules = root.getAsJsonObject(MODULES_KEY);
				if (root.has(GUI_KEY_DEFAULT_KEY)) {
					recordedGuiKeyDefault = root.get(GUI_KEY_DEFAULT_KEY).getAsInt();
				}
			}

			Type type = new TypeToken<Map<String, ModuleData>>() {}.getType();
			Map<String, ModuleData> in = GSON.fromJson(modules, type);
			if (in == null) return;

			for (Map.Entry<String, ModuleData> entry : in.entrySet()) {
				Module module = NyxClient.moduleManager.getModule(entry.getKey());
				if (module == null) continue;

				ModuleData data = entry.getValue();
				module.setKeyCode(data.keyCode);

				if (data.settings != null) {
					for (Setting<?> setting : module.getSettings()) {
						String raw = data.settings.get(setting.getName());
						if (raw != null) {
							try {
								setting.deserialize(raw);
							} catch (Exception e) {
								NyxClient.LOGGER.warn("Failed to parse setting {} for module {}", setting.getName(), module.getName());
							}
						}
					}
				}

				// Apply enabled state last so onEnable() sees fully-loaded settings.
				module.setEnabled(data.enabled);
			}
		} catch (IOException | RuntimeException e) {
			NyxClient.LOGGER.error("Failed to load config", e);
		}
	}

	private static class ModuleData {
		boolean enabled;
		int keyCode = -1;
		Map<String, String> settings = new HashMap<>();
	}
}
