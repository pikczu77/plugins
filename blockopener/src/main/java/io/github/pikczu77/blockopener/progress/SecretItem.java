package io.github.pikczu77.blockopener.progress;

import io.github.pikczu77.blockopener.registry.ModItems;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The secret custom items. The first 10 are the ones from the video, in the order its tracker shows
 * them; the rest only drop in extended mode ({@code /bo settings extendedItems true}).
 * {@link #sourceBlocks} is only used by commands (locate / test row); what a block actually drops
 * is decided by the loot tables in {@code data/blockopener/loot_table/opening} and {@code opening_extended}.
 */
public enum SecretItem {
	PUMPKIN_BOOTS("pumpkin_boots", 0xFF8A1E, () -> ModItems.PUMPKIN_BOOTS,
		List.of(Blocks.PUMPKIN, Blocks.CARVED_PUMPKIN, Blocks.JACK_O_LANTERN)),
	BEE_DRILL("bee_drill", 0xFFC928, () -> ModItems.BEE_DRILL,
		List.of(Blocks.BEEHIVE, Blocks.BEE_NEST)),
	ANVIL_CHESTPLATE("anvil_chestplate", 0x9A9A9A, () -> ModItems.ANVIL_CHESTPLATE,
		List.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL)),
	DIAMOND_LEGGINGS("diamond_leggings", 0x4AEDD9, () -> ModItems.DIAMOND_LEGGINGS,
		List.of(Blocks.DIAMOND_BLOCK)),
	DRIPSTONE_SWORD("dripstone_sword", 0xB08A6E, () -> ModItems.DRIPSTONE_SWORD,
		List.of(Blocks.DRIPSTONE_BLOCK)),
	PISTON_LAUNCHER("piston_launcher", 0xC6A26B, () -> ModItems.PISTON_LAUNCHER,
		List.of(Blocks.PISTON, Blocks.STICKY_PISTON)),
	COPPER_MAGNET("copper_magnet", 0xE0754F, () -> ModItems.COPPER_MAGNET,
		List.of(Blocks.COPPER_BLOCK, Blocks.EXPOSED_COPPER, Blocks.WEATHERED_COPPER, Blocks.OXIDIZED_COPPER,
			Blocks.WAXED_COPPER_BLOCK, Blocks.WAXED_EXPOSED_COPPER, Blocks.WAXED_WEATHERED_COPPER, Blocks.WAXED_OXIDIZED_COPPER)),
	BEDROCK_BUCKET("bedrock_bucket", 0x6B6B6B, () -> ModItems.BEDROCK_BUCKET,
		List.of(Blocks.BEDROCK)),
	SCULK_HELMET("sculk_helmet", 0x1FB5C4, () -> ModItems.SCULK_HELMET,
		List.of(Blocks.SCULK_SHRIEKER, Blocks.SCULK_CATALYST)),
	MOSSPHERE("mossphere", 0x7FC241, () -> ModItems.MOSSPHERE,
		List.of(Blocks.MOSS_BLOCK)),

	// Extended mode
	SIZE_MUSHROOM("size_mushroom", 0xE03C3C, () -> ModItems.SIZE_MUSHROOM,
		List.of(Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.MUSHROOM_STEM), true),
	WEEPING_TOTEM("weeping_totem", 0xA05CFF, () -> ModItems.WEEPING_TOTEM,
		List.of(Blocks.CRYING_OBSIDIAN), true),
	MOB_CAGE("mob_cage", 0x7A9CC0, () -> ModItems.MOB_CAGE,
		List.of(Blocks.SPAWNER, Blocks.TRIAL_SPAWNER), true),
	STORM_HAMMER("storm_hammer", 0x7FD4FF, () -> ModItems.STORM_HAMMER,
		List.of(Blocks.LIGHTNING_ROD, Blocks.EXPOSED_LIGHTNING_ROD, Blocks.WEATHERED_LIGHTNING_ROD, Blocks.OXIDIZED_LIGHTNING_ROD,
			Blocks.WAXED_LIGHTNING_ROD, Blocks.WAXED_EXPOSED_LIGHTNING_ROD, Blocks.WAXED_WEATHERED_LIGHTNING_ROD, Blocks.WAXED_OXIDIZED_LIGHTNING_ROD), true),
	GLASS_SPYGLASS("glass_spyglass", 0xBFE9FF, () -> ModItems.GLASS_SPYGLASS,
		List.of(Blocks.GLASS, Blocks.TINTED_GLASS), true),
	OBSIDIAN_SHIELD("obsidian_shield", 0x8A5CD0, () -> ModItems.OBSIDIAN_SHIELD,
		List.of(Blocks.OBSIDIAN), true),
	ENDER_GLOVES("ender_gloves", 0xD6F07A, () -> ModItems.ENDER_GLOVES,
		List.of(Blocks.END_STONE, Blocks.END_STONE_BRICKS), true),
	SLIME_GLOVES("slime_gloves", 0x7ED957, () -> ModItems.SLIME_GLOVES,
		List.of(Blocks.SLIME_BLOCK), true),
	ICE_WAND("ice_wand", 0x8FD3FF, () -> ModItems.ICE_WAND,
		List.of(Blocks.BLUE_ICE, Blocks.PACKED_ICE), true),
	MAGMA_FIST("magma_fist", 0xFF6A1A, () -> ModItems.MAGMA_FIST,
		List.of(Blocks.MAGMA_BLOCK), true),
	CAKE_OF_LIFE("cake_of_life", 0xFF9EC4, () -> ModItems.CAKE_OF_LIFE,
		List.of(Blocks.CAKE), true),
	HONEY_BLASTER("honey_blaster", 0xF9B233, () -> ModItems.HONEY_BLASTER,
		List.of(Blocks.HONEY_BLOCK), true);

	/** All items, video ones first. */
	public static final List<SecretItem> ALL = List.of(values());
	/** The 10 items of the video. */
	public static final List<SecretItem> VIDEO = ALL.stream().filter(secret -> !secret.extended).toList();
	/** Kept for the video tracker: 10. */
	public static final int COUNT = VIDEO.size();

	private final String id;
	private final int color;
	private final Supplier<Item> item;
	private final List<Block> sourceBlocks;
	private final boolean extended;

	SecretItem(String id, int color, Supplier<Item> item, List<Block> sourceBlocks) {
		this(id, color, item, sourceBlocks, false);
	}

	SecretItem(String id, int color, Supplier<Item> item, List<Block> sourceBlocks, boolean extended) {
		this.id = id;
		this.color = color;
		this.item = item;
		this.sourceBlocks = sourceBlocks;
		this.extended = extended;
	}

	/** The items in play: the video's 10, or all of them in extended mode. */
	public static List<SecretItem> active(boolean extendedMode) {
		return extendedMode ? ALL : VIDEO;
	}

	public boolean extended() {
		return this.extended;
	}

	public String id() {
		return this.id;
	}

	/** Theme colour used for chat messages, titles and particles. */
	public int color() {
		return this.color;
	}

	public Item item() {
		return this.item.get();
	}

	public List<Block> sourceBlocks() {
		return this.sourceBlocks;
	}

	public Block mainSourceBlock() {
		return this.sourceBlocks.getFirst();
	}

	public Identifier itemId() {
		return BuiltInRegistries.ITEM.getKey(this.item());
	}

	public static Optional<SecretItem> byId(String id) {
		for (SecretItem secret : values()) {
			if (secret.id.equals(id)) {
				return Optional.of(secret);
			}
		}
		return Optional.empty();
	}

	public static Optional<SecretItem> of(ItemStack stack) {
		for (SecretItem secret : values()) {
			if (stack.is(secret.item())) {
				return Optional.of(secret);
			}
		}
		return Optional.empty();
	}
}
