package io.github.pikczu77.upgrades.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.upgrades.config.UpgradesConfig;
import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.util.Screens;

/**
 * The eye of ender: hold sneak (standing still) to charge a teleport. It lands somewhere random, with a chance to
 * land right at the end portal of the nearest stronghold.
 */
public final class EyeSpy {
	private static final Map<UUID, Integer> CHARGE = new HashMap<>();

	private EyeSpy() {
	}

	public static void tick(ServerPlayer player, long mask) {
		if (!Upgrade.has(mask, Upgrade.EYE_SPY)) {
			return;
		}

		Input input = player.getLastClientInput();
		boolean charging = player.isShiftKeyDown() && !input.forward() && !input.backward() && !input.left() && !input.right()
				&& !player.isSleeping();
		UUID id = player.getUUID();

		if (!charging) {
			Integer charge = CHARGE.remove(id);

			if (charge != null && charge > 0) {
				Screens.actionBar(player, Component.empty());
			}

			return;
		}

		int charge = CHARGE.merge(id, 1, Integer::sum);
		int needed = UpgradesConfig.get().eyeChargeTicks;

		if (charge < 0) {
			// Already teleported, waiting for sneak to be released.
			return;
		}

		ServerLevel level = player.level();

		if (charge % 2 == 0) {
			level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(1.0), player.getZ(), 6 + charge / 3, 0.4, 0.8, 0.4, 0.6);
			Screens.actionBar(player, bar(charge, needed));
		}

		if (charge % 10 == 0) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.6F,
					0.6F + charge / (float) needed);
		}

		if (charge >= needed) {
			CHARGE.put(id, Integer.MIN_VALUE / 2);
			teleport(player);
		}
	}

	private static Component bar(int charge, int needed) {
		int filled = Mth.clamp(charge * 10 / needed, 0, 10);
		return Component.literal("Teleport ").withStyle(ChatFormatting.DARK_AQUA)
				.append(Component.literal("■".repeat(filled)).withStyle(ChatFormatting.GREEN))
				.append(Component.literal("■".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY));
	}

	private static void teleport(ServerPlayer player) {
		ServerLevel level = player.level();
		RandomSource random = player.getRandom();
		Vec3 target = null;

		if (random.nextInt(100) < UpgradesConfig.get().eyePortalChance && level.dimension() == Level.OVERWORLD) {
			target = endPortal(level, player.blockPosition());
		}

		for (int attempt = 0; attempt < 8 && target == null; attempt++) {
			target = randomSpot(level, player.blockPosition(), random);
		}

		if (target == null) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F);
			return;
		}

		level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(1.0), player.getZ(), 80, 0.5, 1.0, 0.5, 0.2);
		player.teleportTo(level, target.x, target.y, target.z, Set.of(), player.getYRot(), player.getXRot(), true);
		player.resetFallDistance();
		level.playSound(null, target.x, target.y, target.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.x, target.y + 1.0, target.z, 80, 0.5, 1.0, 0.5, 0.2);
	}

	private static @Nullable Vec3 randomSpot(ServerLevel level, BlockPos origin, RandomSource random) {
		boolean overworldLike = !level.dimensionType().hasCeiling();
		double angle = random.nextDouble() * Math.PI * 2.0;
		double distance = overworldLike ? 80.0 + random.nextDouble() * 320.0 : 16.0 + random.nextDouble() * 48.0;
		int x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
		int z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);

		if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, origin.getY(), z))) {
			return null;
		}

		level.getChunk(x >> 4, z >> 4);

		if (overworldLike) {
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

			if (y <= level.getMinY()) {
				return null;
			}

			BlockState below = level.getBlockState(new BlockPos(x, y - 1, z));
			return below.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) ? null : new Vec3(x + 0.5, y, z + 0.5);
		}

		// Nether: scan down for a floor with two air blocks above it.
		for (int y = Math.min(origin.getY() + 24, level.getMaxY() - 8); y > level.getMinY() + 4; y--) {
			BlockPos pos = new BlockPos(x, y, z);

			if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
					&& level.getBlockState(pos.below()).isSolidRender() && level.getFluidState(pos.below()).isEmpty()) {
				return new Vec3(x + 0.5, y, z + 0.5);
			}
		}

		return null;
	}

	/** A safe spot inside the portal room of the nearest stronghold, or null. */
	private static @Nullable Vec3 endPortal(ServerLevel level, BlockPos origin) {
		BlockPos stronghold = level.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, origin, 100, false);

		if (stronghold == null) {
			return null;
		}

		ChunkPos chunkPos = new ChunkPos(stronghold);
		ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
		BoundingBox room = null;

		for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
			boolean eyeLocated = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).wrapAsHolder(entry.getKey())
					.is(StructureTags.EYE_OF_ENDER_LOCATED);

			if (!eyeLocated || !entry.getValue().isValid()) {
				continue;
			}

			for (StructurePiece piece : entry.getValue().getPieces()) {
				if (piece instanceof StrongholdPieces.PortalRoom) {
					room = piece.getBoundingBox();
				}
			}
		}

		if (room == null) {
			return null;
		}

		// Generate the room for real, then pick the free spot closest to the portal frame.
		for (int cx = room.minX() >> 4; cx <= room.maxX() >> 4; cx++) {
			for (int cz = room.minZ() >> 4; cz <= room.maxZ() >> 4; cz++) {
				level.getChunk(cx, cz);
			}
		}

		BlockPos frame = null;

		for (BlockPos pos : BlockPos.betweenClosed(room.minX(), room.minY(), room.minZ(), room.maxX(), room.maxY(), room.maxZ())) {
			if (level.getBlockState(pos).is(Blocks.END_PORTAL_FRAME)) {
				frame = pos.immutable();
				break;
			}
		}

		BlockPos center = frame != null ? frame : room.getCenter();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.betweenClosed(room.minX(), room.minY(), room.minZ(), room.maxX(), room.maxY(), room.maxZ())) {
			BlockState below = level.getBlockState(pos.below());

			if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir() || !below.isSolidRender()
					|| below.is(Blocks.END_PORTAL_FRAME) || !level.getFluidState(pos).isEmpty()) {
				continue;
			}

			double distance = pos.distSqr(center);

			if (distance < bestDistance && distance > 4.0) {
				bestDistance = distance;
				best = pos.immutable();
			}
		}

		return best == null ? null : Vec3.atBottomCenterOf(best);
	}

	public static void forget(ServerPlayer player) {
		CHARGE.remove(player.getUUID());
	}

	public static void reset() {
		CHARGE.clear();
	}
}
