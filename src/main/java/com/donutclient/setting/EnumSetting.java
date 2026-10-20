package com.donutclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A setting that cycles through a fixed list of named options. The value is the option index. */
public class EnumSetting extends Setting<Integer> {
	private final String[] options;

	public EnumSetting(String group, String name, String description, String[] options, int defIndex) {
		super(group, name, description, defIndex);
		this.options = options;
	}

	public int index() {
		return value;
	}

	public String option() {
		return options[value];
	}

	public void next() {
		value = (value + 1) % options.length;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(options[value]);
	}

	@Override
	public void load(JsonElement element) {
		if (element == null || !element.isJsonPrimitive()) {
			return;
		}
		String s = element.getAsString();
		for (int i = 0; i < options.length; i++) {
			if (options[i].equals(s)) {
				value = i;
				return;
			}
		}
	}
}
