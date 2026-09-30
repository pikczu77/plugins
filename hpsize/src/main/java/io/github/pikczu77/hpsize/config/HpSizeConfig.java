package io.github.pikczu77.hpsize.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

import io.github.pikczu77.hpsize.HpSize;

/**
 * Per-world settings of the size-follows-health mechanic, stored in {@code <world>/hpsize.json}.
 */
public final class HpSizeConfig {
	/** Vanilla limits of the {@code minecraft:scale} attribute. */
	public static final double SCALE_MIN = 0.0625;
	public static final double SCALE_MAX = 16.0;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static @Nullable HpSizeConfig current;
	private static @Nullable Path file;

	public enum Mode {
		/** Size = current health (like in the video). */
		HEALTH("health", "rozmiar = aktualne HP"),
		/** Size = max health, damage does not shrink. */
		MAX("max", "rozmiar = maksymalne HP (nie maleje)"),
		/** Size = max health, shrinking proportionally to the lost health (bosses shrink too). */
		PERCENT("percent", "rozmiar = maks. HP × % życia (bossowie też maleją)"),
		/** Size = square root of the current health (smaller giants, less lag). */
		SQRT("sqrt", "rozmiar = √HP (mniejsi giganci, mniej lagów)");

		public final String id;
		public final String description;

		Mode(String id, String description) {
			this.id = id;
			this.description = description;
		}

		public static @Nullable Mode byId(String id) {
			for (Mode mode : values()) {
				if (mode.id.equals(id.toLowerCase(Locale.ROOT))) {
					return mode;
				}
			}

			return null;
		}
	}

	public boolean enabled = true;
	public Mode mode = Mode.HEALTH;
	/** Size multiplier: scale = health × factor. */
	public double factor = 1.0;
	public double minScale = SCALE_MIN;
	public double maxScale = SCALE_MAX;
	/** How gently the size follows the health (0 = instantly, like the command from the video). */
	public int smooth = 5;
	public boolean players = false;
	/** When false, scaled mobs stuck in blocks do not suffocate (and therefore do not shrink). */
	public boolean suffocation = true;
	/** When true, sizes stop following the health (good for a still shot). */
	public boolean frozen = false;
	/** Boss bar with the health and size of the mob the player looks at / has hit. */
	public boolean healthBar = true;
	/** Tiny mobs glow so viewers (and you) can find them. */
	public boolean glowTiny = false;
	public double glowTinyBelow = 0.35;
	/** A "deflating" sound when a hit makes a mob smaller. */
	public boolean shrinkSound = false;
	/** Entity types that keep their normal size. */
	public List<String> excluded = new ArrayList<>(List.of("minecraft:armor_stand", "minecraft:ender_dragon"));

	private transient Set<EntityType<?>> excludedTypes = Set.of();

	public static @Nullable HpSizeConfig get() {
		return current;
	}

	public static void load(MinecraftServer server) {
		file = server.getWorldPath(LevelResource.ROOT).resolve("hpsize.json");
		HpSizeConfig config = null;

		if (Files.exists(file)) {
			try {
				config = GSON.fromJson(Files.readString(file), HpSizeConfig.class);
			} catch (IOException | JsonParseException e) {
				HpSize.LOGGER.error("Could not read {}, using defaults", file, e);
			}
		}

		current = config == null ? new HpSizeConfig() : config;
		current.sanitize();
	}

	public static void unload() {
		current = null;
		file = null;
	}

	public void save() {
		sanitize();

		if (file == null) {
			return;
		}

		try {
			Files.writeString(file, GSON.toJson(this));
		} catch (IOException e) {
			HpSize.LOGGER.error("Could not save {}", file, e);
		}
	}

	public void applyPreset(String preset) {
		HpSizeConfig defaults = new HpSizeConfig();

		switch (preset) {
			case "film" -> {
				// Exactly like the single command from the video.
				mode = Mode.HEALTH;
				factor = 1.0;
				minScale = SCALE_MIN;
				maxScale = SCALE_MAX;
				smooth = 0;
				suffocation = true;
			}
			case "smooth" -> {
				mode = Mode.HEALTH;
				factor = 1.0;
				minScale = SCALE_MIN;
				maxScale = SCALE_MAX;
				smooth = defaults.smooth;
				suffocation = true;
			}
			case "fair" -> {
				mode = Mode.PERCENT;
				factor = 1.0;
				minScale = 0.1;
				maxScale = SCALE_MAX;
				smooth = defaults.smooth;
			}
			case "light" -> {
				mode = Mode.SQRT;
				factor = 1.0;
				minScale = 0.25;
				maxScale = 6.0;
				smooth = defaults.smooth;
			}
			default -> throw new IllegalArgumentException(preset);
		}
	}

	public static List<String> presets() {
		return List.of("film", "smooth", "fair", "light");
	}

	public void sanitize() {
		if (mode == null) {
			mode = Mode.HEALTH;
		}

		factor = clamp(factor, 0.001, 100);
		minScale = clamp(minScale, SCALE_MIN, SCALE_MAX);
		maxScale = clamp(maxScale, SCALE_MIN, SCALE_MAX);

		if (minScale > maxScale) {
			minScale = maxScale;
		}

		smooth = Math.max(0, Math.min(100, smooth));
		glowTinyBelow = clamp(glowTinyBelow, SCALE_MIN, SCALE_MAX);

		if (excluded == null) {
			excluded = new ArrayList<>();
		}

		Set<EntityType<?>> types = new HashSet<>();

		for (String id : excluded) {
			Identifier identifier = Identifier.tryParse(id);

			if (identifier != null) {
				BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).ifPresent(types::add);
			}
		}

		excludedTypes = types;
	}

	/**
	 * Whether the mechanic changes the size of this entity.
	 */
	public boolean affects(LivingEntity entity) {
		if (entity instanceof Player) {
			return players && !entity.isSpectator();
		}

		return !excludedTypes.contains(entity.getType());
	}

	/**
	 * The size multiplier the entity should have (1.0 = vanilla size).
	 */
	public double targetScale(LivingEntity entity) {
		return targetScale(entity, entity.getHealth());
	}

	/**
	 * The size multiplier the entity would have with the given health.
	 */
	public double targetScale(LivingEntity entity, double currentHealth) {
		double health = Math.max(0, currentHealth);
		double maxHealth = Math.max(0.001, entity.getMaxHealth());

		double scale = switch (mode) {
			case HEALTH -> health * factor;
			case MAX -> maxHealth * factor;
			case PERCENT -> clamp(maxHealth * factor, minScale, maxScale) * Math.min(1.0, health / maxHealth);
			case SQRT -> Math.sqrt(health) * factor;
		};

		return clamp(scale, minScale, maxScale);
	}

	private static double clamp(double value, double min, double max) {
		return Double.isNaN(value) ? min : Math.max(min, Math.min(max, value));
	}
}
