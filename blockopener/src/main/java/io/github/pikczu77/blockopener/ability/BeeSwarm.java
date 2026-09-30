package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.opening.TransientEntities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Bee Drill swarm: summoned bees follow their owner, attack what the owner points at (or whatever
 * hurts the owner), never lose their stinger and buzz off after a while.
 */
public final class BeeSwarm {
	public static final int MAX_BEES = 8;
	private static final int SUMMON_COUNT = 3;
	private static final int LIFETIME = 20 * 120;

	private static final Map<UUID, List<SummonedBee>> SWARMS = new HashMap<>();

	public static int summon(ServerPlayer owner) {
		List<SummonedBee> swarm = SWARMS.computeIfAbsent(owner.getUUID(), uuid -> new ArrayList<>());
		swarm.removeIf(summoned -> !summoned.bee.isAlive());
		ServerLevel level = owner.level();
		int count = Math.min(SUMMON_COUNT, MAX_BEES - swarm.size());
		for (int i = 0; i < count; i++) {
			Bee bee = EntityType.BEE.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (bee == null) {
				continue;
			}
			double x = owner.getX() + (level.random.nextDouble() - 0.5) * 2.0;
			double y = owner.getY() + 1.2 + level.random.nextDouble() * 0.6;
			double z = owner.getZ() + (level.random.nextDouble() - 0.5) * 2.0;
			bee.snapTo(x, y, z, owner.getYRot(), 0.0F);
			boost(bee.getAttribute(Attributes.ATTACK_DAMAGE), 4.0);
			boost(bee.getAttribute(Attributes.FLYING_SPEED), 0.9);
			boost(bee.getAttribute(Attributes.MOVEMENT_SPEED), 0.45);
			bee.setStayOutOfHiveCountdown(LIFETIME);
			TransientEntities.spawn(level, bee);
			swarm.add(new SummonedBee(bee, level.getGameTime() + LIFETIME));
			level.sendParticles(ParticleTypes.WAX_ON, x, y, z, 5, 0.2, 0.2, 0.2, 0.0);
		}
		level.playSound(null, owner.blockPosition(), SoundEvents.BEEHIVE_EXIT, SoundSource.PLAYERS, 1.2F, 1.0F);
		return count;
	}

	private static void boost(@Nullable AttributeInstance attribute, double value) {
		if (attribute != null) {
			attribute.setBaseValue(value);
		}
	}

	public static void attack(ServerPlayer owner, LivingEntity target) {
		List<SummonedBee> swarm = SWARMS.get(owner.getUUID());
		if (swarm == null || swarm.stream().noneMatch(summoned -> summoned.bee.isAlive())) {
			summon(owner);
			swarm = SWARMS.get(owner.getUUID());
		}
		for (SummonedBee summoned : swarm) {
			summoned.target = target;
		}
		ServerLevel level = owner.level();
		level.sendParticles(ParticleTypes.ANGRY_VILLAGER, target.getX(), target.getEyeY() + 0.4, target.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
		level.playSound(null, owner.blockPosition(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.PLAYERS, 1.0F, 1.2F);
	}

	/** Someone hurt the owner: the swarm goes after them (if the owner is not already fighting something). */
	static void defend(ServerPlayer owner, LivingEntity attacker) {
		List<SummonedBee> swarm = SWARMS.get(owner.getUUID());
		if (swarm == null || isOwnBee(owner, attacker)) {
			return;
		}
		for (SummonedBee summoned : swarm) {
			if (summoned.target == null || !summoned.target.isAlive()) {
				summoned.target = attacker;
			}
		}
	}

	public static boolean isOwnBee(Player player, Entity entity) {
		List<SummonedBee> swarm = SWARMS.get(player.getUUID());
		if (swarm == null || !(entity instanceof Bee)) {
			return false;
		}
		for (SummonedBee summoned : swarm) {
			if (summoned.bee == entity) {
				return true;
			}
		}
		return false;
	}

	public static int size(ServerPlayer owner) {
		List<SummonedBee> swarm = SWARMS.get(owner.getUUID());
		return swarm == null ? 0 : swarm.size();
	}

	static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, List<SummonedBee>>> swarms = SWARMS.entrySet().iterator();
		while (swarms.hasNext()) {
			Map.Entry<UUID, List<SummonedBee>> entry = swarms.next();
			ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
			Iterator<SummonedBee> bees = entry.getValue().iterator();
			while (bees.hasNext()) {
				SummonedBee summoned = bees.next();
				Bee bee = summoned.bee;
				if (!bee.isAlive()) {
					bees.remove();
				} else if (owner == null || owner.level() != bee.level() || bee.level().getGameTime() > summoned.expires) {
					poof(bee);
					bees.remove();
				} else {
					tickBee(owner, summoned);
				}
			}
			if (entry.getValue().isEmpty()) {
				swarms.remove();
			}
		}
	}

	private static void tickBee(ServerPlayer owner, SummonedBee summoned) {
		Bee bee = summoned.bee;
		if (bee.hasStung()) {
			bee.setHasStung(false);
		}
		LivingEntity target = summoned.target;
		if (target != null && (!target.isAlive() || target.level() != bee.level() || target.distanceToSqr(bee) > 48 * 48 || target == owner)) {
			summoned.target = null;
			target = null;
			bee.stopBeingAngry();
		}
		if (target != null) {
			if (bee.getTarget() != target || !bee.isAngry()) {
				bee.setTarget(target);
				bee.setPersistentAngerTarget(EntityReference.of(target));
				bee.setTimeToRemainAngry(200);
			}
			return;
		}
		if (bee.getTarget() == owner) {
			bee.stopBeingAngry();
		}
		double distance = bee.distanceToSqr(owner);
		if (distance > 24 * 24) {
			bee.teleportTo(owner.getX(), owner.getY() + 1.5, owner.getZ());
		} else if (distance > 4 * 4 && bee.tickCount % 10 == 0) {
			bee.getNavigation().moveTo(owner.getX(), owner.getY() + 1.5, owner.getZ(), 1.6);
		}
	}

	private static void poof(Bee bee) {
		if (bee.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.POOF, bee.getX(), bee.getY(), bee.getZ(), 5, 0.1, 0.1, 0.1, 0.02);
		}
		TransientEntities.remove(bee);
	}

	static void dismiss(UUID owner) {
		List<SummonedBee> swarm = SWARMS.remove(owner);
		if (swarm != null) {
			swarm.forEach(summoned -> poof(summoned.bee));
		}
	}

	static void clear() {
		SWARMS.values().forEach(swarm -> swarm.forEach(summoned -> TransientEntities.remove(summoned.bee)));
		SWARMS.clear();
	}

	private static final class SummonedBee {
		final Bee bee;
		final long expires;
		@Nullable LivingEntity target;

		SummonedBee(Bee bee, long expires) {
			this.bee = bee;
			this.expires = expires;
		}
	}

	private BeeSwarm() {
	}
}
