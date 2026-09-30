package io.github.pikczu77.blockopener.entity;

import io.github.pikczu77.blockopener.registry.ModDamageTypes;
import io.github.pikczu77.blockopener.registry.ModEntities;
import io.github.pikczu77.blockopener.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A glowing ball of moss. Whatever living thing it hits dies on the spot (the Ender Dragon included),
 * and where it lands the ground turns into moss.
 */
public class MossphereEntity extends ThrowableItemProjectile {
	private static final float KILL_DAMAGE = 10_000.0F;

	public MossphereEntity(EntityType<? extends MossphereEntity> type, Level level) {
		super(type, level);
	}

	public MossphereEntity(Level level, LivingEntity owner, ItemStack stack) {
		super(ModEntities.MOSSPHERE, owner, level, stack);
		this.setGlowingTag(true);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.MOSSPHERE;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide() && this.tickCount > 1) {
			this.level().addParticle(ParticleTypes.COMPOSTER, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		Entity target = hit.getEntity();
		if (!(this.level() instanceof ServerLevel level) || target == this.getOwner()) {
			return;
		}
		if (target instanceof LivingEntity || target instanceof EnderDragonPart) {
			target.hurtServer(level, ModDamageTypes.source(level, ModDamageTypes.MOSSPHERE, this, this.getOwner()), KILL_DAMAGE);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY(0.5), target.getZ(), 30, 0.5, 0.6, 0.5, 0.1);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.MOSS_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (this.level() instanceof ServerLevel level) {
			spreadMoss(level, hit.getBlockPos(), this.random);
		}
	}

	private void spreadMoss(ServerLevel level, BlockPos center, RandomSource random) {
		Player player = this.getOwner() instanceof Player owner ? owner : null;
		int radius = 2;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
			if (pos.distSqr(center) > radius * radius + 1 || player != null && !level.mayInteract(player, pos)) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (!state.is(BlockTags.MOSS_REPLACEABLE) || !level.getBlockState(pos.above()).isAir()) {
				continue;
			}
			level.setBlockAndUpdate(pos, Blocks.MOSS_BLOCK.defaultBlockState());
			float roll = random.nextFloat();
			if (roll < 0.35F) {
				level.setBlockAndUpdate(pos.above(), Blocks.MOSS_CARPET.defaultBlockState());
			} else if (roll < 0.42F) {
				level.setBlockAndUpdate(pos.above(), Blocks.AZALEA.defaultBlockState());
			} else if (roll < 0.47F) {
				level.setBlockAndUpdate(pos.above(), Blocks.FLOWERING_AZALEA.defaultBlockState());
			}
		}
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5, 25, 1.5, 0.5, 1.5, 0.0);
		level.playSound(null, center, SoundEvents.MOSS_PLACE, SoundSource.BLOCKS, 1.2F, 0.8F);
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (!this.level().isClientSide()) {
			this.level().broadcastEntityEvent(this, (byte) 3);
			this.discard();
		}
	}

	@Override
	public void handleEntityEvent(byte event) {
		if (event == 3) {
			for (int i = 0; i < 12; i++) {
				this.level().addParticle(ParticleTypes.COMPOSTER, this.getX(), this.getY(), this.getZ(),
					(this.random.nextDouble() - 0.5) * 0.3, this.random.nextDouble() * 0.3, (this.random.nextDouble() - 0.5) * 0.3);
			}
		}
	}
}
