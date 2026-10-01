package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.entity.HoneyBlobEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Honey Blaster (from a honey block): shoots sticky honey that glues mobs in place and leaves honey
 * blocks on walls. Sneak + right-click fires a spread of five.
 */
public class HoneyBlasterItem extends Item {
	private static final int COOLDOWN = 6;
	private static final int SPREAD_COOLDOWN = 40;

	public HoneyBlasterItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean spread = player.isShiftKeyDown();
		if (level instanceof ServerLevel serverLevel) {
			ItemStack ammo = new ItemStack(Items.HONEY_BLOCK);
			if (spread) {
				for (int i = -2; i <= 2; i++) {
					float yaw = player.getYRot() + i * 9.0F;
					Projectile.spawnProjectile(new HoneyBlobEntity(serverLevel, player, ammo), serverLevel, ammo,
						blob -> blob.shootFromRotation(player, player.getXRot(), yaw, 0.0F, 1.6F, 2.0F));
				}
			} else {
				Projectile.spawnProjectileFromRotation(HoneyBlobEntity::new, serverLevel, ammo, player, 0.0F, 1.8F, 0.5F);
			}
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 1.0F, 1.4F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 0.6F);
		player.getCooldowns().addCooldown(stack, spread ? SPREAD_COOLDOWN : COOLDOWN);
		return InteractionResult.SUCCESS;
	}
}
