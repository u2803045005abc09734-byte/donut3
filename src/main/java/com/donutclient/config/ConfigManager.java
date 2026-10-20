package com.donutclient.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.donutclient.gui.Theme;
import com.donutclient.module.ClientModule;
import com.donutclient.module.ModuleManager;
import com.donutclient.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("donutclient");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ConfigManager() {}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("donutclient.json");
	}

	public static void load() {
		Path file = path();
		if (!Files.exists(file)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();

			if (root.has("gui") && root.get("gui").isJsonObject()) {
				JsonObject gui = root.getAsJsonObject("gui");
				for (Setting<?> s : Theme.ALL) {
					if (gui.has(s.name)) {
						s.load(gui.get(s.name));
					}
				}
			}

			if (root.has("modules") && root.get("modules").isJsonObject()) {
				JsonObject modules = root.getAsJsonObject("modules");
				for (ClientModule m : ModuleManager.all()) {
					if (!modules.has(m.name) || !modules.get(m.name).isJsonObject()) {
						continue;
					}
					JsonObject o = modules.getAsJsonObject(m.name);
					if (o.has("enabled")) {
						m.setEnabled(o.get("enabled").getAsBoolean());
					}
					if (o.has("favorite")) {
						m.favorite = o.get("favorite").getAsBoolean();
					}
					if (o.has("settings") && o.get("settings").isJsonObject()) {
						JsonObject settings = o.getAsJsonObject("settings");
						for (Setting<?> s : m.settings()) {
							if (settings.has(s.name)) {
								s.load(settings.get(s.name));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Could not read donutclient.json, using defaults", e);
		}
	}

	public static void save() {
		try {
			JsonObject root = new JsonObject();

			JsonObject gui = new JsonObject();
			for (Setting<?> s : Theme.ALL) {
				gui.add(s.name, s.save());
			}
			root.add("gui", gui);

			JsonObject modules = new JsonObject();
			for (ClientModule m : ModuleManager.all()) {
				JsonObject o = new JsonObject();
				o.addProperty("enabled", m.isEnabled());
				o.addProperty("favorite", m.favorite);
				JsonObject settings = new JsonObject();
				for (Setting<?> s : m.settings()) {
					settings.add(s.name, s.save());
				}
				o.add("settings", settings);
				modules.add(m.name, o);
			}
			root.add("modules", modules);

			Files.writeString(path(), GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.warn("Could not save donutclient.json", e);
		}
	}
}
