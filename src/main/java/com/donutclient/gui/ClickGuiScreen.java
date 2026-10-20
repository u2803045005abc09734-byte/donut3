package com.donutclient.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.donutclient.config.ConfigManager;
import com.donutclient.module.ClientModule;
import com.donutclient.module.ModuleManager;
import com.donutclient.setting.BoolSetting;
import com.donutclient.setting.ColorSetting;
import com.donutclient.setting.EnumSetting;
import com.donutclient.setting.NumberSetting;
import com.donutclient.setting.Setting;
import com.donutclient.util.Colors;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Dark, rounded ClickGUI. Drawn in immediate mode: every frame the layout is computed, drawn and
 * hit-tested in one pass. Mouse clicks are detected by polling the left button state.
 */
public class ClickGuiScreen extends Screen {
	private enum Tab { MODULES, FAVORITES, APPEARANCE }

	private record K(Object owner, int index) {}

	private static final String[] SECTIONS = {"Overview", "Storage", "Spawners"};
	private static final String[] GROUP_ORDER = {"Targets", "Colors", "Rendering", "Scanning", "Interface"};

	private static final int HEADER_H = 42;
	private static final int FOOTER_H = 22;
	private static final int SIDEBAR_W = 104;

	private static final int BG = 0xFF0E1322;
	private static final int BORDER = 0xFF1C2540;
	private static final int CARD_OFF = 0xFF131A2D;
	private static final int ROW = 0xFF121A2E;
	private static final int TEXT = 0xFFF0F3FF;
	private static final int SUB = 0xFF9AA6C8;
	private static final int DIM = 0xFF5E6A8C;

	private Tab tab = Tab.MODULES;
	private String section = "Overview";
	private ClientModule configuring;
	private EditBox search;

	private double scroll;
	private double scrollTarget;
	private int contentHeight;

	private boolean wasDown;
	private boolean down;
	private boolean pressed;
	private Object activeDrag;
	private boolean dirty;

	private final Map<K, Float> animations = new HashMap<>();

	private int mx;
	private int my;
	private int px;
	private int py;
	private int pw;
	private int ph;
	private int viewTop;
	private int viewBottom;
	private int accent;
	private int accentSoft;
	private int accentLight;
	private int radius;

	public ClickGuiScreen() {
		super(Component.literal("Donut Client"));
	}

	@Override
	protected void init() {
		search = new EditBox(this.font, 0, 0, 120, 12, Component.literal("Search modules"));
		search.setBordered(false);
		search.setMaxLength(32);
		search.setTextColor(0xFFE6EBFF);
		search.setResponder(s -> scrollTarget = 0);
		addRenderableWidget(search);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		ConfigManager.save();
		super.onClose();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scrollTarget -= scrollY * 28;
		return true;
	}

	// ------------------------------------------------------------------ frame

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		this.mx = mouseX;
		this.my = mouseY;
		this.down = this.minecraft != null && this.minecraft.mouseHandler.isLeftPressed();
		this.pressed = down && !wasDown;
		boolean released = !down && wasDown;
		this.wasDown = down;
		if (!down) {
			activeDrag = null;
		}
		if (released && dirty) {
			ConfigManager.save();
			dirty = false;
		}

		accent = Colors.opaque(Theme.ACCENT.rgb());
		accentSoft = Colors.mix(accent, BG, 0.62f);
		accentLight = Colors.mix(accent, 0xFFFFFFFF, 0.38f);
		radius = Theme.RADIUS.getInt();

		pw = Math.min(this.width - 24, 600);
		ph = Math.min(this.height - 24, 350);
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;

		g.fill(0, 0, this.width, this.height, 0x77040712);

		int panelAlpha = (int) (Theme.OPACITY.getDouble() / 100.0 * 255.0);
		rrect(g, px - 1, py - 1, pw + 2, ph + 2, radius + 1, BORDER);
		rrect(g, px, py, pw, ph, radius, (panelAlpha << 24) | (BG & 0xFFFFFF));

		// the search box is only shown on the module list
		boolean showSearch = tab != Tab.APPEARANCE && configuring == null;
		search.visible = showSearch;
		search.active = showSearch;
		if (!showSearch) {
			search.setFocused(false);
		}

		drawHeader(g);
		drawSidebar(g);
		drawContent(g);
		drawFooter(g);

