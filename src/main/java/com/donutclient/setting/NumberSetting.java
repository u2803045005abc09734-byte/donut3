package com.donutclient.setting;

import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
	public final double min;
	public final double max;
	public final double step;
	private final String unit;
	private final int decimals;

	public NumberSetting(String group, String name, String description, double def, double min, double max, double step, String unit, int decimals) {
		super(group, name, description, def);
		this.min = min;
		this.max = max;
		this.step = step;
		this.unit = unit;
		this.decimals = decimals;
	}

	public double getDouble() {
		return value;
	}

	public float getFloat() {
		return value.floatValue();
	}

	public int getInt() {
		return (int) Math.round(value);
	}

	public void setDouble(double v) {
		v = Math.max(min, Math.min(max, v));
		value = Math.round(v * 1_000_000d) / 1_000_000d;
	}

	@Override
	public void set(Double v) {
		setDouble(v);
	}

	public String display() {
		return String.format(Locale.ROOT, "%." + decimals + "f%s", value, unit);
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			try {
				setDouble(element.getAsDouble());
			} catch (NumberFormatException ignored) {
			}
		}
	}
}
