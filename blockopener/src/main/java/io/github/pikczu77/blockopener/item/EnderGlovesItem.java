package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.ability.EnderGloves;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Ender Gloves (from end stone): right-click to teleport where you look (up to 64 blocks), right-click
 * a mob to swap places with it. Endermen ignore you while you hold them.
 */
public class EnderGlovesItem extends Item {
	private static final int COOLDOWN = 15;

	public EnderGlovesItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			if (!EnderGloves.teleport(serverPlayer)) {
				player.displayClientMessage(Component.translatable("blockopener.ender_gloves.no_target").withStyle(ChatFormatting.DARK_AQUA), true);
				return InteractionResult.FAIL;
			}
			player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN);
		}
		return InteractionResult.SUCCESS;
	}
}
