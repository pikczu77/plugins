package io.github.pikczu77.hpsize.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import net.fabricmc.loader.api.FabricLoader;

import io.github.pikczu77.hpsize.HpSize;

/**
 * Client settings, stored in {@code config/hpsize-client.json}.
 */
public final class ClientConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ClientConfig instance = new ClientConfig();

	/** HUD elements hidden in the "clean HUD" mode (ids from Fabric's VanillaHudElements). */
	public List<String> hiddenHud = new ArrayList<>(List.of(
			"hotbar", "armor_bar", "health_bar", "food_bar", "air_bar", "mount_health",
			"info_bar", "experience_level", "held_item_tooltip", "status_effects", "crosshair"));
	public double zoomFactor = 4.0;
	/** Cinematic (smoothed) mouse while zooming, like in OptiFine. */
	public boolean zoomCinematic = true;

	public static ClientConfig get() {
		return instance;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("hpsize-client.json");
	}

	public static void load() {
		Path file = file();

		if (Files.exists(file)) {
			try {
				ClientConfig loaded = GSON.fromJson(Files.readString(file), ClientConfig.class);

				if (loaded != null) {
					instance = loaded;
				}
			} catch (IOException | JsonParseException e) {
				HpSize.LOGGER.error("Could not read {}", file, e);
			}
		}

		if (instance.hiddenHud == null) {
			instance.hiddenHud = new ArrayList<>();
		}

		instance.zoomFactor = Math.max(1.1, Math.min(50.0, instance.zoomFactor));
	}

	public void save() {
		try {
			Files.writeString(file(), GSON.toJson(this));
		} catch (IOException e) {
			HpSize.LOGGER.error("Could not save {}", file(), e);
		}
	}
}
