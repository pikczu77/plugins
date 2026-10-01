package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.ability.ObsidianShield;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Obsidian Shield (from obsidian): blocks attacks from every side, never breaks, and sneak + right-click
 * raises an obsidian dome around you for 10 seconds.
 */
public class ObsidianShieldItem extends Item {
	public ObsidianShieldItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.isShiftKeyDown()) {
			if (player instanceof ServerPlayer serverPlayer) {
				ObsidianShield.dome(serverPlayer);
			}
			return InteractionResult.SUCCESS;
		}
		return super.use(level, player, hand);
	}
}
