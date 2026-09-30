package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Copper Magnet: held, it pulls loot towards you; sneaking turns it into a Turbo Pull for mobs and
 * players; right-click is the Area De-weaponizer that rips weapons out of everyone's hands.
 */
public final class CopperMagnet {
	private static final double ITEM_RANGE = 10.0;
	private static final double PULL_RANGE = 16.0;
	private static final double DEWEAPONIZE_RANGE = 14.0;

	static void tick(ServerPlayer player, boolean shift) {
		if (!ModAbilities.holding(player, ModItems.COPPER_MAGNET)) {
			return;
		}
		ServerLevel level = player.level();
		Vec3 center = player.position().add(0.0, 0.6, 0.0);
		double itemRange = shift ? PULL_RANGE : ITEM_RANGE;
		for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(itemRange),
			e -> e instanceof ItemEntity item && item.getOwner() != player || e instanceof ExperienceOrb)) {
			pull(entity, center, shift ? 0.18 : 0.07, 1.2);
		}
		if (!shift) {
			return;
		}
		// Turbo Pull
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(PULL_RANGE), e -> canPull(player, e))) {
			pull(entity, center, 0.16, 1.4);
			if (player.tickCount % 4 == 0) {
				Vec3 mid = entity.position().add(center).scale(0.5);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, mid.x, mid.y + 0.5, mid.z, 2, 0.3, 0.3, 0.3, 0.05);
			}
		}
		if (player.tickCount % 10 == 0) {
			level.playSound(null, player.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.8F);
		}
	}

	private static boolean canPull(Player player, LivingEntity entity) {
		if (entity == player || !entity.isAlive() || entity.isPassenger()) {
			return false;
		}
		return !(entity instanceof Player other) || !other.isCreative() && !other.isSpectator();
	}

	private static void pull(Entity entity, Vec3 center, double strength, double maxSpeed) {
		Vec3 offset = center.subtract(entity.position());
		if (offset.lengthSqr() < 1.5) {
			return;
		}
		Vec3 motion = entity.getDeltaMovement().add(offset.normalize().scale(strength));
		if (motion.length() > maxSpeed) {
			motion = motion.normalize().scale(maxSpeed);
		}
		entity.setDeltaMovement(motion);
		entity.hurtMarked = true;
		if (entity instanceof LivingEntity living) {
			living.resetFallDistance();
		}
	}

	/** Right-click: every mob and player around drops what they hold, and it flies to you. */
	public static int deweaponize(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 center = player.position().add(0.0, 1.0, 0.0);
		int disarmed = 0;
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(DEWEAPONIZE_RANGE), e -> canPull(player, e))) {
			boolean any = false;
			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
				ItemStack held = entity.getItemBySlot(slot);
				if (held.isEmpty()) {
					continue;
				}
				entity.setItemSlot(slot, ItemStack.EMPTY);
				ItemEntity item = new ItemEntity(level, entity.getX(), entity.getEyeY(), entity.getZ(), held);
				Vec3 toPlayer = center.subtract(item.position());
				item.setDeltaMovement(toPlayer.scale(0.12).add(0.0, 0.25, 0.0));
				item.setPickUpDelay(entity instanceof Player ? 40 : 10);
				level.addFreshEntity(item);
				any = true;
			}
			if (any) {
				disarmed++;
				for (int i = 0; i <= 8; i++) {
					Vec3 at = entity.getEyePosition().lerp(center, i / 8.0);
					level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
				}
			}
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 40, 1.5, 0.8, 1.5, 0.3);
		level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 1.8F);
		level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4F, 2.0F);
		return disarmed;
	}

	private CopperMagnet() {
	}
}
