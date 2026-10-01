package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.ability.SizeShift;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Size Mushroom (from mushroom blocks): right-click to become a giant, sneak + right-click to shrink.
 * Use the same way again to go back to normal size.
 */
public class SizeMushroomItem extends Item {
	private static final int COOLDOWN = 20;

	public SizeMushroomItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			SizeShift.Size wanted = player.isShiftKeyDown() ? SizeShift.Size.TINY : SizeShift.Size.GIANT;
			SizeShift.Size next = SizeShift.current(player) == wanted ? SizeShift.Size.NORMAL : wanted;
			if (!SizeShift.set(serverPlayer, next)) {
				player.displayClientMessage(Component.translatable("blockopener.size_mushroom.no_room").withStyle(ChatFormatting.RED), true);
				return InteractionResult.FAIL;
			}
			player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN);
		}
		return InteractionResult.SUCCESS;
	}
}
