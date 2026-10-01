package io.github.pikczu77.reckit;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;

import net.fabricmc.loader.api.FabricLoader;

/**
 * The list of props, read from {@code reckit/catalog.json} in the mod jar. Each pack becomes a creative tab and each
 * entry an item {@code reckit:<id>}; the models, textures and names live in {@code assets/reckit}.
 */
public record Catalog(List<Pack> packs) {
	public static final String PATH = "reckit/catalog.json";

	/** A source datapack/resource pack, shown as a creative tab with the {@code icon} item (the first item by default). */
	public record Pack(String id, String icon, List<Entry> items) {
	}

	/**
	 * One prop.
	 *
	 * @param stack max stack size (1-99)
	 * @param glint whether the item always has the enchantment glint
	 * @param color RGB color of the item name, or null for the default white
	 * @param bold whether the item name is bold
	 * @param head whether the item can be worn on the head (right click), shown with its "head" model transform
	 */
	public record Entry(String id, int stack, boolean glint, @Nullable Integer color, boolean bold, boolean head) {
	}

	public static Catalog load() {
		Path path = FabricLoader.getInstance().getModContainer(Reckit.MOD_ID).orElseThrow().findPath(PATH)
				.orElseThrow(() -> new IllegalStateException("Missing " + PATH));

		try (Reader reader = Files.newBufferedReader(path)) {
			return parse(JsonParser.parseReader(reader).getAsJsonObject());
		} catch (IOException e) {
			throw new IllegalStateException("Could not read " + PATH, e);
		}
	}

	static Catalog parse(JsonObject json) {
		List<Pack> packs = new ArrayList<>();
		Set<String> packIds = new HashSet<>();
		Set<String> itemIds = new HashSet<>();

		for (JsonElement packElement : json.getAsJsonArray("packs")) {
			JsonObject packJson = packElement.getAsJsonObject();
			String packId = validId(packJson.get("id").getAsString(), "pack");
			require(packIds.add(packId), "Duplicate pack id " + packId);
			List<Entry> items = new ArrayList<>();

			for (JsonElement itemElement : packJson.getAsJsonArray("items")) {
				JsonObject itemJson = itemElement.getAsJsonObject();
				String id = validId(itemJson.get("id").getAsString(), "item");
				require(itemIds.add(id), "Duplicate item id " + id + " (pack " + packId + ")");
				int stack = itemJson.has("stack") ? itemJson.get("stack").getAsInt() : 64;
				require(stack >= 1 && stack <= 99, "Stack size of " + id + " must be 1-99");
				boolean glint = itemJson.has("glint") && itemJson.get("glint").getAsBoolean();
				Integer color = itemJson.has("color") ? Integer.parseInt(itemJson.get("color").getAsString().substring(1), 16) : null;
				boolean bold = itemJson.has("bold") && itemJson.get("bold").getAsBoolean();
				boolean head = itemJson.has("head") && itemJson.get("head").getAsBoolean();
				items.add(new Entry(id, stack, glint, color, bold, head));
			}

			require(!items.isEmpty(), "Pack " + packId + " has no items");
			String icon = packJson.has("icon") ? packJson.get("icon").getAsString() : items.getFirst().id();
			require(items.stream().anyMatch(item -> item.id().equals(icon)), "Icon " + icon + " of pack " + packId + " is not one of its items");
			packs.add(new Pack(packId, icon, List.copyOf(items)));
		}

		return new Catalog(List.copyOf(packs));
	}

	private static String validId(String id, String what) {
		require(Identifier.isValidPath(id) && !id.contains("/"), "Invalid " + what + " id '" + id + "' (use a-z, 0-9, _ . -)");
		return id;
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(PATH + ": " + message);
		}
	}
}
