package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.network.ModPayloads;
import io.github.pikczu77.blockopener.registry.ModItems;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Weeping Totem: works from anywhere in the inventory. Once, it cheats death (even the void) and
 * puts you back on the last solid ground you stood on. Perfect for hardcore drama.
 */
public final class WeepingTotem {
	/** @return true if the totem saved the player */
	public static boolean trySave(ServerPlayer player, DamageSource source) {
		if (source.is(DamageTypes.GENERIC_KILL)) {
			return false;
		}
		Inventory inventory = player.getInventory();
		int slot = -1;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(ModItems.WEEPING_TOTEM)) {
				slot = i;
				break;
			}
		}
		if (slot < 0) {
			return false;
		}
		ItemStack used = inventory.getItem(slot).copyWithCount(1);
		inventory.getItem(slot).shrink(1);

		player.setHealth(10.0F);
		player.removeAllEffects();
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 4));
		player.clearFire();
		player.resetFallDistance();
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;

		PlayerState state = ModAbilities.state(player);
		ServerLevel level = player.level();
		boolean voidDeath = source.is(DamageTypes.FELL_OUT_OF_WORLD);
		if (state.safePos != null && level.dimension().equals(state.safeLevel)) {
			player.teleportTo(state.safePos.x, state.safePos.y, state.safePos.z);
		} else if (voidDeath) {
			BlockPos target = level.dimension() == Level.END ? ServerLevel.END_SPAWN_POINT : level.getRespawnData().globalPos().pos();
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, target.getX(), target.getZ());
			player.teleportTo(target.getX() + 0.5, Math.max(y, level.getMinY() + 80), target.getZ() + 0.5);
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0));
		}
		state.noFallUntil = level.getGameTime() + 40;

		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 80, 0.5, 1.0, 0.5, 0.6);
		level.sendParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR, player.getX(), player.getY() + 1.5, player.getZ(), 30, 0.5, 0.5, 0.5, 0.0);
		level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
		if (!(player instanceof FakePlayer) && ServerPlayNetworking.canSend(player, ModPayloads.ItemActivationPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ModPayloads.ItemActivationPayload(used));
		}
		return true;
	}

	/** Remembers the last spot the player stood on safely. */
	static void tick(ServerPlayer player, PlayerState state) {
		if (player.tickCount % 10 != 0 || !player.onGround() || player.isInLava() || player.isOnFire() || player.isInWater()) {
			return;
		}
		BlockPos below = player.blockPosition().below();
		if (player.level().getBlockState(below).isFaceSturdy(player.level(), below, Direction.UP)) {
			state.safePos = player.position();
			state.safeLevel = player.level().dimension();
		}
	}

	private WeepingTotem() {
	}
}