		super.render(g, mouseX, mouseY, delta);
	}

	// ------------------------------------------------------------------ header / sidebar / footer

	private void drawHeader(GuiGraphics g) {
		// logo
		fillCircle(g, px + 24, py + HEADER_H / 2, 10, accent);
		fillCircle(g, px + 24, py + HEADER_H / 2, 4, BG);
		textBold(g, "DONUT", px + 42, py + HEADER_H / 2 - 4, TEXT);

		// tabs
		String[] names = {"Modules", "Favorites", "Appearance"};
		Tab[] tabs = {Tab.MODULES, Tab.FAVORITES, Tab.APPEARANCE};
		int gap = 6;
		int total = -gap;
		for (String n : names) {
			total += font.width(n) + 28 + gap;
		}
		int tx = px + pw / 2 - total / 2 + 20;
		for (int i = 0; i < names.length; i++) {
			int w = font.width(names[i]) + 28;
			int ty = py + 10;
			boolean active = tab == tabs[i];
			boolean hover = hit(tx, ty, w, 22);
			if (active) {
				rrect(g, tx, ty, w, 22, 8, 0xFF1A2340);
			} else if (hover) {
				rrect(g, tx, ty, w, 22, 8, 0xFF141C33);
			}
			text(g, names[i], tx + 14, ty + 7, active ? TEXT : hover ? 0xFFC4CDEB : SUB);
			if (pressed && hover) {
				tab = tabs[i];
				configuring = null;
				scrollTarget = 0;
				scroll = 0;
			}
			tx += w + gap;
		}

		// close button
		int cx = px + pw - 26;
		int cy = py + HEADER_H / 2 - 7;
		boolean hoverClose = hit(cx - 4, cy - 4, 22, 22);
		int xc = hoverClose ? TEXT : SUB;
		line(g, cx + 2, cy + 2, cx + 11, cy + 11, xc);
		line(g, cx + 11, cy + 2, cx + 2, cy + 11, xc);
		if (pressed && hoverClose) {
			this.onClose();
		}

		g.fill(px + 1, py + HEADER_H, px + pw - 1, py + HEADER_H + 1, BORDER);
	}

	private void drawSidebar(GuiGraphics g) {
		int top = py + HEADER_H + 1;
		int bottom = py + ph - FOOTER_H;
		g.fill(px + SIDEBAR_W, top, px + SIDEBAR_W + 1, bottom, BORDER);

		text(g, "VISUAL", px + 18, top + 14, DIM);

		int y = top + 34;
		for (String name : SECTIONS) {
			boolean active = tab == Tab.MODULES && section.equals(name);
			boolean hover = hit(px + 10, y, SIDEBAR_W - 20, 24);
			if (active) {
				rrect(g, px + 10, y, SIDEBAR_W - 20, 24, 8, Colors.mix(accent, BG, 0.74f));
				rrect(g, px + 10, y + 5, 3, 14, 1, accent);
			} else if (hover) {
				rrect(g, px + 10, y, SIDEBAR_W - 20, 24, 8, 0xFF121A2E);
			}
			text(g, name, px + 22, y + 8, active ? TEXT : hover ? 0xFFC4CDEB : SUB);
			if (hasActive(name)) {
				fillCircle(g, px + SIDEBAR_W - 22, y + 12, 2, accent);
			}
			if (pressed && hover) {
				tab = Tab.MODULES;
				section = name;
				configuring = null;
				scrollTarget = 0;
				scroll = 0;
			}
			y += 28;
		}

		int info = bottom - 46;
		text(g, "DONUT CLIENT", px + 18, info, DIM);
		text(g, "Fabric 1.21.11", px + 18, info + 12, SUB);
		text(g, ModuleManager.activeCount() + " / " + ModuleManager.all().size() + " active", px + 18, info + 24, DIM);
	}

	private boolean hasActive(String sectionName) {
		for (ClientModule m : ModuleManager.all()) {
			if (m.isEnabled() && (sectionName.equals("Overview") || m.category.equals(sectionName))) {
				return true;
			}
		}
		return false;
	}

	private void drawFooter(GuiGraphics g) {
		int y = py + ph - FOOTER_H;
		g.fill(px + 1, y, px + pw - 1, y + 1, BORDER);
		fillCircle(g, px + 18, y + FOOTER_H / 2 + 1, 2, accent);
		text(g, "Changes save automatically", px + 28, y + 7, DIM);
		String esc = "ESC  Close";
		text(g, esc, px + pw - 18 - font.width(esc), y + 7, DIM);
	}

	// ------------------------------------------------------------------ content

	private void drawContent(GuiGraphics g) {
		int cx = px + SIDEBAR_W + 18;
		int cy = py + HEADER_H + 14;
		int cw = pw - SIDEBAR_W - 36;
		viewTop = cy + 42;
		viewBottom = py + ph - FOOTER_H - 8;

		if (tab == Tab.APPEARANCE) {
			drawSettingsPage(g, cx, cy, cw, "Appearance", "Make the interface yours.", Theme.ALL, null);
		} else if (configuring != null) {
			drawSettingsPage(g, cx, cy, cw, configuring.name, configuring.description, configuring.settings(), configuring);
		} else {
			drawModuleList(g, cx, cy, cw);
		}
	}

	private void drawModuleList(GuiGraphics g, int cx, int cy, int cw) {
		boolean favorites = tab == Tab.FAVORITES;
		String title = favorites ? "Favorites" : section.equals("Overview") ? "Visual modules" : section + " modules";
		String sub = favorites ? "Your starred modules." : "A clearer view of your world.";
		textBold(g, title, cx, cy, TEXT);
		text(g, sub, cx, cy + 14, SUB);

		// search pill
		int sw = Math.min(210, cw / 2);
		int sx = cx + cw - sw;
		int sy = cy - 2;
		rrect(g, sx, sy, sw, 22, 9, ROW);
		fillCircle(g, sx + 13, sy + 10, 5, SUB);
		fillCircle(g, sx + 13, sy + 10, 3, ROW);
		line(g, sx + 17, sy + 14, sx + 20, sy + 17, SUB);
		search.setX(sx + 28);
		search.setY(sy + 6);
		search.setWidth(sw - 36);
		if (search.getValue().isEmpty() && !search.isFocused()) {
			text(g, "Search modules...", sx + 28, sy + 7, DIM);
		}

		String query = search.getValue().trim().toLowerCase(Locale.ROOT);
		List<ClientModule> shown = new ArrayList<>();
		for (ClientModule m : ModuleManager.all()) {
			if (favorites && !m.favorite) continue;
			if (!favorites && !section.equals("Overview") && !m.category.equals(section)) continue;
			if (!query.isEmpty() && !(m.name + " " + m.description).toLowerCase(Locale.ROOT).contains(query)) continue;
			shown.add(m);
		}

		clampScroll();
		g.enableScissor(cx - 4, viewTop, cx + cw + 4, viewBottom);
		int y = viewTop - (int) scroll;
		int start = y;
		if (shown.isEmpty()) {
			String empty = favorites ? "Star a module to find it here." : "No modules found.";
			text(g, empty, cx + (cw - font.width(empty)) / 2, y + 30, DIM);
			y += 60;
		}
		for (ClientModule m : shown) {
			drawModuleCard(g, m, cx, y, cw);
			y += 54;
		}
		if (!favorites && query.isEmpty() && section.equals("Overview")) {
			y += 10;
			drawHintCard(g, cx, y, cw);
			y += 56;
		}
		contentHeight = y - start;
		g.disableScissor();
	}

	private void drawModuleCard(GuiGraphics g, ClientModule m, int x, int y, int w) {
		int h = 46;
		boolean on = m.isEnabled();
		float a = animate(new K(m, 0), on ? 1f : 0f);

		if (a > 0.02f) {
			rrect(g, x - 1, y - 1, w + 2, h + 2, radius + 2, Colors.withAlpha(accent, (int) (70 * a)));
		}
		rrect(g, x, y, w, h, radius + 1, Colors.mix(CARD_OFF, accentSoft, a));

		// icon
		rrect(g, x + 9, y + 9, 28, 28, 8, Colors.mix(0xFF1A2340, Colors.mix(accent, BG, 0.45f), a));
		drawIcon(g, m.icon, x + 23, y + 23, Colors.mix(SUB, accentLight, a));

		textBold(g, m.name, x + 48, y + 8, TEXT);
		text(g, m.description, x + 48, y + 20, Colors.mix(SUB, 0xFFC1CCF0, a));

		String cfg = "Configure >";
		int cfgW = font.width(cfg);
		boolean hoverCfg = hitView(x + 48, y + 30, cfgW, 11);
		text(g, cfg, x + 48, y + 31, hoverCfg ? 0xFFFFFFFF : accentLight);
		if (pressed && hoverCfg) {
			configuring = m;
			scrollTarget = 0;
			scroll = 0;
		}

		// toggle
		int tx = x + w - 14 - 28;
		int ty = y + h / 2 - 8;
		if (toggleSwitch(g, new K(m, 1), tx, ty, on, true)) {
			m.toggle();
			dirty = true;
		}

		// favorite star
		int starX = tx - 26;
		int starY = y + h / 2 - 5;
		boolean hoverStar = hitView(starX - 3, starY - 3, 16, 16);
		String star = m.favorite ? "★" : "☆";
		int starColor = m.favorite ? 0xFFFFD166 : hoverStar ? 0xFFC4CDEB : DIM;
		text(g, star, starX, starY, starColor);
		if (pressed && hoverStar) {
			m.favorite = !m.favorite;
			dirty = true;
		}
	}

	private void drawHintCard(GuiGraphics g, int x, int y, int w) {
		int h = 46;
		rrect(g, x, y, w, h, radius + 1, 0xFF0F1526);
		int col = Colors.mix(accent, BG, 0.25f);
		int ix = x + 20;
		int iy = y + 12;
		g.renderOutline(ix, iy + 6, 18, 18, col);
		g.renderOutline(ix + 7, iy, 18, 18, col);
		line(g, ix, iy + 6, ix + 7, iy, col);
		line(g, ix + 18, iy + 6, ix + 25, iy, col);
		line(g, ix, iy + 24, ix + 7, iy + 18, col);
		line(g, ix + 18, iy + 24, ix + 25, iy + 18, col);
		textBold(g, "Make it yours", x + 62, y + 12, TEXT);
		text(g, "Select a module for colors, opacity and render modes.", x + 62, y + 25, SUB);
	}

	// ------------------------------------------------------------------ settings page

	private void drawSettingsPage(GuiGraphics g, int cx, int cy, int cw, String title, String sub, List<Setting<?>> settings, ClientModule module) {
		int titleX = cx;
		if (module != null) {
			boolean hoverBack = hit(cx, cy, 58, 22);
			rrect(g, cx, cy, 58, 22, 8, hoverBack ? 0xFF1A2340 : ROW);
			text(g, "< Back", cx + 11, cy + 7, hoverBack ? TEXT : SUB);
			if (pressed && hoverBack) {
				configuring = null;
				scrollTarget = 0;
				scroll = 0;
				return;
			}
			titleX = cx + 70;
		}
		textBold(g, title, titleX, cy, TEXT);
		text(g, sub, titleX, cy + 14, SUB);

		if (module != null) {
			int tx = cx + cw - 28;
			if (toggleSwitch(g, new K(module, 2), tx, cy + 2, module.isEnabled(), false)) {
				module.toggle();
				dirty = true;
			}
			String label = module.isEnabled() ? "Enabled" : "Disabled";
			text(g, label, tx - 10 - font.width(label), cy + 6, module.isEnabled() ? accentLight : DIM);
			String status = module.status();
			if (!status.isEmpty()) {
				text(g, status, tx - 10 - font.width(label) - 14 - font.width(status), cy + 6, DIM);
			}
		}

		// group the visible settings in a fixed order
		Map<String, List<Setting<?>>> groups = new LinkedHashMap<>();
		for (String group : GROUP_ORDER) {
			groups.put(group, new ArrayList<>());
		}
		for (Setting<?> s : settings) {
			if (s.isVisible()) {
				groups.computeIfAbsent(s.group, k -> new ArrayList<>()).add(s);
			}
		}

		clampScroll();
		g.enableScissor(cx - 4, viewTop, cx + cw + 4, viewBottom);
		int y = viewTop - (int) scroll;
		int start = y;
		for (Map.Entry<String, List<Setting<?>>> entry : groups.entrySet()) {
			if (entry.getValue().isEmpty()) {
				continue;
			}
			text(g, entry.getKey().toUpperCase(Locale.ROOT), cx + 4, y + 5, DIM);
			y += 20;
			for (Setting<?> s : entry.getValue()) {
				drawSettingRow(g, s, cx, y, cw);
				y += 38;
			}
			y += 6;
		}
		contentHeight = y - start;
		g.disableScissor();
	}

	private void drawSettingRow(GuiGraphics g, Setting<?> s, int x, int y, int w) {
		int h = 34;
		rrect(g, x, y, w, h, Math.max(2, radius - 1), ROW);

		int controlW;
		if (s instanceof BoolSetting) controlW = 28;
		else if (s instanceof EnumSetting) controlW = 112;
		else if (s instanceof NumberSetting) controlW = 170;
		else controlW = 18 + 10 + 3 * 50;

		int labelMax = Math.max(40, w - controlW - 40);
		text(g, font.plainSubstrByWidth(s.name, labelMax), x + 12, y + 7, TEXT);
		text(g, font.plainSubstrByWidth(s.description, labelMax), x + 12, y + 19, DIM);

		int right = x + w - 12;

		if (s instanceof BoolSetting b) {
			if (toggleSwitch(g, new K(s, 0), right - 28, y + 9, b.on(), true)) {
				b.toggle();
				dirty = true;
			}
		} else if (s instanceof EnumSetting e) {
			int bw = 112;
			int bx = right - bw;
			int by = y + 8;
			boolean hover = hitView(bx, by, bw, 18);
			rrect(g, bx, by, bw, 18, 7, hover ? 0xFF222D4D : 0xFF1A2340);
			String option = e.option();
			text(g, option, bx + (bw - font.width(option)) / 2, by + 5, TEXT);
			if (pressed && hover) {
				e.next();
				dirty = true;
			}
		} else if (s instanceof NumberSetting n) {
			int sw = 110;
			int sx = right - sw;
			double t = (n.getDouble() - n.min) / (n.max - n.min);
			Double nt = slider(g, new K(s, 0), sx, y + 14, sw, t, accentLight, false);
			if (nt != null) {
				double v = n.min + nt * (n.max - n.min);
				v = Math.round(v / n.step) * n.step;
				n.setDouble(v);
				dirty = true;
			}
			String value = n.display();
			text(g, value, sx - 10 - font.width(value), y + 13, 0xFFC7D2F5);
		} else if (s instanceof ColorSetting c) {
			int sw = 44;
			int vx = right - sw;
			int sx = vx - 6 - sw;
			int hx = sx - 6 - sw;
			int swatchX = hx - 12 - 18;

			Double nh = slider(g, new K(s, 0), hx, y + 14, sw, c.h, Colors.opaque(Colors.hsvToRgb(c.h, 1f, 1f)), true);
			Double ns = slider(g, new K(s, 1), sx, y + 14, sw, c.s, Colors.opaque(Colors.hsvToRgb(c.h, 1f, c.v)), false);
			Double nv = slider(g, new K(s, 2), vx, y + 14, sw, c.v, Colors.opaque(Colors.hsvToRgb(c.h, c.s, 1f)), false);
			if (nh != null || ns != null || nv != null) {
				c.setHsv(nh != null ? nh.floatValue() : c.h, ns != null ? ns.floatValue() : c.s, nv != null ? nv.floatValue() : c.v);
				dirty = true;
			}

			rrect(g, swatchX - 1, y + 8, 20, 20, 6, 0xFF2A3556);
			rrect(g, swatchX, y + 9, 18, 18, 5, Colors.opaque(c.rgb()));
		}
	}

	// ------------------------------------------------------------------ widgets

	/** Draws a switch and returns true on the frame it was clicked. */
	private boolean toggleSwitch(GuiGraphics g, K key, int x, int y, boolean on, boolean inScrollArea) {
		float t = animate(key, on ? 1f : 0f);
		rrect(g, x, y, 28, 16, 8, Colors.mix(0xFF2A3350, accent, t));
		fillCircle(g, x + 8 + (int) (t * 12), y + 8, 6, 0xFFFFFFFF);
		boolean hover = inScrollArea ? hitView(x, y, 28, 16) : hit(x, y, 28, 16);
		return pressed && hover;
	}

	/**
	 * Draws a horizontal slider. Returns the new 0..1 value while the user drags it, otherwise null.
	 */
	private Double slider(GuiGraphics g, K id, int x, int y, int w, double t, int fill, boolean hueTrack) {
		boolean hover = my >= viewTop && my <= viewBottom && mx >= x - 4 && mx <= x + w + 4 && my >= y - 6 && my <= y + 12;
		if (pressed && hover) {
			activeDrag = id;
		}
		Double result = null;
		if (Objects.equals(activeDrag, id) && down) {
			t = Math.max(0.0, Math.min(1.0, (mx - x) / (double) w));
			result = t;
		}

		if (hueTrack) {
			for (int i = 0; i < w; i += 2) {
				g.fill(x + i, y, x + Math.min(i + 2, w), y + 6, Colors.opaque(Colors.hsvToRgb(i / (float) w, 1f, 1f)));
			}
		} else {
			rrect(g, x, y, w, 6, 3, 0xFF222B45);
			int fw = (int) (t * w);
			if (fw > 0) {
				rrect(g, x, y, Math.max(6, fw), 6, 3, fill);
			}
		}
		int kx = x + (int) (t * w);
		fillCircle(g, kx, y + 3, 5, 0xFFFFFFFF);
		fillCircle(g, kx, y + 3, 3, fill);
		return result;
	}

	private void drawIcon(GuiGraphics g, ClientModule.Icon icon, int cx, int cy, int color) {
		switch (icon) {
			case CHEST -> {
				g.renderOutline(cx - 8, cy - 6, 16, 12, color);
				g.fill(cx - 8, cy - 2, cx + 8, cy - 1, color);
				g.fill(cx - 1, cy - 3, cx + 1, cy + 1, color);
			}
			case GRID -> {
				g.renderOutline(cx - 8, cy - 8, 16, 16, color);
				g.fill(cx - 3, cy - 8, cx - 2, cy + 8, color);
				g.fill(cx + 2, cy - 8, cx + 3, cy + 8, color);
				g.fill(cx - 8, cy - 3, cx + 8, cy - 2, color);
				g.fill(cx - 8, cy + 2, cx + 8, cy + 3, color);
			}
		}
	}

	// ------------------------------------------------------------------ helpers

	private void clampScroll() {
		int visible = viewBottom - viewTop;
		double max = Math.max(0, contentHeight - visible);
		scrollTarget = Math.max(0, Math.min(max, scrollTarget));
		scroll += (scrollTarget - scroll) * (Theme.ANIMATIONS.on() ? 0.4 : 1.0);
		if (Math.abs(scrollTarget - scroll) < 0.3) {
			scroll = scrollTarget;
		}
	}

	private float animate(K key, float target) {
		float v = animations.getOrDefault(key, target);
		v += (target - v) * (Theme.ANIMATIONS.on() ? 0.35f : 1f);
		if (Math.abs(target - v) < 0.01f) {
			v = target;
		}
		animations.put(key, v);
		return v;
	}

	private boolean hit(int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** Like {@link #hit} but ignores the part of the element that is scrolled out of view. */
	private boolean hitView(int x, int y, int w, int h) {
		return hit(x, y, w, h) && my >= viewTop && my < viewBottom;
	}

	private void text(GuiGraphics g, String s, int x, int y, int color) {
		g.drawString(this.font, s, x, y, color, false);
	}

	private void textBold(GuiGraphics g, String s, int x, int y, int color) {
		g.drawString(this.font, Component.literal(s).withStyle(ChatFormatting.BOLD), x, y, color, false);
	}

	/** Filled rounded rectangle. */
	private static void rrect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
		r = Math.min(r, Math.min(w, h) / 2);
		if (r <= 0) {
			g.fill(x, y, x + w, y + h, color);
			return;
		}
		for (int i = 0; i < r; i++) {
			double dy = r - i - 0.5;
			int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
			g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
			g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
		}
		g.fill(x, y + r, x + w, y + h - r, color);
	}

	private static void fillCircle(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int dx = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy + 0.25)));
			g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
		}
	}

	private static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
		if (steps == 0) {
			g.fill(x1, y1, x1 + 1, y1 + 1, color);
			return;
		}
		for (int i = 0; i <= steps; i++) {
			int x = x1 + Math.round((x2 - x1) * (i / (float) steps));
			int y = y1 + Math.round((y2 - y1) * (i / (float) steps));
			g.fill(x, y, x + 1, y + 1, color);
		}
	}
}
