package io.github.pikczu77.upgrades.entity;

import java.util.Comparator;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Fireball of the dragon wings: steers towards the nearest end crystal, dragon or monster in front of it and
 * explodes without breaking blocks.
 */
public class HomingFireball extends Fireball {
	private static final double SPEED = 1.1;
	private static final double RANGE = 48.0;
	private @Nullable Entity target;
	private int life;

	public HomingFireball(EntityType<? extends HomingFireball> type, Level level) {
		super(type, level);
	}

	public HomingFireball(Level level, LivingEntity owner, Vec3 direction) {
		super(ModEntities.HOMING_FIREBALL, owner, direction.normalize().scale(SPEED), level);
		this.accelerationPower = 0.0;
	}

	@Override
	public void tick() {
		super.tick();

		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}

		if (++this.life > 200) {
			this.discard();
			return;
		}

		if (this.target == null || !this.target.isAlive() || this.life % 10 == 0) {
			this.target = this.findTarget(level);
		}

		if (this.target != null) {
			Vec3 aim = this.target.getBoundingBox().getCenter().subtract(this.position()).normalize().scale(SPEED);
			Vec3 motion = this.getDeltaMovement().lerp(aim, 0.25);
			this.setDeltaMovement(motion.normalize().scale(SPEED));
			this.needsSync = true;
		}
	}

	private @Nullable Entity findTarget(ServerLevel level) {
		Vec3 heading = this.getDeltaMovement().normalize();
		AABB area = this.getBoundingBox().inflate(RANGE);
		Entity owner = this.getOwner();
		Comparator<Entity> nearest = Comparator.comparingDouble(this::distanceToSqr);

		Entity crystal = level.getEntitiesOfClass(EndCrystal.class, area, candidate -> this.ahead(candidate, heading)).stream()
				.min(nearest).orElse(null);

		if (crystal != null) {
			return crystal;
		}

		return level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity.isAlive() && entity != owner
						&& !(entity instanceof CombatClone) && (entity instanceof Enemy || entity instanceof EnderDragon) && this.ahead(entity, heading))
				.stream().min(nearest).orElse(null);
	}

	/** Only targets roughly in front, so the fireballs go where you aim. */
	private boolean ahead(Entity entity, Vec3 heading) {
		Vec3 to = entity.getBoundingBox().getCenter().subtract(this.position());
		return to.lengthSqr() < RANGE * RANGE && to.normalize().dot(heading) > 0.5;
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);

		if (this.level() instanceof ServerLevel level) {
			hit.getEntity().hurtServer(level, this.damageSources().fireball(this, this.getOwner()), 10.0F);
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);

		if (!this.level().isClientSide()) {
			this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.5F, false, Level.ExplosionInteraction.NONE);
			this.discard();
		}
	}

	@Override
	protected boolean shouldBurn() {
		return false;
	}
}
