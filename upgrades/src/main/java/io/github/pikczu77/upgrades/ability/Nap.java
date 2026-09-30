package io.github.pikczu77.upgrades.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import io.github.pikczu77.upgrades.util.Screens;
import io.github.pikczu77.upgrades.util.TempDisplays;

/**
 * The bed on the back: sneak + right-click with an empty hand pulls out a big bed and starts a nap, at any time of
 * day. Sleeping for the full duration heals completely. The bed disappears when you wake up.
 */
public final class Nap {
	/** Footprint of the big bed: blocks along the facing direction, and to each side. */
	private static final int LENGTH = 4;
	private static final int HALF_WIDTH = 1;

	private static final class Napping {
		final ResourceKey<Level> dimension;
		final BlockPos foot;
		final BlockPos head;
		final List<UUID> displays;
		boolean healed;

		Napping(ResourceKey<Level> dimension, BlockPos foot, BlockPos head, List<UUID> displays) {
			this.dimension = dimension;
			this.foot = foot;
			this.head = head;
			this.displays = displays;
		}
	}

	private static final Map<UUID, Napping> NAPPING = new HashMap<>();

	private Nap() {
	}

	public static boolean isNapping(Player player) {
		return NAPPING.containsKey(player.getUUID());
	}

	public static void start(ServerPlayer player) {
		if (isNapping(player) || player.isSleeping() || player.isPassenger()) {
			return;
		}

		ServerLevel level = player.level();
		Direction facing = player.getDirection();
		BlockPos foot = player.blockPosition();
		BlockPos head = foot.relative(facing);

		if (!player.onGround() || !hasSpace(level, foot, facing)) {
			Screens.actionBar(player, Component.literal("Za mało miejsca na duże łóżko!").withStyle(ChatFormatting.RED));
			return;
		}

		int flags = Block.UPDATE_ALL;
		BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, facing);
		level.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), flags);
		level.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD), flags);

		List<UUID> displays = buildBigBed(level, foot, facing);
		NAPPING.put(player.getUUID(), new Napping(level.dimension(), foot.immutable(), head.immutable(), displays));
		player.startSleeping(head);
		level.playSound(null, foot, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
	}

	private static boolean hasSpace(ServerLevel level, BlockPos foot, Direction facing) {
		Direction side = facing.getClockWise();

		for (int along = -1; along < LENGTH - 1; along++) {
			for (int across = -HALF_WIDTH; across <= HALF_WIDTH; across++) {
				BlockPos pos = foot.relative(facing, along).relative(side, across);

				for (int up = 0; up < 2; up++) {
					BlockState state = level.getBlockState(pos.above(up));

					if (!state.canBeReplaced() || !state.getFluidState().isEmpty()) {
						return false;
					}
				}
			}
		}

		BlockPos head = foot.relative(facing);
		return level.getBlockState(foot.below()).isFaceSturdy(level, foot.below(), Direction.UP)
				&& level.getBlockState(head.below()).isFaceSturdy(level, head.below(), Direction.UP);
	}

	/**
	 * The big bed around the real (hidden) one, 3 blocks wide and 4 long: a wooden frame with a headboard, a white
	 * mattress, a pillow and a red blanket.
	 */
	private static List<UUID> buildBigBed(ServerLevel level, BlockPos foot, Direction facing) {
		List<UUID> ids = new ArrayList<>();
		Frame frame = new Frame(foot, facing);
		ids.add(frame.box(level, Blocks.STRIPPED_OAK_WOOD.defaultBlockState(), 0.0, 4.0, 0.0, 3.0, 0.0, 0.3));
		ids.add(frame.box(level, Blocks.WHITE_WOOL.defaultBlockState(), 0.05, 3.95, 0.05, 2.95, 0.3, 0.62));
		ids.add(frame.box(level, Blocks.RED_WOOL.defaultBlockState(), -0.02, 2.7, -0.02, 3.02, 0.25, 0.68));
		ids.add(frame.box(level, Blocks.WHITE_WOOL.defaultBlockState(), 3.0, 3.75, 0.35, 2.65, 0.62, 0.78));
		ids.add(frame.box(level, Blocks.STRIPPED_OAK_WOOD.defaultBlockState(), 3.8, 4.0, -0.05, 3.05, 0.0, 1.25));
		return ids;
	}

	/** Maps "along the bed / across the bed" coordinates to world boxes. */
	private record Frame(BlockPos foot, Direction facing) {
		UUID box(ServerLevel level, BlockState state, double along0, double along1, double across0, double across1, double y0, double y1) {
			Direction side = this.facing.getClockWise();
			// Corner of the footprint at the foot end, on the counter-clockwise side.
			BlockPos corner = this.foot.relative(this.facing, -1).relative(side, -HALF_WIDTH);
			double[] start = cornerOf(corner, this.facing, side);
			double ax = this.facing.getStepX();
			double az = this.facing.getStepZ();
			double sx = side.getStepX();
			double sz = side.getStepZ();

			double xa = start[0] + ax * along0 + sx * across0;
			double za = start[1] + az * along0 + sz * across0;
			double xb = start[0] + ax * along1 + sx * across1;
			double zb = start[1] + az * along1 + sz * across1;
			double y = this.foot.getY();

			return TempDisplays.box(level, state, Math.min(xa, xb), y + y0, Math.min(za, zb), Math.max(xa, xb), y + y1, Math.max(za, zb), false)
					.getUUID();
		}

		/** The outer corner of a block, on the side opposite to both directions. */
		private static double[] cornerOf(BlockPos pos, Direction along, Direction side) {
			double x = pos.getX() + (along.getStepX() < 0 || side.getStepX() < 0 ? 1.0 : 0.0);
			double z = pos.getZ() + (along.getStepZ() < 0 || side.getStepZ() < 0 ? 1.0 : 0.0);
			return new double[] {x, z};
		}
	}

	public static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Napping>> iterator = NAPPING.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, Napping> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

			Napping napping = entry.getValue();

			if (player != null && player.isSleeping() && player.getSleepTimer() >= 100 && !napping.healed) {
				// A full nap heals completely.
				napping.healed = true;
				player.setHealth(player.getMaxHealth());
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);

				// At night vanilla skips the night and wakes everyone; during the day the nap simply ends.
				if (!player.level().environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, player.position()).canSleep(player.level())) {
					player.stopSleepInBed(true, true);
				}
			}

			if (player == null || !player.isSleeping()) {
				iterator.remove();
				removeBed(server, entry.getValue());
			}
		}
	}

	private static void removeBed(MinecraftServer server, Napping napping) {
		ServerLevel level = server.getLevel(napping.dimension);

		if (level == null) {
			return;
		}

		for (UUID id : napping.displays) {
			TempDisplays.remove(level, id);
		}

		int flags = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;

		for (BlockPos pos : List.of(napping.head, napping.foot)) {
			if (level.getBlockState(pos).getBlock() instanceof BedBlock) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
			}
		}
	}

	public static void forget(ServerPlayer player) {
		Napping napping = NAPPING.remove(player.getUUID());

		if (napping != null) {
			removeBed(player.level().getServer(), napping);
		}
	}

	public static void reset() {
		NAPPING.clear();
	}
}
