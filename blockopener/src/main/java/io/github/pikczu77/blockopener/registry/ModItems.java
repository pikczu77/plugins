package io.github.pikczu77.blockopener.registry;

import io.github.pikczu77.blockopener.BlockOpener;
import io.github.pikczu77.blockopener.item.BedrockBucketItem;
import io.github.pikczu77.blockopener.item.BeeDrillItem;
import io.github.pikczu77.blockopener.item.BlockOpenerItem;
import io.github.pikczu77.blockopener.item.CakeOfLifeItem;
import io.github.pikczu77.blockopener.item.CopperMagnetItem;
import io.github.pikczu77.blockopener.item.DripstoneSwordItem;
import io.github.pikczu77.blockopener.item.EnderGlovesItem;
import io.github.pikczu77.blockopener.item.GlassSpyglassItem;
import io.github.pikczu77.blockopener.item.HoneyBlasterItem;
import io.github.pikczu77.blockopener.item.IceWandItem;
import io.github.pikczu77.blockopener.item.MagmaFistItem;
import io.github.pikczu77.blockopener.item.MobCageItem;
import io.github.pikczu77.blockopener.item.MossphereItem;
import io.github.pikczu77.blockopener.item.ObsidianShieldItem;
import io.github.pikczu77.blockopener.item.PistonLauncherItem;
import io.github.pikczu77.blockopener.item.SizeMushroomItem;
import io.github.pikczu77.blockopener.item.SlimeGlovesItem;
import io.github.pikczu77.blockopener.item.StormHammerItem;
import io.github.pikczu77.blockopener.progress.SecretItem;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
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

	// Extended mode
	public static final Item SIZE_MUSHROOM = register("size_mushroom", SizeMushroomItem::new, epic().stacksTo(1));
	public static final Item WEEPING_TOTEM = register("weeping_totem", Item::new, epic().stacksTo(1));
	public static final Item MOB_CAGE = register("mob_cage", MobCageItem::new, epic().stacksTo(1));
	public static final Item STORM_HAMMER = register(
		"storm_hammer", StormHammerItem::new, unbreakable(epic().sword(ToolMaterial.NETHERITE, 5.0F, -3.0F))
	);
	public static final Item GLASS_SPYGLASS = register("glass_spyglass", GlassSpyglassItem::new, epic().stacksTo(1));
	public static final Item OBSIDIAN_SHIELD = register(
		"obsidian_shield", ObsidianShieldItem::new, unbreakable(epic().stacksTo(1)
			.equippableUnswappable(EquipmentSlot.OFFHAND)
			.component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
				0.0F,
				0.0F,
				// 360 degrees: it blocks every attack from every side (void, starving etc. still bypass it, like a shield).
				List.of(new BlocksAttacks.DamageReduction(180.0F, Optional.empty(), 0.0F, 1.0F)),
				new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
				Optional.of(DamageTypeTags.BYPASSES_SHIELD),
				Optional.of(SoundEvents.SHIELD_BLOCK),
				Optional.of(SoundEvents.SHIELD_BREAK)
			)))
	);
	public static final Item ENDER_GLOVES = register("ender_gloves", EnderGlovesItem::new, epic().stacksTo(1));
	public static final Item SLIME_GLOVES = register("slime_gloves", SlimeGlovesItem::new, epic().stacksTo(1));
	public static final Item ICE_WAND = register("ice_wand", IceWandItem::new, epic().stacksTo(1));
	public static final Item MAGMA_FIST = register(
		"magma_fist", MagmaFistItem::new, unbreakable(epic().sword(ToolMaterial.NETHERITE, 3.0F, -2.0F))
	);
	public static final Item CAKE_OF_LIFE = register(
		"cake_of_life", CakeOfLifeItem::new, epic().stacksTo(1).useCooldown(8.0F).food(
			new FoodProperties.Builder().nutrition(8).saturationModifier(1.0F).alwaysEdible().build(),
			Consumable.builder()
				.consumeSeconds(0.8F)
				.animation(ItemUseAnimation.EAT)
				.sound(SoundEvents.GENERIC_EAT)
				.hasConsumeParticles(true)
				.onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
					new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 1),
					new MobEffectInstance(MobEffects.REGENERATION, 100, 1),
					new MobEffectInstance(MobEffects.SATURATION, 20, 0)
				)))
				.build()
		)
	);
	public static final Item HONEY_BLASTER = register("honey_blaster", HoneyBlasterItem::new, epic().stacksTo(1));

	public static final CreativeModeTab TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		BlockOpener.id("block_opener"),
		FabricItemGroup.builder()
			.title(Component.translatable("itemGroup.blockopener"))
			.icon(() -> new ItemStack(BLOCK_OPENER))
			.displayItems((parameters, output) -> {
				output.accept(BLOCK_OPENER);
				for (SecretItem secret : SecretItem.ALL) {
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
