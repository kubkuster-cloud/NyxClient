package com.nyxclient.setting;

public abstract class Setting<T> {
	private final String name;
	protected T value;

	protected Setting(String name, T defaultValue) {
		this.name = name;
		this.value = defaultValue;
	}

	public String getName() {
		return name;
	}

	public T get() {
		return value;
	}

	public abstract void set(T value);

	// Used by ConfigManager to serialize/deserialize without needing per-type Gson adapters.
	public abstract String serialize();

	public abstract void deserialize(String raw);
}
