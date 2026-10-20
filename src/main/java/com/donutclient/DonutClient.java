package com.donutclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.donutclient.config.ConfigManager;
import com.donutclient.gui.ClickGuiScreen;
import com.donutclient.module.ClientModule;
import com.donutclient.module.ModuleManager;
import com.donutclient.render.BoxRenderer;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class DonutClient implements ClientModInitializer {
	public static final String MOD_ID = "donutclient";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyMapping openGuiKey;

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		ConfigManager.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		openGuiKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key." + MOD_ID + ".open_gui", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openGuiKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new ClickGuiScreen());
				}
			}
			for (ClientModule module : ModuleManager.all()) {
				module.onTick(client);
			}
		});

		WorldRenderEvents.BEFORE_TRANSLUCENT.register(BoxRenderer.INSTANCE::render);

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			ConfigManager.save();
			BoxRenderer.INSTANCE.close();
		});

		LOGGER.info("Donut Client loaded ({} modules)", ModuleManager.all().size());
	}
}
