package io.github.pikczu77.blockopener.ability;

import com.mojang.math.Transformation;
import io.github.pikczu77.blockopener.opening.TransientEntities;
import io.github.pikczu77.blockopener.registry.ModAttachments;
import io.github.pikczu77.blockopener.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Pumpkin Boots: sprinting drops a trail of explosive pumpkins, sneaking for 3 seconds turns you
 * into a pumpkin that mobs lose track of.
 */
final class PumpkinBoots {
	private static final int TRANSFORM_TICKS = 60;
	private static final int TRAIL_INTERVAL = 6;

	static void tick(ServerPlayer player, PlayerState state, boolean shift) {
		boolean wearing = ModAbilities.wearing(player, EquipmentSlot.FEET, ModItems.PUMPKIN_BOOTS);
		if (!wearing) {
			state.sneakTicks = 0;
			leaveForm(player, state);
			return;
		}
		ServerLevel level = player.level();

		boolean fastFlying = player.getAbilities().flying && player.getDeltaMovement().horizontalDistanceSqr() > 0.04;
		if ((player.isSprinting() || fastFlying) && player.tickCount % TRAIL_INTERVAL == 0) {
			Vec3 behind = player.position().subtract(player.getViewVector(1.0F).multiply(1.0, 0.0, 1.0).normalize().scale(0.8));
			Bombs.spawn(level, player, behind.add(0.0, 0.1, 0.0), new Vec3(0.0, 0.05, 0.0),
				Blocks.JACK_O_LANTERN.defaultBlockState(), 30, 2.4F);
			level.playSound(null, behind.x, behind.y, behind.z, SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 0.4F, 1.6F);
		}

		if (shift && player.onGround() && !player.getAbilities().flying) {
			state.sneakTicks++;
			if (state.pumpkin == null && state.sneakTicks >= TRANSFORM_TICKS) {
				enterForm(player, state);
			} else if (state.pumpkin == null && state.sneakTicks % 20 == 0) {
				level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.5, player.getZ(), 6, 0.3, 0.4, 0.3, 0.01);
			}
		} else {
			state.sneakTicks = 0;
			leaveForm(player, state);
		}

		if (state.pumpkin != null) {
			tickForm(player, state, level);
		}
	}

	private static void enterForm(ServerPlayer player, PlayerState state) {
		ServerLevel level = player.level();
		Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
		display.setPos(player.getX(), player.getY(), player.getZ());
		display.setBlockState(Blocks.PUMPKIN.defaultBlockState());
		display.setTransformation(new Transformation(new Vector3f(-0.5F, 0.0F, -0.5F), null, null, null));
		display.setPosRotInterpolationDuration(2);
		state.pumpkin = TransientEntities.spawn(level, display);
		player.setAttached(ModAttachments.PUMPKIN_FORM, true);
		if (!player.hasEffect(MobEffects.INVISIBILITY)) {
			player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, -1, 0, false, false, false));
			state.addedInvisibility = true;
		}
		level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.5, player.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
		level.playSound(null, player.blockPosition(), SoundEvents.PUMPKIN_CARVE, SoundSource.PLAYERS, 1.0F, 0.8F);
	}

	private static void tickForm(ServerPlayer player, PlayerState state, ServerLevel level) {
		if (state.pumpkin.isRemoved() || state.pumpkin.level() != level) {
			// Changed dimension (or the display got unloaded): drop the disguise, sneaking again restores it.
			leaveForm(player, state);
			return;
		}
		state.pumpkin.setPos(player.getX(), player.getY(), player.getZ());
		if (player.tickCount % 5 == 0) {
			// Nothing to see here, just a pumpkin.
			Stealth.loseTrack(player, 0.0, mob -> true);
		}
	}

	static void leaveForm(ServerPlayer player, PlayerState state) {
		if (state.pumpkin == null) {
			return;
		}
		TransientEntities.remove(state.pumpkin);
		state.pumpkin = null;
		player.removeAttached(ModAttachments.PUMPKIN_FORM);
		if (state.addedInvisibility) {
			player.removeEffect(MobEffects.INVISIBILITY);
			state.addedInvisibility = false;
		}
		ServerLevel level = player.level();
		level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.5, player.getZ(), 12, 0.3, 0.4, 0.3, 0.03);
		level.playSound(null, player.blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
	}

	private PumpkinBoots() {
	}
}
