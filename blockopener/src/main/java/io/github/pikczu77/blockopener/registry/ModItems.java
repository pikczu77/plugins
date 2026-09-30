package io.github.pikczu77.blockopener.registry;

import io.github.pikczu77.blockopener.BlockOpener;
import io.github.pikczu77.blockopener.item.BedrockBucketItem;
import io.github.pikczu77.blockopener.item.BeeDrillItem;
import io.github.pikczu77.blockopener.item.BlockOpenerItem;
import io.github.pikczu77.blockopener.item.CopperMagnetItem;
import io.github.pikczu77.blockopener.item.DripstoneSwordItem;
import io.github.pikczu77.blockopener.item.MossphereItem;
import io.github.pikczu77.blockopener.item.PistonLauncherItem;
import io.github.pikczu77.blockopener.progress.SecretItem;
import java.util.function.Function;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorType;

public final class ModItems {
	/** Netherite-like material with +5 damage, so the sword hits for 9 like in the video. */
	public static final ToolMaterial DRIPSTONE = new ToolMaterial(
		BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 9.0F, 5.0F, 15, ItemTags.NETHERITE_TOOL_MATERIALS
	);

	public static final Item BLOCK_OPENER = register("block_opener", BlockOpenerItem::new, epic().stacksTo(1));

	public static final Item PUMPKIN_BOOTS = register(
		"pumpkin_boots", Item::new, unbreakable(epic().humanoidArmor(ModArmorMaterials.PUMPKIN, ArmorType.BOOTS))
	);
	public static final Item BEE_DRILL = register("bee_drill", BeeDrillItem::new, epic().stacksTo(1));
	public static final Item ANVIL_CHESTPLATE = register(
		"anvil_chestplate", Item::new, unbreakable(epic().humanoidArmor(ModArmorMaterials.ANVIL, ArmorType.CHESTPLATE))
	);
	public static final Item DIAMOND_LEGGINGS = register(
		"diamond_leggings", Item::new, unbreakable(epic().humanoidArmor(ModArmorMaterials.DIAMOND_FLIGHT, ArmorType.LEGGINGS))
	);
	public static final Item DRIPSTONE_SWORD = register(
		"dripstone_sword", DripstoneSwordItem::new, unbreakable(epic().sword(DRIPSTONE, 3.0F, -2.4F))
	);
	public static final Item PISTON_LAUNCHER = register("piston_launcher", PistonLauncherItem::new, epic().stacksTo(1));
	public static final Item COPPER_MAGNET = register("copper_magnet", CopperMagnetItem::new, epic().stacksTo(1));
	public static final Item BEDROCK_BUCKET = register("bedrock_bucket", BedrockBucketItem::new, epic().stacksTo(1));
	public static final Item SCULK_HELMET = register(
		"sculk_helmet", Item::new, unbreakable(epic().humanoidArmor(ModArmorMaterials.SCULK, ArmorType.HELMET))
	);
	public static final Item MOSSPHERE = register("mossphere", MossphereItem::new, epic().stacksTo(16));

	public static final CreativeModeTab TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		BlockOpener.id("block_opener"),
		FabricItemGroup.builder()
			.title(Component.translatable("itemGroup.blockopener"))
			.icon(() -> new ItemStack(BLOCK_OPENER))
			.displayItems((parameters, output) -> {
				output.accept(BLOCK_OPENER);
				for (SecretItem secret : SecretItem.values()) {
					output.accept(secret.item());
				}
			})
			.build()
	);

	private static Item.Properties epic() {
		return new Item.Properties().rarity(Rarity.EPIC).fireResistant();
	}

	/** Custom gear never breaks mid-recording. */
	private static Item.Properties unbreakable(Item.Properties properties) {
		return properties.component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BlockOpener.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void init() {
	}

	private ModItems() {
	}
}
