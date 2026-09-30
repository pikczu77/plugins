package io.github.pikczu77.upgrades.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.upgrades.config.UpgradesConfig;

/**
 * Obsidian thrown with the obsidian horn: blows up on impact and fills the spot with obsidian.
 */
public class ThrownObsidian extends ThrowableItemProjectile {
	private static final int OBSIDIAN_RADIUS = 2;

	public ThrownObsidian(EntityType<? extends ThrownObsidian> type, Level level) {
		super(type, level);
	}

	public ThrownObsidian(Level level, LivingEntity owner) {
		super(ModEntities.THROWN_OBSIDIAN, owner, level, new ItemStack(Items.OBSIDIAN));
	}

	@Override
	protected Item getDefaultItem() {
		return Items.OBSIDIAN;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.04;
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		hit.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 8.0F);
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);

		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}

		Vec3 center = hit.getLocation();
		level.explode(this, center.x, center.y, center.z, UpgradesConfig.get().obsidianPower, Level.ExplosionInteraction.TNT);
		fillWithObsidian(level, BlockPos.containing(center));
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 60, 1.5, 1.5, 1.5, 0.1);
		this.discard();
	}

	private static void fillWithObsidian(ServerLevel level, BlockPos center) {
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-OBSIDIAN_RADIUS, -OBSIDIAN_RADIUS, -OBSIDIAN_RADIUS),
				center.offset(OBSIDIAN_RADIUS, OBSIDIAN_RADIUS, OBSIDIAN_RADIUS))) {
			if (pos.distSqr(center) > OBSIDIAN_RADIUS * OBSIDIAN_RADIUS + 1 || level.random.nextFloat() < 0.25F) {
				continue;
			}

			BlockState state = level.getBlockState(pos);

			if (state.canBeReplaced() || state.isAir()) {
				level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
			}
		}
	}
}
