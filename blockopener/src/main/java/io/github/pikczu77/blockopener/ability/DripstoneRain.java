package io.github.pikczu77.blockopener.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.Vec3;

/**
 * Dripstone Sword's Dripstone Rain: a volley of falling stalactites over the target.
 * It finds its own ceiling, so unlike in the video it also works in low caves.
 */
public final class DripstoneRain {
	private static final int SPIKES = 14;
	private static final int DURATION = 14;
	private static final double RADIUS = 2.6;

	private static final List<Rain> ACTIVE = new ArrayList<>();
	/** Falling dripstone entity -> the player who called the rain (who is never hurt by it). */
	private static final Map<UUID, Owned> OWNERS = new HashMap<>();
	private static long clock;

	public static void start(ServerPlayer owner, Vec3 center) {
		ServerLevel level = owner.level();
		ACTIVE.add(new Rain(level, owner, center));
		level.playSound(null, center.x, center.y, center.z, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundSource.PLAYERS, 1.5F, 0.5F);
		level.sendParticles(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, center.x, center.y + 4.0, center.z, 30, RADIUS, 1.0, RADIUS, 0.0);
	}

	static void tick() {
		Iterator<Rain> iterator = ACTIVE.iterator();
		while (iterator.hasNext()) {
			Rain rain = iterator.next();
			if (rain.tick()) {
				iterator.remove();
			}
		}
		clock++;
		OWNERS.values().removeIf(owned -> owned.expires < clock);
	}

	static boolean isOwnDripstone(ServerPlayer player, DamageSource source) {
		if (!source.is(DamageTypes.FALLING_STALACTITE) || source.getDirectEntity() == null) {
			return false;
		}
		Owned owned = OWNERS.get(source.getDirectEntity().getUUID());
		return owned != null && owned.owner.equals(player.getUUID());
	}

	static void clear() {
		ACTIVE.clear();
		OWNERS.clear();
	}

	private record Owned(UUID owner, long expires) {
	}

	private static final class Rain {
		private final ServerLevel level;
		private final ServerPlayer owner;
		private final Vec3 center;
		private int age;
		private int spawned;

		Rain(ServerLevel level, ServerPlayer owner, Vec3 center) {
			this.level = level;
			this.owner = owner;
			this.center = center;
		}

		boolean tick() {
			int due = Math.min(SPIKES, (this.age + 1) * SPIKES / DURATION);
			while (this.spawned < due) {
				spawnSpike(this.spawned == 0);
				this.spawned++;
			}
			this.age++;
			return this.spawned >= SPIKES;
		}

		private void spawnSpike(boolean aimed) {
			double dx = aimed ? 0.0 : (this.level.random.nextDouble() * 2 - 1) * RADIUS;
			double dz = aimed ? 0.0 : (this.level.random.nextDouble() * 2 - 1) * RADIUS;
			Vec3 ground = this.center.add(dx, 0.0, dz);
			if (!aimed && ground.distanceToSqr(this.owner.position()) < 1.7 * 1.7) {
				return;
			}
			BlockPos base = BlockPos.containing(ground.x, this.center.y + 1.0, ground.z);
			BlockPos spawnAt = null;
			for (int up = 2; up <= 14; up++) {
				BlockPos candidate = base.above(up);
				if (!this.level.getBlockState(candidate).isAir()) {
					break;
				}
				spawnAt = candidate;
			}
			if (spawnAt == null) {
				return;
			}
			BlockState tip = Blocks.POINTED_DRIPSTONE.defaultBlockState()
				.setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
				.setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP);
			FallingBlockEntity spike = FallingBlockEntity.fall(this.level, spawnAt, tip);
			spike.setHurtsEntities(2.5F, 30);
			spike.disableDrop();
			OWNERS.put(spike.getUUID(), new Owned(this.owner.getUUID(), clock + 200));
			this.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.defaultBlockState()),
				spawnAt.getX() + 0.5, spawnAt.getY() + 0.5, spawnAt.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.0);
		}
	}

	private DripstoneRain() {
	}
}
