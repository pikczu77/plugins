package io.github.pikczu77.reckit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

/** Registers the catalog: one item per entry and one creative tab per pack. */
public final class PropItems {
	private static final Map<String, List<Item>> BY_PACK = new LinkedHashMap<>();
	private static final List<Item> ALL = new ArrayList<>();

	private PropItems() {
	}

	static void register(Catalog catalog) {
		for (Catalog.Pack pack : catalog.packs()) {
			List<Item> items = pack.items().stream().map(PropItems::register).toList();
			BY_PACK.put(pack.id(), items);
			ALL.addAll(items);

			ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Reckit.id(pack.id()));
			Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key, FabricItemGroup.builder()
					.title(Component.translatable("itemGroup.reckit." + pack.id()))
					.icon(() -> new ItemStack(items.getFirst()))
					.displayItems((parameters, output) -> items.forEach(output::accept))
					.build());
		}
	}

	private static Item register(Catalog.Entry entry) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Reckit.id(entry.id()));
		Item.Properties properties = new Item.Properties().setId(key).stacksTo(entry.stack());

		if (entry.glint()) {
			properties.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}

		return Registry.register(BuiltInRegistries.ITEM, key, new PropItem(properties, entry.color()));
	}

	/** Items of each pack, in catalog order. */
	public static Map<String, List<Item>> byPack() {
		return Collections.unmodifiableMap(BY_PACK);
	}

	public static List<Item> all() {
		return Collections.unmodifiableList(ALL);
	}
}
