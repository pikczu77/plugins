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
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
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
	/** Health of a player: a mob with this much health keeps its normal size. */
	public static final double NORMAL_HP = 20.0;
	/** Version of the settings file; older files are migrated in {@link #load}. */
	private static final int CONFIG_VERSION = 2;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static @Nullable HpSizeConfig current;
	private static @Nullable Path file;

	public enum Mode {
		/** Size follows the current health (like in the video). */
		HEALTH("health", "size follows the current HP", "rozmiar według aktualnego HP"),
		/** Size follows the max health, damage does not shrink. */
		MAX("max", "size follows the max HP (never shrinks)", "rozmiar według maksymalnego HP (nie maleje)"),
		/** Size follows the max health, shrinking proportionally to the lost health (bosses shrink too). */
		PERCENT("percent", "size follows the max HP and shrinks with the % of health left (bosses shrink too)",
				"rozmiar według maks. HP, maleje z % życia (bossowie też maleją)"),
		/** Size follows the square root of the health (tiny mobs less tiny, giants less giant). */
		SQRT("sqrt", "size follows √HP (smaller differences, less lag)", "rozmiar według √HP (mniejsze różnice, mniej lagów)");

		public final String id;
		public final String english;
		public final String polish;

		Mode(String id, String english, String polish) {
			this.id = id;
			this.english = english;
			this.polish = polish;
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
	/** Health at which a mob has its normal size: scale = health / normalHp (chicken 4 HP -> x0.2, iron golem 100 HP -> x5). */
	public double normalHp = NORMAL_HP;
	public double minScale = SCALE_MIN;
	public double maxScale = SCALE_MAX;
	/** How gently the size follows the health (0 = instantly, like the command from the video). */
	public int smooth = 5;
	public boolean players = false;
	/** When false, scaled mobs stuck in blocks do not suffocate (and therefore do not shrink). */
	public boolean suffocation = true;
	/** When true, sizes stop following the health (good for a still shot). */
	public boolean frozen = false;
	/** Tiny mobs glow so viewers (and you) can find them. */
	public boolean glowTiny = false;
	public double glowTinyBelow = 0.35;
	/** A "deflating" sound when a hit makes a mob smaller. */
	public boolean shrinkSound = false;
	/** Size of the Ender Dragon at full health (it shrinks with its health like every other mob). */
	public double dragonScale = 8.0;
	/** Entity types that keep their normal size. */
	public List<String> excluded = new ArrayList<>(List.of("minecraft:armor_stand"));
	/** 0 in files written before versions existed. */
	public int configVersion;

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

		if (current.configVersion < 2 && current.excluded != null) {
			// The Ender Dragon used to be excluded because vanilla does not draw it scaled; the mod scales it now.
			current.excluded.remove("minecraft:ender_dragon");
		}

		current.configVersion = CONFIG_VERSION;
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
				normalHp = NORMAL_HP;
				minScale = SCALE_MIN;
				maxScale = SCALE_MAX;
				smooth = 0;
				suffocation = true;
			}
			case "smooth" -> {
				mode = Mode.HEALTH;
				normalHp = NORMAL_HP;
				minScale = SCALE_MIN;
				maxScale = SCALE_MAX;
				smooth = defaults.smooth;
				suffocation = true;
			}
			case "fair" -> {
				mode = Mode.PERCENT;
				normalHp = NORMAL_HP;
				minScale = 0.1;
				maxScale = SCALE_MAX;
				smooth = defaults.smooth;
			}
			case "light" -> {
				mode = Mode.SQRT;
				normalHp = NORMAL_HP;
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

		normalHp = clamp(normalHp, 1, 1000);
		minScale = clamp(minScale, SCALE_MIN, SCALE_MAX);
		maxScale = clamp(maxScale, SCALE_MIN, SCALE_MAX);

		if (minScale > maxScale) {
			minScale = maxScale;
		}

		smooth = Math.max(0, Math.min(100, smooth));
		glowTinyBelow = clamp(glowTinyBelow, SCALE_MIN, SCALE_MAX);
		dragonScale = clamp(dragonScale, SCALE_MIN, SCALE_MAX);

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

		if (entity instanceof EnderDragon) {
			// A fixed size at full health (x8 by default), shrinking with the health left.
			return clamp(mode == Mode.MAX ? dragonScale : dragonScale * Math.min(1.0, health / maxHealth), minScale, maxScale);
		}

		double scale = switch (mode) {
			case HEALTH -> health / normalHp;
			case MAX -> maxHealth / normalHp;
			case PERCENT -> clamp(maxHealth / normalHp, minScale, maxScale) * Math.min(1.0, health / maxHealth);
			case SQRT -> Math.sqrt(health / normalHp);
		};

		return clamp(scale, minScale, maxScale);
	}

	private static double clamp(double value, double min, double max) {
		return Double.isNaN(value) ? min : Math.max(min, Math.min(max, value));
	}
}
