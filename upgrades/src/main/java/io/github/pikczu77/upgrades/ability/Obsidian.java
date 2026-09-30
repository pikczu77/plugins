package io.github.pikczu77.upgrades.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import io.github.pikczu77.upgrades.entity.ThrownObsidian;
import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeAccess;

/**
 * The obsidian horn: right-clicking with obsidian throws it (sneak to place it normally, the video showed that
 * obsidian could not be placed anymore).
 */
public final class Obsidian {
	private Obsidian() {
	}

	public static InteractionResult useItem(Player player, Level level, InteractionHand hand) {
		return tryThrow(player, level, hand);
	}

	public static InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		return tryThrow(player, level, hand);
	}

	private static InteractionResult tryThrow(Player player, Level level, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (!stack.is(Items.OBSIDIAN) || player.isShiftKeyDown() || player.isSpectator() || !UpgradeAccess.has(player, Upgrade.OBSIDIAN_HORN)) {
			return InteractionResult.PASS;
		}

		if (player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}

		if (player instanceof ServerPlayer serverPlayer) {
			ThrownObsidian thrown = new ThrownObsidian(level, serverPlayer);
			thrown.shootFromRotation(serverPlayer, serverPlayer.getXRot(), serverPlayer.getYRot(), 0.0F, 1.8F, 0.5F);
			level.addFreshEntity(thrown);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.6F, 1.6F);
			stack.consume(1, player);
			player.getCooldowns().addCooldown(stack, 10);
		}

		player.swing(hand, true);
		return InteractionResult.SUCCESS;
	}
}
