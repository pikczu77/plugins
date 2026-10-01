package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.ability.IceWand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Ice Wand (from blue or packed ice): right-click freezes the mob you look at inside a block of ice for
 * 5 seconds. While you hold it, water freezes under your feet.
 */
public class IceWandItem extends Item {
	private static final int COOLDOWN = 30;

	public IceWandItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			boolean hit = IceWand.freeze(serverPlayer);
			player.getCooldowns().addCooldown(player.getItemInHand(hand), hit ? COOLDOWN : 8);
		}
		return InteractionResult.SUCCESS;
	}
}
