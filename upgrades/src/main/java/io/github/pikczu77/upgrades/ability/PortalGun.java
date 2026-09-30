package io.github.pikczu77.upgrades.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.upgrades.util.TempDisplays;

/**
 * The portal in the pocket: an empty-hand right-click places a small nether portal where you look. Up to two portals
 * per player, the third replaces the oldest; walking into one takes you out of the other (also across dimensions).
 * Portals are only visuals (block displays), nothing in the world is changed.
 */
public final class PortalGun {
	private static final double RANGE = 64.0;
	private static final int TELEPORT_COOLDOWN = 20;

	/** A 1×2 portal standing on {@code base}. {@code axis} is the axis the portal plane spans (like the nether portal block). */
	private record Portal(ResourceKey<Level> dimension, BlockPos base, Direction.Axis axis, List<UUID> displays) {
		AABB box() {
			double thin = 0.3;
			return this.axis == Direction.Axis.X
					? new AABB(this.base.getX(), this.base.getY(), this.base.getZ() + 0.5 - thin, this.base.getX() + 1.0, this.base.getY() + 2.0,
							this.base.getZ() + 0.5 + thin)
					: new AABB(this.base.getX() + 0.5 - thin, this.base.getY(), this.base.getZ(), this.base.getX() + 0.5 + thin, this.base.getY() + 2.0,
							this.base.getZ() + 1.0);
		}

		/** Unit vector through the portal plane. */
		Vec3 normal() {
			return this.axis == Direction.Axis.X ? new Vec3(0.0, 0.0, 1.0) : new Vec3(1.0, 0.0, 0.0);
		}
	}

	private static final Map<UUID, List<Portal>> PORTALS = new HashMap<>();
	/** Players who just came out of a portal: they must leave it before it works again. */
	private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

	private PortalGun() {
	}

	public static void shoot(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(RANGE));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

		if (hit.getType() != HitResult.Type.BLOCK) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.8F);
			return;
		}

		Direction face = hit.getDirection();
		BlockPos base;
		Direction.Axis axis;

		if (face == Direction.UP) {
			base = hit.getBlockPos().above();
			// Facing the player.
			axis = player.getDirection().getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
		} else if (face == Direction.DOWN) {
			base = hit.getBlockPos().below(2);
			axis = player.getDirection().getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
		} else {
			base = hit.getBlockPos().relative(face);
			// Flat against the wall.
			axis = face.getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;

			if (!free(level, base) && free(level, base.below())) {
				base = base.below();
			}
		}

		if (!free(level, base)) {
			base = base.above();
		}

		if (!free(level, base)) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.8F);
			return;
		}

		List<Portal> portals = PORTALS.computeIfAbsent(player.getUUID(), uuid -> new ArrayList<>());

		if (portals.size() >= 2) {
			remove(level.getServer(), portals.removeFirst());
		}

		BlockState state = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, axis);
		List<UUID> displays = new ArrayList<>();

		for (int y = 0; y < 2; y++) {
			BlockPos pos = base.above(y);
			displays.add(TempDisplays.box(level, state, pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, true).getUUID());
		}

		portals.add(new Portal(level.dimension(), base.immutable(), axis, displays));
		level.playSound(null, base, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.3F, 2.0F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.6F);
		player.swing(InteractionHand.MAIN_HAND, true);
	}

	private static boolean free(ServerLevel level, BlockPos base) {
		return level.getBlockState(base).canBeReplaced() && level.getBlockState(base.above()).canBeReplaced();
	}

	public static void tick(MinecraftServer server) {
		if (PORTALS.isEmpty()) {
			return;
		}

		for (List<Portal> portals : PORTALS.values()) {
			for (Portal portal : portals) {
				ServerLevel level = server.getLevel(portal.dimension());

				if (level != null && level.getGameTime() % 4 == 0) {
					Vec3 center = portal.box().getCenter();
					level.sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 3, 0.3, 0.6, 0.3, 0.3);
				}
			}

			if (portals.size() < 2) {
				continue;
			}

			for (int i = 0; i < 2; i++) {
				Portal from = portals.get(i);
				Portal to = portals.get(1 - i);
				ServerLevel level = server.getLevel(from.dimension());

				if (level == null) {
					continue;
				}

				for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, from.box())) {
					enter(server, player, from, to);
				}
			}
		}

		long now = server.overworld().getGameTime();
		COOLDOWN.values().removeIf(until -> until < now);
	}

	private static void enter(MinecraftServer server, ServerPlayer player, Portal from, Portal to) {
		long now = server.overworld().getGameTime();
		Long until = COOLDOWN.get(player.getUUID());

		if (until != null && until > now) {
			// Still standing in the exit portal: keep the cooldown going.
			COOLDOWN.put(player.getUUID(), now + TELEPORT_COOLDOWN);
			return;
		}

		ServerLevel target = server.getLevel(to.dimension());

		if (target == null) {
			return;
		}

		// Keep walking the same way: exit on the side matching the direction the player entered.
		double side = Math.signum(player.getDeltaMovement().dot(from.normal()));
		Vec3 motion = player.getDeltaMovement();

		if (side == 0.0) {
			side = Math.signum(player.position().subtract(from.box().getCenter()).dot(from.normal())) * -1.0;
		}

		if (side == 0.0) {
			side = 1.0;
		}

		Vec3 exit = Vec3.atBottomCenterOf(to.base()).add(to.normal().scale(side * 0.9));
		ServerLevel fromLevel = player.level();
		fromLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(1.0), player.getZ(), 30, 0.3, 0.6, 0.3, 0.1);
		COOLDOWN.put(player.getUUID(), now + TELEPORT_COOLDOWN);
		player.teleportTo(target, exit.x, exit.y, exit.z, Set.of(), player.getYRot(), player.getXRot(), true);
		player.setDeltaMovement(motion);
		player.hurtMarked = true;
		target.playSound(null, exit.x, exit.y, exit.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.4F);
	}

	private static void remove(MinecraftServer server, Portal portal) {
		ServerLevel level = server.getLevel(portal.dimension());

		if (level != null) {
			for (UUID id : portal.displays()) {
				TempDisplays.remove(level, id);
			}
		}
	}

	public static int clear(MinecraftServer server, @Nullable UUID owner) {
		int removed = 0;

		for (Map.Entry<UUID, List<Portal>> entry : PORTALS.entrySet()) {
			if (owner == null || owner.equals(entry.getKey())) {
				for (Portal portal : entry.getValue()) {
					remove(server, portal);
					removed++;
				}

				entry.getValue().clear();
			}
		}

		return removed;
	}

	public static void forget(ServerPlayer player) {
		clear(player.level().getServer(), player.getUUID());
		PORTALS.remove(player.getUUID());
		COOLDOWN.remove(player.getUUID());
	}

	public static void reset() {
		PORTALS.clear();
		COOLDOWN.clear();
	}
}
