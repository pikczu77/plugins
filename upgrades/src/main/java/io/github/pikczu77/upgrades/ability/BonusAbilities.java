package io.github.pikczu77.upgrades.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

import io.github.pikczu77.upgrades.entity.CombatClone;
import io.github.pikczu77.upgrades.mixin.AbstractArrowAccessor;
import io.github.pikczu77.upgrades.upgrade.Bonus;
import io.github.pikczu77.upgrades.upgrade.Bonus.Special;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;
import io.github.pikczu77.upgrades.util.Screens;
import io.github.pikczu77.upgrades.util.TempDisplays;

/**
 * The bonus abilities that need code: combat effects, movement tricks, auras, pets and so on.
 */
public final class BonusAbilities {
	private static final String BUDDY_TAG = "upgrades_buddy";
	private static final int TOTEM_COOLDOWN = 20 * 60 * 10;
	private static final int ESCAPE_COOLDOWN = 20 * 30;

	private static final Map<UUID, Long> TOTEM_READY = new HashMap<>();
	private static final Map<UUID, Long> ESCAPE_READY = new HashMap<>();
	private static final Map<UUID, Boolean> JUMP_HELD = new HashMap<>();
	private static final Map<UUID, Boolean> DOUBLE_JUMP_USED = new HashMap<>();

	private record StraightArrow(ResourceKey<Level> dimension, UUID id, long gravityAt) {
	}

	private static final List<StraightArrow> STRAIGHT_ARROWS = new ArrayList<>();
	private static boolean extraHit;

	private BonusAbilities() {
	}

