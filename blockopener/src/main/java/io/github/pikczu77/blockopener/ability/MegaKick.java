package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.registry.ModItems;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

/**
 * Piston Launcher's Mega Kick: the mob gets punted across the map and explodes where it lands.
 */
final class MegaKick {
	private static final int COOLDOWN = 10;
	private static final int MAX_FLIGHT_TICKS = 100;
	private static final List<Kicked> ACTIVE = new ArrayList<>();

	static void kick(ServerPlayer player, LivingEntity target) {
		if (player.getCooldowns().isOnCooldown(player.getMainHandItem()) || target instanceof EnderDragon || !target.isAlive()) {
			return;
		}
		player.getCooldowns().addCooldown(player.getMainHandItem(), COOLDOWN);
		ServerLevel level = player.level();
		Vec3 look = player.getViewVector(1.0F);
		double resistance = 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.5;
		Vec3 launch = new Vec3(look.x * 3.4, Math.max(look.y * 2.0, 0.0) + 0.9, look.z * 3.4).scale(resistance);
		target.hurtServer(level, level.damageSources().playerAttack(player), 4.0F);
		target.setDeltaMovement(launch);
		target.hurtMarked = true;
		ACTIVE.removeIf(kicked -> kicked.target == target);
		ACTIVE.add(new Kicked(target, player));

		level.playSound(null, target.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.5F, 0.6F);
		level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.5F, 0.8F);
		level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(0.5), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
	}

	static void tick() {
		Iterator<Kicked> iterator = ACTIVE.iterator();
		while (iterator.hasNext()) {
			Kicked kicked = iterator.next();
			LivingEntity target = kicked.target;
			kicked.ticks++;
			if (target.isRemoved() || kicked.ticks > MAX_FLIGHT_TICKS) {
				iterator.remove();
				continue;
			}
			ServerLevel level = (ServerLevel) target.level();
			level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(0.5), target.getZ(), 2, 0.1, 0.1, 0.1, 0.0);
			boolean crashed = target.horizontalCollision || target.verticalCollision || target.isInWater() || target.isInLava();
			if (kicked.ticks > 3 && crashed) {
				iterator.remove();
				Explosions.explode(level, kicked.owner, target.position().add(0.0, 0.5, 0.0), 2.6F);
			}
		}
	}

	static void clear() {
		ACTIVE.clear();
	}

	private static final class Kicked {
		final LivingEntity target;
		final ServerPlayer owner;
		int ticks;

		Kicked(LivingEntity target, ServerPlayer owner) {
			this.target = target;
			this.owner = owner;
		}
	}

	static boolean isLauncher(ServerPlayer player) {
		return player.getMainHandItem().is(ModItems.PISTON_LAUNCHER);
	}

	private MegaKick() {
	}
}
