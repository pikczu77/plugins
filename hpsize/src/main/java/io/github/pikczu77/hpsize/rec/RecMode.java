package io.github.pikczu77.hpsize.rec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.jspecify.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelResource;

import io.github.pikczu77.hpsize.HpSize;

/**
 * "Recording mode": hides command feedback and admin command logs in the chat, so typing commands
 * during a take does not spam the recording. The previous game rule values are restored afterwards.
 */
public final class RecMode {
	private static final Gson GSON = new Gson();
	private static final Map<String, GameRule<Boolean>> RULES = new LinkedHashMap<>();

	static {
		RULES.put("send_command_feedback", GameRules.SEND_COMMAND_FEEDBACK);
		RULES.put("log_admin_commands", GameRules.LOG_ADMIN_COMMANDS);
	}

	private static @Nullable Path file;
	private static @Nullable Map<String, Boolean> saved;

	private RecMode() {
	}

	public static boolean isActive() {
		return saved != null;
	}

	public static boolean enable(MinecraftServer server) {
		if (saved != null) {
			return false;
		}

		GameRules rules = server.overworld().getGameRules();
		Map<String, Boolean> previous = new LinkedHashMap<>();

		RULES.forEach((key, rule) -> {
			previous.put(key, rules.get(rule));
			rules.set(rule, false, server);
		});

		saved = previous;
		write();
		return true;
	}

	public static boolean disable(MinecraftServer server) {
		if (saved == null) {
			return false;
		}

		GameRules rules = server.overworld().getGameRules();

		saved.forEach((key, value) -> {
			GameRule<Boolean> rule = RULES.get(key);

			if (rule != null) {
				rules.set(rule, value, server);
			}
		});

		saved = null;
		write();
		return true;
	}

	public static void load(MinecraftServer server) {
		file = server.getWorldPath(LevelResource.ROOT).resolve("hpsize-recmode.json");
		saved = null;

		if (!Files.exists(file)) {
			return;
		}

		try {
			JsonObject json = GSON.fromJson(Files.readString(file), JsonObject.class);

			if (json != null && json.has("previous")) {
				Map<String, Boolean> previous = new LinkedHashMap<>();
				json.getAsJsonObject("previous").entrySet().forEach(entry -> previous.put(entry.getKey(), entry.getValue().getAsBoolean()));
				saved = previous;
			}
		} catch (IOException | JsonParseException | IllegalStateException e) {
			HpSize.LOGGER.error("Could not read {}", file, e);
		}
	}

	public static void unload() {
		file = null;
		saved = null;
	}

	private static void write() {
		if (file == null) {
			return;
		}

		try {
			if (saved == null) {
				Files.deleteIfExists(file);
			} else {
				JsonObject json = new JsonObject();
				JsonObject previous = new JsonObject();
				saved.forEach(previous::addProperty);
				json.add("previous", previous);
				Files.writeString(file, GSON.toJson(json));
			}
		} catch (IOException e) {
			HpSize.LOGGER.error("Could not save {}", file, e);
		}
	}
}
