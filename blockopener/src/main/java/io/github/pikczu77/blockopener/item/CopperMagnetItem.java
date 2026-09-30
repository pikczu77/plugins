package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.ability.CopperMagnet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Copper Magnet (from a copper block). Held: pulls items. Sneak: Turbo Pull. Right-click: Area De-weaponizer.
 * The passive parts run in {@code CopperMagnet#tick}.
 */
public class CopperMagnetItem extends Item {
	private static final int COOLDOWN = 60;

	public CopperMagnetItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			int disarmed = CopperMagnet.deweaponize(serverPlayer);
			player.displayClientMessage(Component.translatable("blockopener.copper_magnet.disarmed", disarmed).withStyle(ChatFormatting.GOLD), true);
			player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN);
		}
		return InteractionResult.SUCCESS;
	}
}
