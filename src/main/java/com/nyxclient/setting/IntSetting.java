package com.nyxclient.setting;

public class IntSetting extends Setting<Integer> {

	private final int min;
	private final int max;

	public IntSetting(String name, int defaultValue, int min, int max) {
		super(name, defaultValue);
		this.min = min;
		this.max = max;
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	@Override
	public void set(Integer value) {
		this.value = Math.max(min, Math.min(max, value));
	}

	@Override
	public String serialize() {
		return String.valueOf(value);
	}

	@Override
	public void deserialize(String raw) {
		set(Integer.parseInt(raw));
	}
}
