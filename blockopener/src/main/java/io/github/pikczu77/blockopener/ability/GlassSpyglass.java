package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.command.Markers;
import io.github.pikczu77.blockopener.progress.Progress;
import io.github.pikczu77.blockopener.progress.SecretItem;
import io.github.pikczu77.blockopener.registry.ModItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Glass Spyglass: while you look through it, ores and the blocks hiding secret items you have not
 * found yet glow through walls (visible for everyone, so it shows up in every recording angle).
 */
public final class GlassSpyglass {
	private static final int RADIUS = 24;
	private static final int MAX_MARKERS = 48;
	private static final Map<TagKey<Block>, Integer> ORE_COLORS = Map.of(
		BlockTags.COAL_ORES, 0x3A3A3A,
		BlockTags.IRON_ORES, 0xD8AF93,
		BlockTags.COPPER_ORES, 0xE0754F,
		BlockTags.GOLD_ORES, 0xFCEE4B,
		BlockTags.REDSTONE_ORES, 0xFF2020,
		BlockTags.LAPIS_ORES, 0x2D5BD4,
		BlockTags.DIAMOND_ORES, 0x4AEDD9,
		BlockTags.EMERALD_ORES, 0x17DD62
	);

	static void tick(ServerPlayer player) {
		if (!player.isUsingItem() || !player.getUseItem().is(ModItems.GLASS_SPYGLASS) || player.tickCount % 20 != 0) {
			return;
		}
		ServerLevel level = player.level();
		Map<Block, Integer> colors = colors(player);
		BlockPos origin = player.blockPosition();
		List<BlockPos> found = new ArrayList<>();
		ChunkPos center = new ChunkPos(origin);
		int chunks = (RADIUS >> 4) + 1;
		for (int dx = -chunks; dx <= chunks; dx++) {
			for (int dz = -chunks; dz <= chunks; dz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
				if (chunk == null) {
					continue;
				}
				LevelChunkSection[] sections = chunk.getSections();
				for (int index = 0; index < sections.length; index++) {
					LevelChunkSection section = sections[index];
					int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
					if (section.hasOnlyAir() || Math.abs(baseY + 8 - origin.getY()) > RADIUS + 8
						|| !section.maybeHas(state -> colors.containsKey(state.getBlock()))) {
						continue;
					}
					for (int y = 0; y < 16; y++) {
						for (int z = 0; z < 16; z++) {
							for (int x = 0; x < 16; x++) {
								if (colors.containsKey(section.getBlockState(x, y, z).getBlock())) {
									BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
									if (pos.distSqr(origin) <= RADIUS * RADIUS) {
										found.add(pos);
									}
								}
							}
						}
					}
				}
			}
		}
		found.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
		for (BlockPos pos : found.subList(0, Math.min(MAX_MARKERS, found.size()))) {
			BlockState state = level.getBlockState(pos);
			Markers.highlight(level, pos, state, colors.getOrDefault(state.getBlock(), 0xFFFFFF), 22, true);
		}
	}

	/** Ore colours plus the source blocks of every secret item this player still has to find. */
	private static Map<Block, Integer> colors(ServerPlayer player) {
		Map<Block, Integer> colors = new HashMap<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			BlockState state = block.defaultBlockState();
			for (Map.Entry<TagKey<Block>, Integer> entry : ORE_COLORS.entrySet()) {
				if (state.is(entry.getKey())) {
					colors.put(block, entry.getValue());
				}
			}
		}
		colors.put(Blocks.ANCIENT_DEBRIS, 0x7A4A3A);
		colors.put(Blocks.NETHER_QUARTZ_ORE, 0xEEE6DA);
		for (SecretItem secret : Progress.active(player.level().getServer())) {
			if (!Progress.hasFound(player, secret)) {
				secret.sourceBlocks().forEach(block -> colors.put(block, secret.color()));
			}
		}
		return colors;
	}

	private GlassSpyglass() {
	}
}
