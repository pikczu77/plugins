package io.github.pikczu77.upgrades.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import io.github.pikczu77.upgrades.upgrade.Bonus;
import io.github.pikczu77.upgrades.upgrade.Bonus.Special;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

/**
 * The bonus abilities that need code: fire and wither on hit, wither for attackers, the escape teleport, extra
 * experience, calm piglins and the wolf that always comes back. (The three-arrow shot lives in Shooting.)
 */
public final class BonusAbilities {
	private static final String BUDDY_TAG = "upgrades_buddy";
	private static final int ESCAPE_COOLDOWN = 20 * 30;

	private static final Map<UUID, Long> ESCAPE_READY = new HashMap<>();

	private BonusAbilities() {
	}

	private static boolean has(Player player, Special special) {
		return UpgradeManager.hasSpecial(player, special);
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(BonusAbilities::afterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(BonusAbilities::afterDeath);
	}

	private static void afterDamage(LivingEntity victim, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (blocked) {
			return;
		}

		ServerLevel level = (ServerLevel) victim.level();

		// The player hits something in melee.
		if (source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player && victim != player
				&& source.is(DamageTypes.PLAYER_ATTACK)) {
			if (has(player, Special.FIRE_HIT)) {
				victim.igniteForSeconds(4.0F);
			}

			if (has(player, Special.WITHER_HIT)) {
				victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), player);
			}
		}

		// Something hurts the player.
		if (victim instanceof ServerPlayer player) {
			if (source.getEntity() instanceof LivingEntity attacker && attacker != player && source.getDirectEntity() == attacker
					&& has(player, Special.THORNS_WITHER)) {
				attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1), player);
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, attacker.getX(), attacker.getY(0.5), attacker.getZ(), 15, 0.3, 0.4, 0.3, 0.02);
			}

			if (player.getHealth() <= 6.0F && player.isAlive() && has(player, Special.ESCAPE)) {
				escape(player);
			}
		}
	}

	/** Like a chorus fruit: a random teleport nearby, at most every 30 seconds. */
	private static void escape(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long ready = ESCAPE_READY.get(player.getUUID());

		if (ready != null && ready > now) {
			return;
		}

		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();

		for (int attempt = 0; attempt < 16; attempt++) {
			double tx = x + (player.getRandom().nextDouble() - 0.5) * 24.0;
			double ty = y + player.getRandom().nextInt(9) - 4;
			double tz = z + (player.getRandom().nextDouble() - 0.5) * 24.0;

			if (player.randomTeleport(tx, ty, tz, true)) {
				ESCAPE_READY.put(player.getUUID(), now + ESCAPE_COOLDOWN);
				player.level().playSound(null, x, y, z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
				player.resetFallDistance();
				return;
			}
		}
	}

	private static void afterDeath(LivingEntity entity, DamageSource source) {
		if (entity instanceof Mob && source.getEntity() instanceof ServerPlayer player && has(player, Special.XP_BONUS)) {
			ServerLevel level = (ServerLevel) entity.level();
			level.addFreshEntity(new ExperienceOrb(level, entity.getX(), entity.getY(0.5), entity.getZ(), 3 + level.getRandom().nextInt(6)));
		}
	}

	public static void tick(ServerPlayer player) {
		long[] bits = UpgradeManager.bonuses(player);

		if (Bonus.count(bits) == 0) {
			return;
		}

		BonusPassives.tick(player, bits, false);

		if (Bonus.hasSpecial(bits, Special.PIGLIN_CALM) && player.tickCount % 10 == 0) {
			for (AbstractPiglin piglin : player.level().getEntitiesOfClass(AbstractPiglin.class, player.getBoundingBox().inflate(16.0))) {
				if (piglin.getTarget() == player) {
					piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
					piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
					piglin.setTarget(null);
				}
			}
		}

		if (Bonus.hasSpecial(bits, Special.WOLF_BUDDY) && player.tickCount % 100 == 0) {
			wolfBuddy(player);
		}
	}

	/** Keeps the player's tamed wolf around: if it is gone, a new one comes. */
	private static void wolfBuddy(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();

		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof Wolf wolf && wolf.isAlive() && wolf.getTags().contains(BUDDY_TAG) && wolf.isOwnedBy(player)) {
					return;
				}
			}
		}

		ServerLevel level = player.level();
		Wolf wolf = EntityType.WOLF.create(level, EntitySpawnReason.MOB_SUMMONED);

		if (wolf == null) {
			return;
		}

		wolf.snapTo(player.getX() + level.getRandom().nextInt(3) - 1, player.getY(), player.getZ() + level.getRandom().nextInt(3) - 1,
				player.getYRot(), 0.0F);
		wolf.tame(player);
		wolf.addTag(BUDDY_TAG);
		level.addFreshEntity(wolf);
		level.sendParticles(ParticleTypes.HEART, wolf.getX(), wolf.getY(1.0), wolf.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
	}

	public static void forget(ServerPlayer player) {
		ESCAPE_READY.remove(player.getUUID());
	}

	public static void reset() {
		ESCAPE_READY.clear();
	}
}
