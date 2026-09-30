package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.registry.ModItems;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Sculk Helmet: a Warden sonic boom on a key, plus immunity to Darkness and sculk sensors
 * (see the entity mixins).
 */
public final class SculkHelmet {
	private static final int COOLDOWN = 40;
	private static final double RANGE = 24.0;
	private static final float DAMAGE = 14.0F;

	public static boolean isWearing(LivingEntity entity) {
		return ModAbilities.wearing(entity, EquipmentSlot.HEAD, ModItems.SCULK_HELMET);
	}

	static void tick(ServerPlayer player) {
		if (isWearing(player) && player.tickCount % 40 == 0) {
			player.level().sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getEyeY() + 0.3, player.getZ(), 1, 0.15, 0.05, 0.15, 0.01);
		}
	}

	/** Sonic boom key pressed. The beam passes through walls, like the Warden's. */
	public static void sonicBoom(ServerPlayer player) {
		if (!isWearing(player)) {
			player.displayClientMessage(Component.translatable("blockopener.sonic_boom.need_helmet").withStyle(ChatFormatting.RED), true);
			return;
		}
		PlayerState state = ModAbilities.state(player);
		ServerLevel level = player.level();
		long now = level.getGameTime();
		if (now < state.sonicBoomReadyAt) {
			player.displayClientMessage(Component.translatable("blockopener.sonic_boom.cooldown",
				String.format("%.1f", (state.sonicBoomReadyAt - now) / 20.0)).withStyle(ChatFormatting.DARK_AQUA), true);
			return;
		}
		state.sonicBoomReadyAt = now + COOLDOWN;

		Vec3 start = player.getEyePosition().subtract(0.0, 0.2, 0.0);
		Vec3 dir = player.getViewVector(1.0F);
		Vec3 end = start.add(dir.scale(RANGE));
		for (int i = 1; i <= RANGE; i++) {
			Vec3 at = start.add(dir.scale(i));
			level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 1.0F);

		AABB area = new AABB(start, end).inflate(1.5);
		Set<Entity> hit = new HashSet<>();
		for (Entity entity : level.getEntities(player, area, e -> e instanceof LivingEntity || e instanceof EnderDragonPart)) {
			if (entity.getBoundingBox().inflate(0.6).clip(start, end).isEmpty() && !entity.getBoundingBox().inflate(0.6).contains(start)) {
				continue;
			}
			Entity root = Aim.root(entity);
			if (!hit.add(root)) {
				continue;
			}
			entity.hurtServer(level, level.damageSources().sonicBoom(player), DAMAGE);
			if (root instanceof LivingEntity living) {
				double resistance = living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
				double horizontal = 2.5 * (1.0 - resistance);
				double vertical = 0.5 * (1.0 - resistance);
				living.push(dir.x * horizontal, dir.y * vertical + 0.2, dir.z * horizontal);
				living.hurtMarked = true;
			}
		}
	}

	private SculkHelmet() {
	}
}
