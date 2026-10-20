package com.donutclient.gui;

import java.util.List;

import com.donutclient.setting.BoolSetting;
import com.donutclient.setting.ColorSetting;
import com.donutclient.setting.NumberSetting;
import com.donutclient.setting.Setting;

/** User-adjustable look of the ClickGUI (the "Appearance" tab). */
public final class Theme {
	public static final ColorSetting ACCENT = new ColorSetting("Interface", "Accent Color",
		"Main color of the interface.", 0x4F7CFF);
	public static final NumberSetting OPACITY = new NumberSetting("Interface", "Panel Opacity",
		"How see-through the window is.", 96, 50, 100, 1, "%", 0);
	public static final NumberSetting RADIUS = new NumberSetting("Interface", "Corner Radius",
		"How round the window and cards are.", 9, 0, 14, 1, " px", 0);
	public static final BoolSetting ANIMATIONS = new BoolSetting("Interface", "Animations",
		"Smooth toggles and sliders.", true);

	public static final List<Setting<?>> ALL = List.of(ACCENT, OPACITY, RADIUS, ANIMATIONS);

	private Theme() {}
}
