package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.registry.ModAttachments;
import io.github.pikczu77.blockopener.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.phys.Vec3;

/**
 * Diamond Leggings: Speed II, a flight toggle key, and fireballs from an empty-hand swing while flying.
 */
public final class DiamondLeggings {
	private static final float FLY_SPEED = 0.075F;
	private static final float DEFAULT_FLY_SPEED = 0.05F;
	private static final int FIREBALL_COOLDOWN = 8;

	public static boolean isWearing(ServerPlayer player) {
		return ModAbilities.wearing(player, EquipmentSlot.LEGS, ModItems.DIAMOND_LEGGINGS);
	}

	public static boolean isFlightOn(ServerPlayer player) {
		return isWearing(player) && Boolean.TRUE.equals(player.getAttached(ModAttachments.DIAMOND_FLIGHT));
	}

	/** Flight key pressed. */
	public static void toggleFlight(ServerPlayer player) {
		if (!isWearing(player)) {
			player.displayClientMessage(Component.translatable("blockopener.flight.need_leggings").withStyle(ChatFormatting.RED), true);
			return;
		}
		boolean on = !Boolean.TRUE.equals(player.getAttached(ModAttachments.DIAMOND_FLIGHT));
		player.setAttached(ModAttachments.DIAMOND_FLIGHT, on);
		PlayerState state = ModAbilities.state(player);
		Abilities abilities = player.getAbilities();
		if (on) {
			grant(player, state);
			abilities.flying = true;
			player.onUpdateAbilities();
			player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.35, 0.0));
			player.hurtMarked = true;
		} else {
			revoke(player, state);
		}
		player.displayClientMessage(Component.translatable(on ? "blockopener.flight.on" : "blockopener.flight.off")
			.withStyle(on ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
		player.level().playSound(null, player.blockPosition(), on ? SoundEvents.ELYTRA_FLYING : SoundEvents.ARMOR_EQUIP_DIAMOND.value(),
			SoundSource.PLAYERS, 0.4F, 1.6F);
	}

	static void tick(ServerPlayer player, PlayerState state) {
		boolean wearing = isWearing(player);
		if (wearing && player.tickCount % 10 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 1, true, false, true));
		}
		boolean on = wearing && Boolean.TRUE.equals(player.getAttached(ModAttachments.DIAMOND_FLIGHT));
		if (on && !state.grantedFlight) {
			grant(player, state);
		} else if (!on && state.grantedFlight) {
			revoke(player, state);
		}
		if (on && player.getAbilities().flying && player.tickCount % 3 == 0) {
			player.level().sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 0.4, player.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
		}
	}

	private static void grant(ServerPlayer player, PlayerState state) {
		if (player.isCreative() || player.isSpectator()) {
			return;
		}
		Abilities abilities = player.getAbilities();
		abilities.mayfly = true;
		abilities.setFlyingSpeed(FLY_SPEED);
		player.onUpdateAbilities();
		state.grantedFlight = true;
	}

	private static void revoke(ServerPlayer player, PlayerState state) {
		state.grantedFlight = false;
		if (player.isCreative() || player.isSpectator()) {
			return;
		}
		Abilities abilities = player.getAbilities();
		abilities.mayfly = false;
		abilities.flying = false;
		abilities.setFlyingSpeed(DEFAULT_FLY_SPEED);
		player.onUpdateAbilities();
		// A short grace period so switching flight off mid-air is not a death sentence right away.
		state.noFallUntil = player.level().getGameTime() + 10;
	}

	/** Empty-hand swing while flying. */
	public static void fireball(ServerPlayer player) {
		PlayerState state = ModAbilities.state(player);
		ServerLevel level = player.level();
		if (!isFlightOn(player) || !player.getAbilities().flying || !player.getMainHandItem().isEmpty()
			|| level.getGameTime() < state.fireballReadyAt) {
			return;
		}
		state.fireballReadyAt = level.getGameTime() + FIREBALL_COOLDOWN;
		Vec3 look = player.getViewVector(1.0F);
		LargeFireball fireball = new LargeFireball(level, player, look, 2);
		Vec3 start = player.getEyePosition().add(look.scale(1.5));
		fireball.setPos(start.x, start.y, start.z);
		Explosions.markOwned(fireball);
		level.addFreshEntity(fireball);
		level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.8F, 1.2F);
	}

	private DiamondLeggings() {
	}
}
