package io.github.pikczu77.blockopener.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blocks that disappear on their own: the Obsidian Shield dome, Slime Gloves platform, Ice Wand shell
 * and Magma Fist lava crust. They never drop anything (breaking or blowing them up just removes them),
 * cannot be opened (no free secret items), and are all reverted when the server stops so nothing is left behind.
 */
public final class TempBlocks {
	private static final List<Temp> ACTIVE = new ArrayList<>();
	private static final Map<Level, Map<BlockPos, Temp>> BY_POS = new HashMap<>();

	/** Places into empty space only. @return true if the block was placed */
	public static boolean place(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
		BlockState current = level.getBlockState(pos);
		if (!current.isAir() && !(current.canBeReplaced() && current.getFluidState().isEmpty())) {
			return false;
		}
		return put(level, pos, state, Blocks.AIR.defaultBlockState(), ticks);
	}

	/** Covers a fluid source block, which comes back when the temporary block expires. */
	public static boolean placeOverSource(ServerLevel level, BlockPos pos, BlockState state, Block fluidBlock, int ticks) {
		BlockState current = level.getBlockState(pos);
		if (!current.is(fluidBlock) || !current.getFluidState().isSource()) {
			return false;
		}
		return put(level, pos, state, current, ticks);
	}

	private static boolean put(ServerLevel level, BlockPos pos, BlockState state, BlockState restore, int ticks) {
		if (isTemporary(level, pos)) {
			return false;
		}
		level.setBlock(pos, state, Block.UPDATE_ALL);
		Temp temp = new Temp(level, pos.immutable(), state, restore, level.getGameTime() + ticks);
		ACTIVE.add(temp);
		BY_POS.computeIfAbsent(level, l -> new HashMap<>()).put(temp.pos, temp);
		return true;
	}

	public static boolean isTemporary(Level level, BlockPos pos) {
		Map<BlockPos, Temp> map = BY_POS.get(level);
		return map != null && map.containsKey(pos);
	}

	/** Keeps a temporary block around for at least {@code ticks} more ticks. */
	static void refresh(Level level, BlockPos pos, int ticks) {
		Map<BlockPos, Temp> map = BY_POS.get(level);
		Temp temp = map == null ? null : map.get(pos);
		if (temp != null) {
			temp.until = Math.max(temp.until, level.getGameTime() + ticks);
		}
	}

	/** Removes a temporary block right away, without drops. @return false if it was not temporary */
	public static boolean remove(Level level, BlockPos pos) {
		Map<BlockPos, Temp> map = BY_POS.get(level);
		Temp temp = map == null ? null : map.get(pos);
		if (temp == null) {
			return false;
		}
		ACTIVE.remove(temp);
		revert(temp, true);
		return true;
	}

	static void tick() {
		Iterator<Temp> iterator = ACTIVE.iterator();
		while (iterator.hasNext()) {
			Temp temp = iterator.next();
			if (temp.level.getGameTime() >= temp.until) {
				iterator.remove();
				revert(temp, true);
			}
		}
	}

	static void clear() {
		ACTIVE.forEach(temp -> revert(temp, false));
		ACTIVE.clear();
		BY_POS.clear();
	}

	private static void revert(Temp temp, boolean effects) {
		Map<BlockPos, Temp> map = BY_POS.get(temp.level);
		if (map != null) {
			map.remove(temp.pos);
		}
		if (temp.level.getBlockState(temp.pos) == temp.state) {
			temp.level.setBlock(temp.pos, temp.restore, Block.UPDATE_ALL);
			if (effects) {
				temp.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, temp.state),
					temp.pos.getX() + 0.5, temp.pos.getY() + 0.5, temp.pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.0);
			}
		}
	}

	private static final class Temp {
		final ServerLevel level;
		final BlockPos pos;
		final BlockState state;
		final BlockState restore;
		long until;

		Temp(ServerLevel level, BlockPos pos, BlockState state, BlockState restore, long until) {
			this.level = level;
			this.pos = pos;
			this.state = state;
			this.restore = restore;
			this.until = until;
		}
	}

	private TempBlocks() {
	}
}
