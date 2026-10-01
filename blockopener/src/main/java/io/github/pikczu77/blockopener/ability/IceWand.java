package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/** Ice Wand: freezes the mob you point at inside a block of ice, and you walk on water while holding it. */
public final class IceWand {
	private static final double RANGE = 40.0;
	private static final int FREEZE_TICKS = 100;

	/** @return false when nothing was in sight */
	public static boolean freeze(ServerPlayer player) {
		ServerLevel level = player.level();
		Entity aimed = Aim.entity(player, RANGE, entity -> entity instanceof LivingEntity && entity != player && entity.isAlive());
		Vec3 end = aimed != null ? aimed.position().add(0.0, aimed.getBbHeight() / 2, 0.0) : Aim.point(player, RANGE);
		beam(level, player.getEyePosition().subtract(0.0, 0.2, 0.0), end);
		if (!(aimed instanceof LivingEntity target)) {
			return false;
		}
		freeze(player, target);
		return true;
	}

	public static void freeze(@Nullable ServerPlayer owner, LivingEntity target) {
		ServerLevel level = (ServerLevel) target.level();
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FREEZE_TICKS, 10, false, false, true));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FREEZE_TICKS, 10, false, false, true));
		target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, FREEZE_TICKS, 4, false, false, true));
		target.setTicksFrozen(target.getTicksRequiredToFreeze() + FREEZE_TICKS);
		target.setDeltaMovement(Vec3.ZERO);
		target.hurtMarked = true;
		if (target instanceof Mob mob) {
			mob.getNavigation().stop();
		}
		target.hurtServer(level, level.damageSources().source(DamageTypes.FREEZE, owner), 4.0F);
		encase(level, target);
		level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.5), target.getZ(), 60, 0.6, 0.8, 0.6, 0.05);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0F, 0.8F);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.2F, 0.6F);
	}

	/** A shell of ice around the target's hitbox (skipped for huge mobs). */
	private static void encase(ServerLevel level, LivingEntity target) {
		AABB box = target.getBoundingBox();
		if (box.getXsize() > 3.0 || box.getYsize() > 4.0) {
			return;
		}
		BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
		BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
		BlockState ice = Blocks.ICE.defaultBlockState();
		for (BlockPos pos : BlockPos.betweenClosed(min.offset(-1, 0, -1), max.offset(1, 1, 1))) {
			if (!new AABB(pos).intersects(box)) {
				TempBlocks.place(level, pos, ice, FREEZE_TICKS + level.random.nextInt(10));
			}
		}
	}

	private static void beam(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 step = to.subtract(from);
		int points = Mth.clamp((int) (step.length() * 2), 1, 80);
		for (int i = 1; i <= points; i++) {
			Vec3 at = from.add(step.scale(i / (double) points));
			level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
		level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.6F);
	}

	/** Frost walking: still water under you turns into frosted ice (which melts back on its own, like Frost Walker). */
	static void tick(ServerPlayer player) {
		if (!ModAbilities.holding(player, ModItems.ICE_WAND) || player.isSpectator()) {
			return;
		}
		ServerLevel level = player.level();
		BlockState frosted = Blocks.FROSTED_ICE.defaultBlockState();
		BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.5, player.getZ());
		int radius = 3;
		for (BlockPos pos : BlockPos.betweenClosed(below.offset(-radius, 0, -radius), below.offset(radius, 0, radius))) {
			if (pos.distToCenterSqr(player.getX(), pos.getY() + 0.5, player.getZ()) > radius * radius) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.WATER) && state.getFluidState().isSource() && level.getBlockState(pos.above()).isAir()
				&& level.isUnobstructed(frosted, pos, CollisionContext.empty())) {
				level.setBlockAndUpdate(pos, frosted);
			}
		}
	}

	private IceWand() {
	}
}
