package io.github.pikczu77.upgrades.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.upgrades.entity.HomingFireball;
import io.github.pikczu77.upgrades.upgrade.Bonus;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

/**
 * Empty-hand shooting: the forehead bow (trigger finger) and the dragon wing fireballs.
 */
public final class Shooting {
	private static final Map<UUID, Long> ARROW_READY = new HashMap<>();
	private static final Map<UUID, Long> FIREBALL_READY = new HashMap<>();
	private static final Map<UUID, Long> ROD_READY = new HashMap<>();
	private static final double ROD_RANGE = 24.0;

	private Shooting() {
	}

	public static void arrow(ServerPlayer player) {
		ServerLevel level = player.level();

		if (!ready(ARROW_READY, player, 5)) {
			return;
		}

		// Ol' Betsy bonus: three arrows like a multishot crossbow.
		float[] spread = UpgradeManager.hasSpecial(player, Bonus.Special.MULTISHOT) ? new float[] {0.0F, -10.0F, 10.0F} : new float[] {0.0F};

		for (float yaw : spread) {
			Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), null);
			// Shot from the bow on the forehead, a bit above the eyes.
			arrow.setPos(player.getX(), player.getEyeY() + 0.1, player.getZ());
			arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + yaw, 0.0F, 3.0F, 1.0F);
			arrow.setCritArrow(true);
			arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
			level.addFreshEntity(arrow);
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
				1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
		player.swing(InteractionHand.MAIN_HAND, true);
	}

	public static void homingFireballs(ServerPlayer player) {
		ServerLevel level = player.level();

		if (!ready(FIREBALL_READY, player, 6)) {
			return;
		}

		Vec3 look = player.getLookAngle();
		HomingFireball fireball = new HomingFireball(level, player, look);
		fireball.setPos(player.getX() + look.x * 1.2, player.getEyeY() - 0.1 + look.y * 1.2, player.getZ() + look.z * 1.2);
		level.addFreshEntity(fireball);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.8F, 1.2F);
		player.swing(InteractionHand.MAIN_HAND, true);
	}

	/** Tactical fishing: the rod arm hooks the mob you look at and yanks it to you. */
	public static void fishingRod(ServerPlayer player) {
		if (!ready(ROD_READY, player, 15)) {
			return;
		}

		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 end = eye.add(look.scale(ROD_RANGE));
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, player.getBoundingBox().expandTowards(look.scale(ROD_RANGE)).inflate(1.0),
				entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator(), 0.0F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.8F, 1.0F);
		player.swing(InteractionHand.MAIN_HAND, true);

		if (hit == null) {
			return;
		}

		Entity target = hit.getEntity();
		Vec3 pull = player.position().subtract(target.position());
		double distance = pull.length();
		// Same kind of yank as a fishing rod, a bit stronger.
		target.setDeltaMovement(target.getDeltaMovement().add(pull.x * 0.15, pull.y * 0.15 + Math.sqrt(distance) * 0.1, pull.z * 0.15));
		target.hurtMarked = true;

		for (int i = 0; i <= 12; i++) {
			Vec3 point = eye.lerp(target.getBoundingBox().getCenter(), i / 12.0);
			level.sendParticles(ParticleTypes.FISHING, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}

		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	private static boolean ready(Map<UUID, Long> cooldowns, ServerPlayer player, int ticks) {
		long now = player.level().getGameTime();
		Long ready = cooldowns.get(player.getUUID());

		if (ready != null && ready > now) {
			return false;
		}

		cooldowns.put(player.getUUID(), now + ticks);
		return true;
	}

	public static void forget(ServerPlayer player) {
		ARROW_READY.remove(player.getUUID());
		FIREBALL_READY.remove(player.getUUID());
		ROD_READY.remove(player.getUUID());
	}

	public static void reset() {
		ARROW_READY.clear();
		FIREBALL_READY.clear();
		ROD_READY.clear();
	}
}
