package io.github.pikczu77.upgrades.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.upgrades.config.UpgradesConfig;
import io.github.pikczu77.upgrades.entity.CombatClone;
import io.github.pikczu77.upgrades.entity.ModEntities;
import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

/**
 * Spawns combat clones when monsters hurt their owner, splits level 2 clones on death and breeds the dragon egg
 * clones (multiplicity).
 */
public final class Clones {
	private static final float MIN_SPLIT_SCALE = 0.3F;
	/** Game time until each player can get another clone. */
	private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
	/** Live clone count per owner, refreshed every second. */
	private static final Map<UUID, Integer> COUNT = new HashMap<>();
	private static final Map<UUID, Integer> BREEDERS = new HashMap<>();

	private Clones() {
	}

	public static void afterDamage(LivingEntity victim, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (!(victim instanceof ServerPlayer player) || !(source.getEntity() instanceof Enemy) || !(source.getEntity() instanceof LivingEntity attacker)) {
			return;
		}

		int level = Upgrade.cloneLevel(UpgradeManager.mask(player));

		if (level <= 0) {
			return;
		}

		long now = player.level().getGameTime();
		Long ready = COOLDOWN.get(player.getUUID());

		if (ready != null && ready > now) {
			return;
		}

		COOLDOWN.put(player.getUUID(), now + 20);
		CombatClone clone = spawn(player.level(), player, level, 1.0F, false, player.position(), 1.5);

		if (clone != null) {
			distract(player, attacker, clone);
		}
	}

	/** Monsters going after the owner switch to the new clone. */
	private static void distract(ServerPlayer player, LivingEntity attacker, CombatClone clone) {
		if (attacker instanceof Mob mob) {
			mob.setTarget(clone);
		}

		AABB area = player.getBoundingBox().inflate(12.0);

		for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area, mob -> mob instanceof Enemy && mob.getTarget() == player)) {
			if (mob.getRandom().nextBoolean()) {
				mob.setTarget(clone);
			}
		}
	}

	public static @Nullable CombatClone spawn(ServerLevel level, @Nullable Player owner, int cloneLevel, float scale, boolean breeder, Vec3 around, double spread) {
		if (owner != null) {
			int limit = breeder ? UpgradesConfig.get().multiplicityLimit : UpgradesConfig.get().maxClones;
			Map<UUID, Integer> counts = breeder ? BREEDERS : COUNT;
			int count = counts.getOrDefault(owner.getUUID(), 0);

			if (count >= limit) {
				return null;
			}

			counts.put(owner.getUUID(), count + 1);
		}

		CombatClone clone = ModEntities.COMBAT_CLONE.create(level, EntitySpawnReason.MOB_SUMMONED);

		if (clone == null) {
			return null;
		}

		double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
		double distance = spread * level.getRandom().nextDouble();
		clone.snapTo(around.x + Math.cos(angle) * distance, around.y, around.z + Math.sin(angle) * distance,
				owner != null ? owner.getYRot() : 0.0F, 0.0F);
		clone.setup(owner, cloneLevel, scale, breeder);
		level.addFreshEntity(clone);
		level.sendParticles(ParticleTypes.POOF, clone.getX(), clone.getY(0.5), clone.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
		level.playSound(null, clone.getX(), clone.getY(), clone.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.3F);
		return clone;
	}

	/** Level 2 clones split into two smaller ones when they die ("clones split upon death"). */
	public static void afterDeath(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof CombatClone clone) || clone.cloneLevel() != 2 || !(clone.level() instanceof ServerLevel level)) {
			return;
		}

		float scale = clone.getScale() * 0.7F;

		if (scale < MIN_SPLIT_SCALE) {
			return;
		}

		Player owner = clone.owner();

		for (int i = 0; i < 2; i++) {
			spawn(level, owner, 2, scale, clone.isBreeder(), clone.position(), 0.6);
		}
	}

	/** A multiplicity clone makes another one next to it. */
	public static void breed(CombatClone parent) {
		if (!(parent.level() instanceof ServerLevel level)) {
			return;
		}

		Player owner = parent.owner();

		if (owner == null || !(owner instanceof ServerPlayer player) || !UpgradeManager.has(player, Upgrade.MULTIPLICITY)) {
			return;
		}

		int level2 = Math.max(2, Upgrade.cloneLevel(UpgradeManager.mask(player)));
		spawn(level, owner, level2, parent.getScale(), true, parent.position(), 1.0);
	}

	/** The next generation: a group of little clones that keep multiplying. */
	public static void startMultiplicity(ServerPlayer player) {
		int level = Math.max(2, Upgrade.cloneLevel(UpgradeManager.mask(player)));

		for (int i = 0; i < 4; i++) {
			spawn(player.level(), player, level, 0.5F, true, player.position(), 2.5);
		}
	}

	public static int clear(MinecraftServer server, @Nullable UUID owner) {
		int removed = 0;

		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof CombatClone clone && (owner == null || owner.equals(clone.ownerUuid()))) {
					clone.discard();
					removed++;
				}
			}
		}

		COUNT.clear();
		BREEDERS.clear();
		return removed;
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) {
			return;
		}

		COUNT.clear();
		BREEDERS.clear();

		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof CombatClone clone && clone.isAlive() && clone.ownerUuid() != null) {
					(clone.isBreeder() ? BREEDERS : COUNT).merge(clone.ownerUuid(), 1, Integer::sum);
				}
			}
		}
	}

	public static void reset() {
		COOLDOWN.clear();
		COUNT.clear();
		BREEDERS.clear();
	}
}
