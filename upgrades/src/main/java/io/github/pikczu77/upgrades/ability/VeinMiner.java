package io.github.pikczu77.upgrades.ability;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.github.pikczu77.upgrades.config.UpgradesConfig;
import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

/**
 * Breaking a block also breaks every connected block of the same kind (diagonals count), up to the limit of the
 * current level. It works on every block, just like in the video (furnaces, crafting tables, netherrack...).
 */
public final class VeinMiner {
	private static boolean mining;

	private VeinMiner() {
	}

	public static void afterBreak(Level level, Player player, BlockPos origin, BlockState state, @Nullable BlockEntity blockEntity) {
		if (mining || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		int veinLevel = Upgrade.veinLevel(UpgradeManager.mask(serverPlayer));

		if (veinLevel <= 0 || state.isAir() || (serverPlayer.isShiftKeyDown() && UpgradesConfig.get().veinSneakSingle)) {
			return;
		}

		int limit = UpgradesConfig.get().veinSize(veinLevel) - 1;
		List<BlockPos> vein = collect(level, origin, state.getBlock(), limit);

		if (vein.isEmpty()) {
			return;
		}

		mining = true;

		try {
			for (BlockPos pos : vein) {
				// The tool can break halfway through, like the iron pickaxe in the video.
				if (!serverPlayer.isAlive()) {
					break;
				}

				serverPlayer.gameMode.destroyBlock(pos);
			}
		} finally {
			mining = false;
		}
	}

	/** Connected blocks of the same type, nearest first, without the origin. */
	private static List<BlockPos> collect(Level level, BlockPos origin, Block block, int limit) {
		List<BlockPos> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(origin);
		queue.add(origin);

		while (!queue.isEmpty() && found.size() < limit) {
			BlockPos current = queue.poll();

			for (int dx = -1; dx <= 1 && found.size() < limit; dx++) {
				for (int dy = -1; dy <= 1 && found.size() < limit; dy++) {
					for (int dz = -1; dz <= 1 && found.size() < limit; dz++) {
						BlockPos next = current.offset(dx, dy, dz);

						if (!seen.add(next) || !level.isLoaded(next)) {
							continue;
						}

						BlockState nextState = level.getBlockState(next);

						if (nextState.is(block) && nextState.getDestroySpeed(level, next) >= 0.0F) {
							found.add(next);
							queue.add(next);
						}
					}
				}
			}
		}

		return found;
	}

	public static boolean isMining() {
		return mining;
	}
}
