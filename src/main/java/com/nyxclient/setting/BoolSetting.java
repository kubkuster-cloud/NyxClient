package com.nyxclient.setting;

public class BoolSetting extends Setting<Boolean> {

	public BoolSetting(String name, boolean defaultValue) {
		super(name, defaultValue);
	}

	@Override
	public void set(Boolean value) {
		this.value = value;
	}

	public void toggle() {
		this.value = !this.value;
	}

	@Override
	public String serialize() {
		return String.valueOf(value);
	}

	@Override
	public void deserialize(String raw) {
		this.value = Boolean.parseBoolean(raw);
	}
}