	private static boolean has(Player player, Special special) {
		return UpgradeManager.hasSpecial(player, special);
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(BonusAbilities::allowDamage);
		ServerLivingEntityEvents.ALLOW_DEATH.register(BonusAbilities::allowDeath);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(BonusAbilities::afterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(BonusAbilities::afterDeath);
		ServerEntityEvents.ENTITY_LOAD.register(BonusAbilities::onEntityLoad);
		PlayerBlockBreakEvents.BEFORE.register(BonusAbilities::beforeBreak);
	}

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayer player)) {
			return true;
		}

		if (source.is(DamageTypes.LIGHTNING_BOLT) && (has(player, Special.LIGHTNING_IMMUNE) || has(player, Special.LIGHTNING_HIT))) {
			return false;
		}

		if (source.is(DamageTypeTags.IS_FALL) && has(player, Special.NO_FALL)) {
			return false;
		}

		return !(source.is(DamageTypes.WITHER) && has(player, Special.WITHER_IMMUNE));
	}

	/** Second life: a totem that recharges instead of being used up. */
	private static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayer player) || !has(player, Special.TOTEM) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return true;
		}

		long now = player.level().getGameTime();
		Long ready = TOTEM_READY.get(player.getUUID());

		if (ready != null && ready > now) {
			return true;
		}

		TOTEM_READY.put(player.getUUID(), now + TOTEM_COOLDOWN);
		player.setHealth(1.0F);
		player.removeAllEffects();
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
		player.level().broadcastEntityEvent(player, (byte) 35);
		Screens.actionBar(player, Component.literal("Drugie Życie zużyte — wróci za 10 minut").withStyle(ChatFormatting.GOLD));
		return false;
	}

	private static void afterDamage(LivingEntity victim, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (blocked) {
			return;
		}

		ServerLevel level = (ServerLevel) victim.level();

		// The player hits something in melee.
		if (!extraHit && source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player && victim != player
				&& source.is(DamageTypes.PLAYER_ATTACK)) {
			if (has(player, Special.STING)) {
				victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), player);
			}

			if (has(player, Special.WITHER_HIT)) {
				victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), player);
			}

			if (has(player, Special.FIRE_HIT)) {
				victim.igniteForSeconds(4.0F);
			}

			if (has(player, Special.LIGHTNING_HIT) && level.getRandom().nextFloat() < 0.15F) {
				LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);

				if (bolt != null) {
					bolt.snapTo(victim.position());
					bolt.setCause(player);
					level.addFreshEntity(bolt);
				}
			}

			if (has(player, Special.SMASH) && player.fallDistance > 1.5) {
				// Like a mace: the longer the fall, the harder the hit.
				float extra = (float) Math.min(24.0, player.fallDistance * 1.5);
				extraHit = true;

				try {
					victim.hurtServer(level, source, baseDamage + extra);
				} finally {
					extraHit = false;
				}

				player.resetFallDistance();
				level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 1.0F);
				level.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY(0.5), victim.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			}
		}

		// Something hurts the player.
		if (victim instanceof ServerPlayer player) {
			if (source.getEntity() instanceof LivingEntity attacker && attacker != player && source.getDirectEntity() == attacker
					&& has(player, Special.THORNS_WITHER)) {
				attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1), player);
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, attacker.getX(), attacker.getY(0.5), attacker.getZ(), 15, 0.3, 0.4, 0.3, 0.02);
			}

			if (player.getHealth() <= 6.0F && player.isAlive() && has(player, Special.ESCAPE)) {
				escape(player);
			}
		}
	}

	private static void escape(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long ready = ESCAPE_READY.get(player.getUUID());

		if (ready != null && ready > now) {
			return;
		}

		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();

		for (int attempt = 0; attempt < 16; attempt++) {
			double tx = x + (player.getRandom().nextDouble() - 0.5) * 24.0;
			double ty = y + player.getRandom().nextInt(9) - 4;
			double tz = z + (player.getRandom().nextDouble() - 0.5) * 24.0;

			if (player.randomTeleport(tx, ty, tz, true)) {
				ESCAPE_READY.put(player.getUUID(), now + ESCAPE_COOLDOWN);
				player.level().playSound(null, x, y, z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
				player.resetFallDistance();
				return;
			}
		}
	}

	private static void afterDeath(LivingEntity entity, DamageSource source) {
		if (entity instanceof Mob && source.getEntity() instanceof ServerPlayer player && has(player, Special.XP_BONUS)) {
			ServerLevel level = (ServerLevel) entity.level();
			level.addFreshEntity(new ExperienceOrb(level, entity.getX(), entity.getY(0.5), entity.getZ(), 3 + level.getRandom().nextInt(6)));
		}
	}

	/** Arrows shot by a player with arrow bonuses. */
	private static void onEntityLoad(Entity entity, ServerLevel level) {
		if (!(entity instanceof AbstractArrow arrow) || arrow.tickCount > 0 || !(arrow.getOwner() instanceof ServerPlayer player)) {
			return;
		}

		if (has(player, Special.ARROW_POWER)) {
			AbstractArrowAccessor accessor = (AbstractArrowAccessor) arrow;
			arrow.setBaseDamage(accessor.upgrades$getBaseDamage() * 1.5);
		}

		if (has(player, Special.ARROW_SPEED)) {
			arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.5));
		}

		if (has(player, Special.ARROW_PIERCE)) {
			((AbstractArrowAccessor) arrow).upgrades$setPierceLevel((byte) 3);
		}

		if (has(player, Special.ARROW_STRAIGHT)) {
			arrow.setNoGravity(true);
			STRAIGHT_ARROWS.add(new StraightArrow(level.dimension(), arrow.getUUID(), level.getGameTime() + 80));
		}
	}

	/** Sneak + empty hand = silk touch (blocks with contents are left to vanilla). */
	private static boolean beforeBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
		if (!(player instanceof ServerPlayer serverPlayer) || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()
				|| player.isCreative() || blockEntity != null || state.getDestroySpeed(level, pos) < 0.0F || !has(serverPlayer, Special.SILK_FIST)) {
			return true;
		}

		ItemStack drop = new ItemStack(state.getBlock());

		if (drop.isEmpty()) {
			return true;
		}

		level.destroyBlock(pos, false, player);
		Block.popResource(level, pos, drop);
		return false;
	}

	public static void tick(ServerPlayer player) {
		long[] bits = UpgradeManager.bonuses(player);

		if (Bonus.count(bits) == 0) {
			return;
		}

		BonusPassives.tick(player, bits, false);
		ServerLevel level = player.level();
		int age = player.tickCount;

		if (Bonus.hasSpecial(bits, Special.DOUBLE_JUMP)) {
			doubleJump(player);
		}

		if (Bonus.hasSpecial(bits, Special.SLOW_FALL_SNEAK) && !player.onGround() && player.isShiftKeyDown()
				&& player.getDeltaMovement().y < -0.1 && !player.getAbilities().flying) {
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, true, false, false));
		}

		if (Bonus.hasSpecial(bits, Special.MAGNET) && age % 2 == 0) {
			magnet(player);
		}

		if (Bonus.hasSpecial(bits, Special.REGEN_SLOW) && age % 160 == 0 && player.getHealth() < player.getMaxHealth()) {
			player.heal(1.0F);
			level.sendParticles(ParticleTypes.NOTE, player.getX(), player.getY(2.1), player.getZ(), 1, 0.3, 0.1, 0.3, 1.0);
		}

		if (Bonus.hasSpecial(bits, Special.WATER_REGEN) && age % 40 == 0 && player.isInWaterOrRain()) {
			player.heal(1.0F);
		}

		if (Bonus.hasSpecial(bits, Special.SATIATED) && age % 600 == 0) {
			player.getFoodData().eat(1, 0.2F);
		}

		if (Bonus.hasSpecial(bits, Special.NO_HUNGER) && age % 40 == 0) {
			player.getFoodData().setFoodLevel(20);
			player.getFoodData().setSaturation(5.0F);
		}

		if (Bonus.hasSpecial(bits, Special.SPOT_MOBS) && age % 40 == 0) {
			for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20.0), mob -> mob instanceof Enemy)) {
				mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
			}
		}

		if (age % 10 == 0) {
			calmMobs(player, bits);
		}

		if (Bonus.hasSpecial(bits, Special.CLEANSE) && age % 20 == 0) {
			List<Holder<MobEffect>> harmful = new ArrayList<>();

			for (MobEffectInstance effect : player.getActiveEffects()) {
				if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
					harmful.add(effect.getEffect());
				}
			}

			harmful.forEach(player::removeEffect);
		}

		if (Bonus.hasSpecial(bits, Special.WITHER_IMMUNE) && player.hasEffect(MobEffects.WITHER)) {
			player.removeEffect(MobEffects.WITHER);
		}

		if (Bonus.hasSpecial(bits, Special.AUTO_REPAIR) && age % 100 == 0) {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				ItemStack stack = player.getItemBySlot(slot);

				if (stack.isDamaged()) {
					stack.setDamageValue(stack.getDamageValue() - 1);
				}
			}
		}

		if (Bonus.hasSpecial(bits, Special.BEACON_AURA) && age % 60 == 0) {
			beaconAura(player, Bonus.hasSpecial(bits, Special.BEACON_FULL));
		}

		if (Bonus.hasSpecial(bits, Special.ORE_SENSE) && age % 60 == 0) {
			oreSense(player);
		}

		if (age % 100 == 0) {
			int wolves = Bonus.hasSpecial(bits, Special.WOLF_PACK) ? 3 : Bonus.hasSpecial(bits, Special.WOLF_BUDDY) ? 1 : 0;

			if (wolves > 0) {
				wolfBuddies(player, wolves);
			}
		}
	}

	/** A second jump in the air (reads the jump key the client sends). */
	private static void doubleJump(ServerPlayer player) {
		Input input = player.getLastClientInput();
		UUID id = player.getUUID();
		boolean wasHeld = JUMP_HELD.getOrDefault(id, false);
		JUMP_HELD.put(id, input.jump());

		if (player.onGround() || player.isInWater() || player.onClimbable()) {
			DOUBLE_JUMP_USED.put(id, false);
			return;
		}

		if (input.jump() && !wasHeld && !DOUBLE_JUMP_USED.getOrDefault(id, false) && !player.getAbilities().flying && !player.isFallFlying()) {
			DOUBLE_JUMP_USED.put(id, true);
			Vec3 look = player.getLookAngle().horizontal().normalize().scale(0.25);
			player.setDeltaMovement(player.getDeltaMovement().x + look.x, 0.6, player.getDeltaMovement().z + look.z);
			player.hurtMarked = true;
			player.resetFallDistance();
			ServerLevel level = player.level();
			level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 10, 0.3, 0.05, 0.3, 0.02);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
		}
	}

	private static void magnet(ServerPlayer player) {
		Vec3 target = player.position().add(0.0, 0.5, 0.0);
		AABB area = player.getBoundingBox().inflate(7.0);

		for (Entity entity : player.level().getEntities(player, area, entity -> entity instanceof ItemEntity item && !item.hasPickUpDelay()
				|| entity instanceof ExperienceOrb)) {
			Vec3 pull = target.subtract(entity.position());

			if (pull.lengthSqr() > 1.0) {
				entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5).add(pull.normalize().scale(0.35)));
			}
		}
	}

	private static void calmMobs(ServerPlayer player, long[] bits) {
		ServerLevel level = player.level();

		if (Bonus.hasSpecial(bits, Special.CREEPER_CALM)) {
			for (Creeper creeper : level.getEntitiesOfClass(Creeper.class, player.getBoundingBox().inflate(8.0))) {
				if (creeper.getTarget() == player) {
					creeper.setTarget(null);
				}

				creeper.setSwellDir(-1);
			}
		}

		if (Bonus.hasSpecial(bits, Special.PIGLIN_CALM)) {
			for (AbstractPiglin piglin : level.getEntitiesOfClass(AbstractPiglin.class, player.getBoundingBox().inflate(16.0))) {
				if (piglin.getTarget() == player) {
					piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
					piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
					piglin.setTarget(null);
				}
			}
		}

		if (Bonus.hasSpecial(bits, Special.GHAST_CALM)) {
			for (Ghast ghast : level.getEntitiesOfClass(Ghast.class, player.getBoundingBox().inflate(64.0))) {
				if (ghast.getTarget() == player) {
					ghast.setTarget(null);
				}
			}
		}
	}

	/** Regeneration (and strength with the full beacon) for the player, their clones, pets and nearby players. */
	private static void beaconAura(ServerPlayer player, boolean full) {
		for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12.0),
				entity -> entity instanceof Player || entity instanceof CombatClone clone && clone.isOwnedBy(player)
						|| entity instanceof TamableAnimal pet && pet.isOwnedBy(player))) {
			entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, true));

			if (full) {
				entity.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 0, true, true));
			}
		}
	}

	/** The nearest ores glow through the walls for a few seconds. */
	private static void oreSense(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos origin = player.blockPosition();
		List<BlockPos> ores = new ArrayList<>();

		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -8, -8), origin.offset(8, 8, 8))) {
			if (level.getBlockState(pos).is(ConventionalBlockTags.ORES)) {
				ores.add(pos.immutable());
			}
		}

		ores.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));

		for (BlockPos pos : ores.subList(0, Math.min(12, ores.size()))) {
			TempDisplays.outline(level, level.getBlockState(pos), pos, 0xFFD700, 50);
		}
	}

	/** Keeps the player's tamed wolves around: missing ones come back. */
	private static void wolfBuddies(ServerPlayer player, int wanted) {
		MinecraftServer server = player.level().getServer();
		int alive = 0;

		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof Wolf wolf && wolf.isAlive() && wolf.getTags().contains(BUDDY_TAG) && wolf.isOwnedBy(player)) {
					alive++;
				}
			}
		}

		ServerLevel level = player.level();

		for (int i = alive; i < wanted; i++) {
			Wolf wolf = EntityType.WOLF.create(level, EntitySpawnReason.MOB_SUMMONED);

			if (wolf == null) {
				return;
			}

			wolf.snapTo(player.getX() + level.getRandom().nextInt(3) - 1, player.getY(), player.getZ() + level.getRandom().nextInt(3) - 1,
					player.getYRot(), 0.0F);
			wolf.tame(player);
			wolf.addTag(BUDDY_TAG);
			level.addFreshEntity(wolf);
			level.sendParticles(ParticleTypes.HEART, wolf.getX(), wolf.getY(1.0), wolf.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
		}
	}

	/** Straight arrows get their gravity back after a while, so they do not fly forever. */
	public static void tickServer(MinecraftServer server) {
		Iterator<StraightArrow> iterator = STRAIGHT_ARROWS.iterator();

		while (iterator.hasNext()) {
			StraightArrow arrow = iterator.next();
			ServerLevel level = server.getLevel(arrow.dimension());

			if (level == null || level.getGameTime() >= arrow.gravityAt()) {
				iterator.remove();
				Entity entity = level == null ? null : level.getEntity(arrow.id());

				if (entity != null) {
					entity.setNoGravity(false);
				}
			}
		}
	}

	public static void forget(ServerPlayer player) {
		JUMP_HELD.remove(player.getUUID());
		DOUBLE_JUMP_USED.remove(player.getUUID());
	}

	public static void reset() {
		TOTEM_READY.clear();
		ESCAPE_READY.clear();
		JUMP_HELD.clear();
		DOUBLE_JUMP_USED.clear();
		STRAIGHT_ARROWS.clear();
		extraHit = false;
	}
}
