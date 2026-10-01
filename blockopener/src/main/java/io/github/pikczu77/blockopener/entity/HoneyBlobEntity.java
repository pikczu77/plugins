package io.github.pikczu77.blockopener.entity;

import io.github.pikczu77.blockopener.ability.TempBlocks;
import io.github.pikczu77.blockopener.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A blob of honey from the Honey Blaster: glues mobs in place and leaves a sticky honey block where it lands. */
public class HoneyBlobEntity extends ThrowableItemProjectile {
	private static final int STUCK_TICKS = 80;

	public HoneyBlobEntity(EntityType<? extends HoneyBlobEntity> type, Level level) {
		super(type, level);
	}

	public HoneyBlobEntity(Level level, LivingEntity owner, ItemStack stack) {
		super(ModEntities.HONEY_BLOB, owner, level, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.HONEY_BLOCK;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide() && this.tickCount > 1 && this.random.nextInt(2) == 0) {
			this.level().addParticle(ParticleTypes.FALLING_HONEY, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		Entity target = hit.getEntity();
		if (!(this.level() instanceof ServerLevel level) || target == this.getOwner() || !(target instanceof LivingEntity living)) {
			return;
		}
		living.hurtServer(level, this.damageSources().thrown(this, this.getOwner()), 2.0F);
		living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, STUCK_TICKS, 6), this.getEffectSource());
		living.setDeltaMovement(living.getDeltaMovement().multiply(0.0, 1.0, 0.0));
		living.hurtMarked = true;
		level.sendParticles(ParticleTypes.LANDING_HONEY, living.getX(), living.getY(0.6), living.getZ(), 25, 0.4, 0.5, 0.4, 0.0);
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (this.level() instanceof ServerLevel level) {
			BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
			boolean allowed = !(this.getOwner() instanceof Player player) || level.mayInteract(player, pos);
			if (allowed) {
				TempBlocks.place(level, pos, Blocks.HONEY_BLOCK.defaultBlockState(), 200);
			}
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (this.level() instanceof ServerLevel level) {
			level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.HONEY_BLOCK_PLACE, SoundSource.PLAYERS, 1.2F, 0.8F);
			level.sendParticles(ParticleTypes.LANDING_HONEY, this.getX(), this.getY(), this.getZ(), 12, 0.3, 0.3, 0.3, 0.0);
			this.discard();
		}
	}
}
