package com.donutclient.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.donutclient.setting.Setting;

import net.minecraft.client.Minecraft;

public abstract class ClientModule {
	public enum Icon { CHEST, GRID }

	public final String name;
	public final String description;
	/** Sidebar section this module belongs to (e.g. "Storage"). */
	public final String category;
	public final Icon icon;

	private boolean enabled;
	public boolean favorite;
	private final List<Setting<?>> settings = new ArrayList<>();

	protected ClientModule(String name, String description, String category, Icon icon) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.icon = icon;
	}

	protected <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	public List<Setting<?>> settings() {
		return Collections.unmodifiableList(settings);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	/** Short status text shown in the settings page header, may be empty. */
	public String status() {
		return "";
	}

	protected void onEnable() {}

	protected void onDisable() {}

	public void onTick(Minecraft mc) {}
}
