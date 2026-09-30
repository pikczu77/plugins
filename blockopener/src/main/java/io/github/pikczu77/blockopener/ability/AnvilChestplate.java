package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.opening.BlockOpening;
import io.github.pikczu77.blockopener.registry.ModItems;
import io.github.pikczu77.blockopener.settings.ModSettings;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Anvil Chestplate: sneaking on the ground is a Resistant Crouch, sneaking mid-jump slams you into
 * the ground like a falling anvil with an explosion on impact.
 */
final class AnvilChestplate {
	private static final int SLAM_COOLDOWN = 12;
	private static final float SLAM_POWER = 3.2F;
	private static final double SLAM_RADIUS = 2.4;

	static void tick(ServerPlayer player, PlayerState state, boolean shift) {
		if (!ModAbilities.wearing(player, EquipmentSlot.CHEST, ModItems.ANVIL_CHESTPLATE)) {
			state.slamming = false;
			return;
		}
		ServerLevel level = player.level();
		long now = level.getGameTime();

		if (state.slamming) {
			if (player.getAbilities().flying || player.isInWater() || player.onClimbable()) {
				state.slamming = false;
			} else if (player.onGround()) {
				land(player, state, level, now);
			} else {
				level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.5, player.getZ(), 3, 0.2, 0.3, 0.2, 0.0);
			}
			return;
		}

		boolean airborne = !player.onGround() && !player.getAbilities().flying && !player.isFallFlying()
			&& !player.isInWater() && !player.onClimbable() && !player.isPassenger();
		if (shift && !state.prevShift && airborne && now >= state.slamReadyAt) {
			state.slamming = true;
			Vec3 motion = player.getDeltaMovement();
			player.setDeltaMovement(motion.x * 0.2, -2.6, motion.z * 0.2);
			player.hurtMarked = true;
			level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6F, 0.6F);
			return;
		}

		if (shift && player.onGround()) {
			// Resistant Crouch: tanky but slow.
			if (player.tickCount % 5 == 0) {
				player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 12, 2, true, false, true));
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 12, 1, true, false, true));
			}
		}
	}

	private static void land(ServerPlayer player, PlayerState state, ServerLevel level, long now) {
		state.slamming = false;
		state.slamReadyAt = now + SLAM_COOLDOWN;
		// The slam digs a crater, falling into it is part of the move.
		state.noFallUntil = now + 40;
		Vec3 at = player.position();
		// The slam does not just break the ground, it OPENS it: every block around pops its loot.
		if (ModSettings.get(level.getServer()).explosionsBreakBlocks()) {
			BlockOpening.openArea(level, at.subtract(0.0, 0.5, 0.0), SLAM_RADIUS, player);
		}
		Explosions.explode(level, player, at, SLAM_POWER, false);
		level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2F, 0.5F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ANVIL.defaultBlockState()), at.x, at.y + 0.1, at.z, 60, 1.5, 0.1, 1.5, 0.2);
		// Shockwave: everything around gets thrown up and away.
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5.0), e -> e != player && e.isAlive())) {
			Vec3 push = entity.position().subtract(at).multiply(1.0, 0.0, 1.0).normalize().scale(0.9).add(0.0, 0.7, 0.0);
			entity.push(push.x, push.y, push.z);
			entity.hurtMarked = true;
		}
	}

	private AnvilChestplate() {
	}
}
