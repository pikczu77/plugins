package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Slime Gloves: super jump, an emergency slime platform, and you bounce instead of taking fall damage. */
public final class SlimeGloves {
	public static void superJump(ServerPlayer player) {
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x * 1.3, 1.5, motion.z * 1.3);
		player.hurtMarked = true;
		ModAbilities.markLaunched(player);
		effects(player.level(), player.position(), 1.0F);
	}

	public static void platform(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos below = player.blockPosition().below();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos pos = below.offset(dx, 0, dz);
				if (level.mayInteract(player, pos)) {
					TempBlocks.place(level, pos, Blocks.SLIME_BLOCK.defaultBlockState(), 200);
				}
			}
		}
		effects(level, player.position(), 0.6F);
	}

	static boolean holding(ServerPlayer player) {
		return ModAbilities.holding(player, ModItems.SLIME_GLOVES);
	}

	/** Bounces the player back up when landing hard while holding the gloves. */
	static void tick(ServerPlayer player, PlayerState state, boolean shift) {
		boolean onGround = player.onGround();
		if (!onGround) {
			state.airVy = player.getDeltaMovement().y;
		} else if (!state.wasOnGround && holding(player) && !shift && state.airVy < -0.6) {
			Vec3 motion = player.getDeltaMovement();
			player.setDeltaMovement(motion.x, Math.min(1.6, -state.airVy * 0.8), motion.z);
			player.hurtMarked = true;
			effects(player.level(), player.position(), 0.8F);
		}
		state.wasOnGround = onGround;
	}

	private static void effects(ServerLevel level, Vec3 at, float pitch) {
		level.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.2, at.z, 20, 0.5, 0.1, 0.5, 0.1);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 1.0F, pitch);
	}

	private SlimeGloves() {
	}
}
