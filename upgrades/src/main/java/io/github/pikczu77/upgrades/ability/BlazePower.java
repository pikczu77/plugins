package io.github.pikczu77.upgrades.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeAccess;

/**
 * Right-click with a blaze rod shoots a volley of blaze fireballs. The rod is not used up.
 */
public final class BlazePower {
	private static final int VOLLEY = 3;

	private BlazePower() {
	}

	public static InteractionResult useItem(Player player, Level level, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (!stack.is(Items.BLAZE_ROD) || player.isSpectator() || !UpgradeAccess.has(player, Upgrade.BLAZE_POWER)) {
			return InteractionResult.PASS;
		}

		if (player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}

		if (player instanceof ServerPlayer serverPlayer) {
			Vec3 look = serverPlayer.getLookAngle();

			for (int i = 0; i < VOLLEY; i++) {
				Vec3 direction = look.add(serverPlayer.getRandom().triangle(0.0, 0.12), serverPlayer.getRandom().triangle(0.0, 0.08),
						serverPlayer.getRandom().triangle(0.0, 0.12)).normalize();
				SmallFireball fireball = new SmallFireball(level, serverPlayer, direction.scale(1.5));
				fireball.setPos(serverPlayer.getX() + look.x, serverPlayer.getEyeY() - 0.2 + look.y, serverPlayer.getZ() + look.z);
				level.addFreshEntity(fireball);
			}

			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.getCooldowns().addCooldown(stack, 8);
		}

		player.swing(hand, true);
		return InteractionResult.SUCCESS;
	}
}
