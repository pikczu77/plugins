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
 * The 10 secret custom items, in the order the video's tracker shows them.
 * {@link #sourceBlocks} is only used by commands (locate / test row); what a block actually drops
 * is decided by the loot tables in {@code data/blockopener/loot_table/opening}.
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
		List.of(Blocks.MOSS_BLOCK));

	public static final int COUNT = values().length;

	private final String id;
	private final int color;
	private final Supplier<Item> item;
	private final List<Block> sourceBlocks;

	SecretItem(String id, int color, Supplier<Item> item, List<Block> sourceBlocks) {
		this.id = id;
		this.color = color;
		this.item = item;
		this.sourceBlocks = sourceBlocks;
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
