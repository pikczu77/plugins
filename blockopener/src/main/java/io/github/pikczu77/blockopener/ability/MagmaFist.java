package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Magma Fist: every hit sets the target on fire, sneak + right-click is a fire nova, and while you
 * hold it you are fireproof and lava cools into a magma crust under your feet (it melts back behind you).
 */
public final class MagmaFist {
	private static final double NOVA_RADIUS = 6.0;
	private static final int CRUST_TICKS = 60;

	public static void ignite(LivingEntity target) {
		target.igniteForSeconds(6.0F);
		if (target.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.5), target.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
		}
	}

	public static void nova(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 center = player.position();
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(NOVA_RADIUS),
			entity -> entity != player && entity.isAlive() && !Allies.isAllyOf(player, entity) && entity.distanceToSqr(player) <= NOVA_RADIUS * NOVA_RADIUS)) {
			target.hurtServer(level, level.damageSources().playerAttack(player), 6.0F);
			ignite(target);
			Vec3 push = target.position().subtract(center).multiply(1.0, 0.0, 1.0).normalize().scale(1.2).add(0.0, 0.45, 0.0);
			target.push(push.x, push.y, push.z);
			target.hurtMarked = true;
		}
		for (int i = 0; i < 48; i++) {
			double angle = Math.PI * 2 * i / 48;
			Vec3 dir = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
			level.sendParticles(ParticleTypes.FLAME, center.x + dir.x, center.y + 0.3, center.z + dir.z, 0, dir.x, 0.05, dir.z, 0.45);
		}
		level.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.5, center.z, 25, 1.0, 0.3, 1.0, 0.0);
		level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.6F);
		level.playSound(null, player.blockPosition(), SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.5F, 0.8F);
	}

	static void tick(ServerPlayer player) {
		if (!ModAbilities.holding(player, ModItems.MAGMA_FIST) || player.isSpectator()) {
			return;
		}
		if (player.tickCount % 20 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
		}
		ServerLevel level = player.level();
		BlockState crust = Blocks.MAGMA_BLOCK.defaultBlockState();
		BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.5, player.getZ());
		int radius = 2;
		for (BlockPos pos : BlockPos.betweenClosed(below.offset(-radius, 0, -radius), below.offset(radius, 0, radius))) {
			if (pos.distToCenterSqr(player.getX(), pos.getY() + 0.5, player.getZ()) > radius * radius + 1) {
				continue;
			}
			if (TempBlocks.isTemporary(level, pos) && level.getBlockState(pos) == crust) {
				TempBlocks.refresh(level, pos, CRUST_TICKS);
			} else if (level.getBlockState(pos.above()).isAir() && TempBlocks.placeOverSource(level, pos, crust, Blocks.LAVA, CRUST_TICKS)
				&& level.random.nextInt(4) == 0) {
				level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.2, 0.0, 0.2, 0.01);
			}
		}
	}

	private MagmaFist() {
	}
}
