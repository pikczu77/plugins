package io.github.pikczu77.upgrades.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.jspecify.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import io.github.pikczu77.upgrades.Upgrades;
import io.github.pikczu77.upgrades.upgrade.Upgrade;

/**
 * Per-world settings, stored in {@code <world>/upgrades.json}. The defaults play like the video.
 */
public final class UpgradesConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static @Nullable UpgradesConfig current;
	private static @Nullable Path file;

	public enum PowerMode {
		/** One power per input, the newest unlocked one is picked automatically and a key cycles them. */
		SELECT("select", "jedna moc na przycisk (najnowsza, klawisz V zmienia)"),
		/** Every unlocked power bound to the input fires at once. */
		ALL("all", "wszystkie moce naraz");

		public final String id;
		public final String description;

		PowerMode(String id, String description) {
			this.id = id;
			this.description = description;
		}

		public static @Nullable PowerMode byId(String id) {
			for (PowerMode mode : values()) {
				if (mode.id.equals(id.toLowerCase(Locale.ROOT))) {
					return mode;
				}
			}

			return null;
		}
	}

	public boolean enabled = true;
	/** Upgrades turned off with {@code /upgrades disable}. */
	public Set<String> disabled = new LinkedHashSet<>();
	public PowerMode powerMode = PowerMode.SELECT;
	/** Show the big "UPGRADE UNLOCKED!" title. */
	public boolean titles = true;
	/** Vein miner block limits for levels 1-4. */
	public int[] veinSizes = {16, 48, 128, 512};
	/** Sneaking mines a single block (off = vein mining always, like in the video). */
	public boolean veinSneakSingle = true;
	/** Most combat clones one player can have at once. */
	public int maxClones = 40;
	/** Most clones the multiplicity upgrade breeds per player (the video had no limit and the world broke). */
	public int multiplicityLimit = 150;
	/** Drops multiplier range of the enchanted body. */
	public int dropsMin = 2;
	public int dropsMax = 5;
	/** Chance (percent) that the eye teleport lands at the end portal. */
	public int eyePortalChance = 35;
	/** Ticks of holding sneak before the eye teleport fires. */
	public int eyeChargeTicks = 40;
	/** Obsidian throw explosion power. */
	public float obsidianPower = 4.0F;
	/** Seconds the hot hands lava stays. */
	public int lavaSeconds = 4;

	public boolean isEnabled(Upgrade upgrade) {
		return this.enabled && !this.disabled.contains(upgrade.id());
	}

	public int veinSize(int level) {
		if (level <= 0) {
			return 1;
		}

		int index = Math.min(level, this.veinSizes.length) - 1;
		return Math.max(1, this.veinSizes[index]);
	}

	public static UpgradesConfig get() {
		if (current == null) {
			current = new UpgradesConfig();
		}

		return current;
	}

	public static void load(MinecraftServer server) {
		file = server.getWorldPath(LevelResource.ROOT).resolve("upgrades.json");
		current = new UpgradesConfig();

		if (Files.exists(file)) {
			try {
				UpgradesConfig loaded = GSON.fromJson(Files.readString(file), UpgradesConfig.class);

				if (loaded != null) {
					current = loaded.sanitized();
				}
			} catch (IOException | JsonParseException e) {
				Upgrades.LOGGER.warn("Could not read {}, using the defaults", file, e);
			}
		}
	}

	public static void save() {
		if (file == null || current == null) {
			return;
		}

		try {
			Files.writeString(file, GSON.toJson(current));
		} catch (IOException e) {
			Upgrades.LOGGER.warn("Could not save {}", file, e);
		}
	}

	public static void unload() {
		current = null;
		file = null;
	}

	private UpgradesConfig sanitized() {
		UpgradesConfig defaults = new UpgradesConfig();

		if (this.disabled == null) {
			this.disabled = new LinkedHashSet<>();
		}

		if (this.powerMode == null) {
			this.powerMode = defaults.powerMode;
		}

		if (this.veinSizes == null || this.veinSizes.length == 0) {
			this.veinSizes = defaults.veinSizes;
		}

		this.maxClones = Math.max(0, this.maxClones);
		this.multiplicityLimit = Math.max(0, this.multiplicityLimit);
		this.dropsMin = Math.max(1, this.dropsMin);
		this.dropsMax = Math.max(this.dropsMin, this.dropsMax);
		this.eyePortalChance = Math.clamp(this.eyePortalChance, 0, 100);
		this.eyeChargeTicks = Math.max(5, this.eyeChargeTicks);
		this.lavaSeconds = Math.max(1, this.lavaSeconds);
		return this;
	}
}
