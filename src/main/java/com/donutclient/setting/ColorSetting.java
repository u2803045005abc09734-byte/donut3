package com.donutclient.setting;

import com.donutclient.util.Colors;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** An RGB color edited through hue / saturation / brightness. */
public class ColorSetting extends Setting<Integer> {
	public float h;
	public float s;
	public float v;

	public ColorSetting(String group, String name, String description, int rgb) {
		super(group, name, description, rgb & 0xFFFFFF);
		setRgb(rgb);
	}

	public int rgb() {
		return Colors.hsvToRgb(h, s, v);
	}

	public void setRgb(int rgb) {
		float[] hsv = Colors.rgbToHsv(rgb & 0xFFFFFF);
		this.h = hsv[0];
		this.s = hsv[1];
		this.v = hsv[2];
	}

	public void setHsv(float h, float s, float v) {
		this.h = Math.max(0f, Math.min(1f, h));
		this.s = Math.max(0f, Math.min(1f, s));
		this.v = Math.max(0f, Math.min(1f, v));
	}

	@Override
	public Integer get() {
		return rgb();
	}

	@Override
	public void set(Integer rgb) {
		setRgb(rgb);
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(rgb());
	}

	@Override
	public void load(JsonElement element) {
		if (element != null && element.isJsonPrimitive()) {
			try {
				setRgb(element.getAsInt());
			} catch (NumberFormatException ignored) {
			}
		}
	}
}
