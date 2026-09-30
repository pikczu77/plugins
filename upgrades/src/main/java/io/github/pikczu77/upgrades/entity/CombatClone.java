package io.github.pikczu77.upgrades.entity;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A copy of its owner (drawn with the owner's skin) that distracts monsters (level 1), fights them (level 2, splits
 * into two smaller clones on death) or fights them while being immune with a netherite sword (level 3).
 * Multiplicity clones breed on their own.
 */
public class CombatClone extends PathfinderMob {
	private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> OWNER =
			SynchedEntityData.defineId(CombatClone.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
	private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(CombatClone.class, EntityDataSerializers.INT);

	/** Breeds more clones every now and then (the dragon egg upgrade). */
	private boolean breeder;
	private int breedCooldown = 200;

	public CombatClone(EntityType<? extends CombatClone> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(OWNER, Optional.empty());
		builder.define(LEVEL, 1);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new FightGoal(this));
		this.goalSelector.addGoal(4, new FollowOwnerGoal(this));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new DefendOwnerGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
				(target, level) -> target instanceof Enemy && this.fights()));
	}

	/** Sets the level and the stats that go with it. */
	public void setup(@Nullable Player owner, int level, float scale, boolean breeder) {
		this.entityData.set(OWNER, Optional.ofNullable(owner).map(EntityReference::of));
		this.entityData.set(LEVEL, level);
		this.breeder = breeder;

		double health = switch (level) {
			case 1 -> 10.0;
			case 2 -> 6.0;
			default -> 20.0;
		};

		setBase(Attributes.MAX_HEALTH, health);
		setBase(Attributes.SCALE, scale);
		setBase(Attributes.ATTACK_DAMAGE, level >= 3 ? 2.0 : 3.0);
		this.setHealth((float) health);
		this.setInvulnerable(level >= 3);

		if (level >= 3) {
			this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
			this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		}

		if (owner != null) {
			this.setCustomName(owner.getName());
			this.setCustomNameVisible(false);
		}
	}

	private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		AttributeInstance instance = this.getAttribute(attribute);

		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	public int cloneLevel() {
		return this.entityData.get(LEVEL);
	}

	/** Level 1 clones only distract, higher levels fight. */
	public boolean fights() {
		return this.cloneLevel() >= 2;
	}

	public boolean isBreeder() {
		return this.breeder;
	}

	public @Nullable UUID ownerUuid() {
		return this.entityData.get(OWNER).map(EntityReference::getUUID).orElse(null);
	}

	public @Nullable Player owner() {
		UUID uuid = this.ownerUuid();
		return uuid == null ? null : this.level().getPlayerByUUID(uuid);
	}

	public boolean isOwnedBy(Entity entity) {
		return entity.getUUID().equals(this.ownerUuid());
	}

	@Override
	protected boolean considersEntityAsAlly(Entity entity) {
		if (this.isOwnedBy(entity)) {
			return true;
		}

		if (entity instanceof CombatClone other && other.ownerUuid() != null && other.ownerUuid().equals(this.ownerUuid())) {
			return true;
		}

		return super.considersEntityAsAlly(entity);
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return !this.isAlliedTo(target) && super.canAttack(target);
	}

	@Override
	public void aiStep() {
		this.updateSwingTime();
		super.aiStep();

		if (!this.level().isClientSide() && this.breeder && --this.breedCooldown <= 0) {
			this.breedCooldown = 200 + this.random.nextInt(300);
			io.github.pikczu77.upgrades.ability.Clones.breed(this);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		EntityReference.store(this.entityData.get(OWNER).orElse(null), output, "Owner");
		output.putInt("CloneLevel", this.cloneLevel());
		output.putBoolean("Breeder", this.breeder);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		EntityReference<LivingEntity> owner = EntityReference.read(input, "Owner");
		this.entityData.set(OWNER, Optional.ofNullable(owner));
		this.entityData.set(LEVEL, input.getIntOr("CloneLevel", 1));
		this.breeder = input.getBooleanOr("Breeder", false);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// Clones never fight each other (sweeping attacks of other clones), but the owner's own sweep still hurts them.
		if (source.getEntity() instanceof CombatClone other && this.isAlliedTo(other)) {
			return false;
		}

		return super.hurtServer(level, source, amount);
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PLAYER_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.PLAYER_DEATH;
	}

	@Override
	protected int getBaseExperienceReward(ServerLevel level) {
		return 0;
	}

	/** Attacks only when the clone fights (level 2+). */
	private static class FightGoal extends MeleeAttackGoal {
		private final CombatClone clone;

		FightGoal(CombatClone clone) {
			super(clone, 1.25, true);
			this.clone = clone;
		}

		@Override
		public boolean canUse() {
			return this.clone.fights() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return this.clone.fights() && super.canContinueToUse();
		}
	}

	/** Keeps close to the owner, teleports when left far behind. */
	private static class FollowOwnerGoal extends Goal {
		private final CombatClone clone;
		private @Nullable Player owner;
		private int recalc;

		FollowOwnerGoal(CombatClone clone) {
			this.clone = clone;
			this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			Player owner = this.clone.owner();

			if (owner == null || owner.isSpectator() || this.clone.getTarget() != null) {
				return false;
			}

			this.owner = owner;
			return this.clone.distanceToSqr(owner) > 64.0;
		}

		@Override
		public boolean canContinueToUse() {
			return this.owner != null && this.owner.isAlive() && this.clone.getTarget() == null
					&& this.clone.distanceToSqr(this.owner) > 9.0;
		}

		@Override
		public void stop() {
			this.owner = null;
			this.clone.getNavigation().stop();
		}

		@Override
		public void tick() {
			if (this.owner == null) {
				return;
			}

			this.clone.getLookControl().setLookAt(this.owner, 10.0F, this.clone.getMaxHeadXRot());

			if (--this.recalc > 0) {
				return;
			}

			this.recalc = this.adjustedTickDelay(10);

			if (this.clone.distanceToSqr(this.owner) > 32.0 * 32.0) {
				double angle = this.clone.getRandom().nextDouble() * Math.PI * 2.0;
				this.clone.teleportTo(this.owner.getX() + Math.cos(angle) * 2.0, this.owner.getY(), this.owner.getZ() + Math.sin(angle) * 2.0);
				this.clone.getNavigation().stop();
			} else {
				this.clone.getNavigation().moveTo(this.owner, 1.2);
			}
		}
	}

	/** Attacks whatever hurt the owner or whatever the owner hits (level 2+). */
	private static class DefendOwnerGoal extends TargetGoal {
		private final CombatClone clone;
		private @Nullable LivingEntity candidate;

		DefendOwnerGoal(CombatClone clone) {
			super(clone, false);
			this.clone = clone;
			this.setFlags(EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			Player owner = this.clone.owner();

			if (owner == null || !this.clone.fights()) {
				return false;
			}

			LivingEntity attacker = owner.getLastHurtByMob();

			if (attacker == null || attacker.tickCount - owner.getLastHurtByMobTimestamp() > 200) {
				attacker = owner.getLastHurtMob();
			}

			if (attacker == null || attacker instanceof Player || this.clone.isAlliedTo(attacker) || !attacker.isAlive()) {
				return false;
			}

			this.candidate = attacker;
			return this.clone.distanceToSqr(attacker) < 24.0 * 24.0;
		}

		@Override
		public void start() {
			this.mob.setTarget(this.candidate);
			super.start();
		}
	}
}
