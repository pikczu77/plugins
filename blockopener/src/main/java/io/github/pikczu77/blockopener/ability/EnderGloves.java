package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Ender Gloves: teleport where you look, swap places with a mob, endermen leave you alone. */
public final class EnderGloves {
	private static final double RANGE = 64.0;

	/** @return false if there is nowhere to land */
	public static boolean teleport(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockHitResult hit = Aim.block(player, RANGE, ClipContext.Fluid.NONE);
		if (hit.getType() == HitResult.Type.MISS) {
			return false;
		}
		BlockPos landing = hit.getBlockPos().relative(hit.getDirection());
		for (int up = 0; up <= 3; up++) {
			BlockPos candidate = landing.above(up);
			Vec3 feet = Vec3.atBottomCenterOf(candidate);
			AABB box = player.getBoundingBox().move(feet.subtract(player.position()));
			if (level.noCollision(player, box)) {
				warp(player, feet);
				return true;
			}
		}
		return false;
	}

	public static void swap(ServerPlayer player, LivingEntity target) {
		Vec3 mine = player.position();
		Vec3 theirs = target.position();
		target.teleportTo(mine.x, mine.y, mine.z);
		warp(player, theirs);
	}

	private static void warp(ServerPlayer player, Vec3 to) {
		ServerLevel level = player.level();
		level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.3, 0.8, 0.3, 0.5);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
		player.teleportTo(to.x, to.y, to.z);
		player.resetFallDistance();
		ModAbilities.state(player).noFallUntil = level.getGameTime() + 10;
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1.0, to.z, 40, 0.3, 0.8, 0.3, 0.1);
		level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
	}

	static void tick(ServerPlayer player) {
		if (player.tickCount % 10 == 0 && ModAbilities.holding(player, ModItems.ENDER_GLOVES)) {
			Stealth.loseTrack(player, 0.0, mob -> mob instanceof EnderMan);
		}
	}

	private EnderGloves() {
	}
}
