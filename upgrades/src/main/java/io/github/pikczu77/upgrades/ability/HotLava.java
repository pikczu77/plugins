package io.github.pikczu77.upgrades.ability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import io.github.pikczu77.upgrades.config.UpgradesConfig;

/**
 * Temporary lava placed by the hot hands. It is removed after a few seconds (only if it is still our lava source).
 */
public final class HotLava {
	private record Placed(ResourceKey<Level> dimension, BlockPos pos, long removeAt) {
	}

	private static final List<Placed> PLACED = new ArrayList<>();

	private HotLava() {
	}

	public static void placeUnder(ServerLevel level, LivingEntity victim) {
		BlockPos pos = victim.blockPosition();
		BlockState state = level.getBlockState(pos);

		if (!state.canBeReplaced() || !state.getFluidState().isEmpty()) {
			return;
		}

		level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
		level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 1.0F, 1.0F);
		PLACED.add(new Placed(level.dimension(), pos.immutable(), level.getGameTime() + UpgradesConfig.get().lavaSeconds * 20L));
	}

	public static void tick(MinecraftServer server) {
		Iterator<Placed> iterator = PLACED.iterator();

		while (iterator.hasNext()) {
			Placed placed = iterator.next();
			ServerLevel level = server.getLevel(placed.dimension());

			if (level == null) {
				iterator.remove();
				continue;
			}

			if (level.getGameTime() < placed.removeAt()) {
				continue;
			}

			iterator.remove();

			if (level.isLoaded(placed.pos()) && level.getBlockState(placed.pos()).is(Blocks.LAVA)
					&& level.getFluidState(placed.pos()).isSource()) {
				level.setBlock(placed.pos(), Blocks.AIR.defaultBlockState(), 3);
				level.playSound(null, placed.pos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.4F);
			}
		}
	}

	public static void reset() {
		PLACED.clear();
	}
}
