package io.github.pikczu77.reckit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;

/**
 * Item library for videos: custom items taken from datapacks and resource packs, without their gameplay. Every item is
 * a plain prop with its own model and texture, grouped by source pack in the creative inventory.
 */
public class Reckit implements ModInitializer {
	public static final String MOD_ID = "reckit";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		Catalog catalog = Catalog.load();
		PropItems.register(catalog);
		LOGGER.info("Registered {} props from {} packs", PropItems.all().size(), catalog.packs().size());
	}
}
