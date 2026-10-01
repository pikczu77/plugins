package io.github.pikczu77.blockopener.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Cake of Life (from a cake): heals and feeds you, and is never used up. 8 second cooldown. */
public class CakeOfLifeItem extends Item {
	public CakeOfLifeItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		ItemStack kept = stack.copy();
		super.finishUsingItem(stack, level, entity);
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + 1.2, entity.getZ(), 6, 0.5, 0.4, 0.5, 0.0);
		}
		return kept;
	}
}
