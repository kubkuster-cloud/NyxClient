package com.nyxclient.module;

import com.nyxclient.module.modules.combat.KillauraModule;
import com.nyxclient.module.modules.movement.FlyModule;
import com.nyxclient.module.modules.movement.NoFallModule;
import com.nyxclient.module.modules.movement.ScaffoldModule;
import com.nyxclient.module.modules.movement.SpeedModule;
import com.nyxclient.module.modules.render.ESPModule;
import com.nyxclient.module.modules.render.FullbrightModule;
import com.nyxclient.module.modules.world.FastbreakModule;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {

	private final List<Module> modules = new ArrayList<>();
	private final Map<Module, Boolean> keyWasDown = new HashMap<>();

	public void registerAll() {
		register(new FlyModule());
		register(new SpeedModule());
		register(new ScaffoldModule());
		register(new NoFallModule());
		register(new KillauraModule());
		register(new ESPModule());
		register(new FullbrightModule());
		register(new FastbreakModule());
	}

	private void register(Module module) {
		modules.add(module);
		keyWasDown.put(module, false);
	}

	public List<Module> getModules() {
		return modules;
	}

	public List<Module> getModules(Category category) {
		List<Module> result = new ArrayList<>();
		for (Module m : modules) {
			if (m.getCategory() == category) result.add(m);
		}
		return result;
	}

	public Module getModule(String name) {
		for (Module m : modules) {
			if (m.getName().equalsIgnoreCase(name)) return m;
		}
		return null;
	}

	public void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) return;

		handleKeybinds(client);

		for (Module m : modules) {
			if (m.isEnabled()) {
				m.onTick();
			}
		}
	}

	private void handleKeybinds(MinecraftClient client) {
		// Only toggle from bare key presses when no screen (chat, gui, inventory...) is open,
		// otherwise typing in a text field would trigger modules.
		if (client.currentScreen != null) return;

		long handle = client.getWindow().getHandle();
		for (Module m : modules) {
			int key = m.getKeyCode();
			if (key < 0) continue;

			boolean down = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
			boolean wasDown = keyWasDown.getOrDefault(m, false);

			if (down && !wasDown) {
				m.toggle();
			}
			keyWasDown.put(m, down);
		}
	}
}
