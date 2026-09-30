package io.github.pikczu77.upgrades.ability;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.upgrades.entity.CombatClone;
import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

/**
 * Melee upgrades: the golem arm throws mobs up, the sword boot sweeps everything around and hurts mobs you sprint
 * into, and the hot hands put lava under the victim.
 */
public final class Combat {
	private static final double SWEEP_RADIUS = 3.5;
	private static final float RAM_DAMAGE = 4.0F;
	/** Game time until each entity can be rammed again. */
	private static final Map<Integer, Long> RAM_COOLDOWN = new HashMap<>();
	private static boolean sweeping;

	private Combat() {
	}

	public static void afterDamage(LivingEntity victim, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (sweeping || blocked || !(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player
				|| !source.is(DamageTypes.PLAYER_ATTACK) || victim == player) {
			return;
		}

		long mask = UpgradeManager.mask(player);

		if (Upgrade.has(mask, Upgrade.GOLEM_ARM)) {
			// Like an iron golem: a strong push straight up.
			victim.setDeltaMovement(victim.getDeltaMovement().add(0.0, 0.75, 0.0));
			victim.hurtMarked = true;
		}

		if (Upgrade.has(mask, Upgrade.HOT_HANDS)) {
			HotLava.placeUnder(player.level(), victim);
		}

		if (Upgrade.has(mask, Upgrade.SWORD_BOOT)) {
			sweep(player, victim, Math.max(1.0F, baseDamage * 0.6F));
		}
	}

	private static void sweep(ServerPlayer player, LivingEntity victim, float damage) {
		ServerLevel level = player.level();
		AABB area = player.getBoundingBox().inflate(SWEEP_RADIUS, 1.0, SWEEP_RADIUS);
		sweeping = true;

		try {
			for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, area)) {
				// The sweep hurts everything, your own clones too ("the sweeping edge is too strong, sorry guys").
				if (other == player || other == victim || other instanceof ArmorStand || !other.isAlive()
						|| other.isAlliedTo(player) && !(other instanceof CombatClone)) {
					continue;
				}

				if (other.hurtServer(level, player.damageSources().playerAttack(player), damage)) {
					Vec3 push = other.position().subtract(player.position()).horizontal().normalize();
					other.knockback(0.5, -push.x, -push.z);
				}
			}
		} finally {
			sweeping = false;
		}

		Vec3 look = player.getLookAngle().horizontal().normalize();
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + look.x, player.getY(0.5), player.getZ() + look.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	/** Sprinting into mobs damages them and knocks them back. */
	public static void tick(ServerPlayer player, long mask) {
		if (!Upgrade.has(mask, Upgrade.SWORD_BOOT) || !player.isSprinting()) {
			return;
		}

		ServerLevel level = player.level();
		long now = level.getGameTime();
		Vec3 direction = player.getDeltaMovement().horizontal();

		if (direction.lengthSqr() < 1.0E-4) {
			direction = player.getLookAngle().horizontal();
		}

		direction = direction.normalize();

		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(0.35))) {
			if (other == player || other instanceof ArmorStand || other instanceof CombatClone || !other.isAlive() || other.isAlliedTo(player)
					|| other instanceof ServerPlayer) {
				continue;
			}

			Long ready = RAM_COOLDOWN.get(other.getId());

			if (ready != null && ready > now) {
				continue;
			}

			RAM_COOLDOWN.put(other.getId(), now + 10);
			sweeping = true;

			try {
				if (other.hurtServer(level, player.damageSources().playerAttack(player), RAM_DAMAGE)) {
					other.knockback(1.2, -direction.x, -direction.z);
					other.hurtMarked = true;
				}
			} finally {
				sweeping = false;
			}
		}

		if (now % 200 == 0) {
			RAM_COOLDOWN.values().removeIf(time -> time < now);
		}
	}

	public static void reset() {
		RAM_COOLDOWN.clear();
		sweeping = false;
	}
}
