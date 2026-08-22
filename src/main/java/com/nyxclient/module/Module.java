package com.nyxclient.module;

import com.nyxclient.setting.Setting;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {

	private final String name;
	private final String description;
	private final Category category;
	private final List<Setting<?>> settings = new ArrayList<>();

	private boolean enabled = false;
	private int keyCode = -1; // GLFW key code, -1 = unbound

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	protected Module(String name, String description, Category category, int defaultKeyCode) {
		this(name, description, category);
		this.keyCode = defaultKeyCode;
	}

	public final void toggle() {
		setEnabled(!enabled);
	}

	public final void setEnabled(boolean value) {
		if (this.enabled == value) return;
		this.enabled = value;
		if (value) {
			onEnable();
		} else {
			onDisable();
		}
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	public void onTick() {
	}

	protected <T extends Setting<?>> T addSetting(T setting) {
		settings.add(setting);
		return setting;
	}

	public List<Setting<?>> getSettings() {
		return settings;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Category getCategory() {
		return category;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int getKeyCode() {
		return keyCode;
	}

	public void setKeyCode(int keyCode) {
		this.keyCode = keyCode;
	}
}
