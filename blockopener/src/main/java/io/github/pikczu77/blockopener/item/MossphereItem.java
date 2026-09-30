package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.entity.MossphereEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Mossphere (from a moss block): throw it, and whatever it hits dies. Yes, the dragon too. */
public class MossphereItem extends Item {
	public MossphereItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.6F, 0.5F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.MOSS_PLACE, SoundSource.NEUTRAL, 0.6F, 1.2F);
		if (level instanceof ServerLevel serverLevel) {
			Projectile.spawnProjectileFromRotation(MossphereEntity::new, serverLevel, stack, player, 0.0F, 1.8F, 0.5F);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		player.getCooldowns().addCooldown(stack, 4);
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
