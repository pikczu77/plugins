package io.github.pikczu77.reckit.test;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.pikczu77.reckit.Catalog;
import io.github.pikczu77.reckit.PropItems;
import io.github.pikczu77.reckit.Reckit;

public class ReckitGameTest {
	/** Every catalog entry is a registered item, and every pack has its creative tab. */
	@GameTest
	public void catalogIsRegistered(GameTestHelper helper) {
		Catalog catalog = Catalog.load();

		for (Catalog.Pack pack : catalog.packs()) {
			helper.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(Reckit.id(pack.id())), "Missing creative tab " + pack.id());
			List<Item> items = PropItems.byPack().get(pack.id());
			helper.assertTrue(items != null && items.size() == pack.items().size(), "Pack " + pack.id() + " is not fully registered");

			for (Catalog.Entry entry : pack.items()) {
				Item item = BuiltInRegistries.ITEM.getValue(Reckit.id(entry.id()));
				helper.assertTrue(items.contains(item), "Missing item " + entry.id());
				ItemStack stack = new ItemStack(item);
				helper.assertTrue(stack.getMaxStackSize() == entry.stack(), "Wrong stack size of " + entry.id());
				helper.assertTrue(stack.hasFoil() == entry.glint(), "Wrong glint of " + entry.id());
				helper.assertTrue(stack.has(DataComponents.EQUIPPABLE) == entry.head(), "Wrong head slot of " + entry.id());
			}
		}

		helper.succeed();
	}
}
