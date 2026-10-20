package com.donutclient.setting;

import java.util.function.BooleanSupplier;

import com.google.gson.JsonElement;

public abstract class Setting<T> {
	public final String group;
	public final String name;
	public final String description;
	protected final T defaultValue;
	protected T value;
	private BooleanSupplier visibility;

	protected Setting(String group, String name, String description, T defaultValue) {
		this.group = group;
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	public void reset() {
		set(defaultValue);
	}

	public void visibleWhen(BooleanSupplier supplier) {
		this.visibility = supplier;
	}

	public boolean isVisible() {
		return visibility == null || visibility.getAsBoolean();
	}

	public abstract JsonElement save();

	public abstract void load(JsonElement element);
}
