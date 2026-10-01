package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.ability.SlimeGloves;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Slime Gloves (from a slime block): right-click for a super jump, sneak + right-click for an emergency
 * slime platform. While holding them you bounce instead of taking fall damage (sneak to land softly).
 */
public class SlimeGlovesItem extends Item {
	private static final int JUMP_COOLDOWN = 15;
	private static final int PLATFORM_COOLDOWN = 60;

	public SlimeGlovesItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		boolean platform = player.isShiftKeyDown();
		if (!platform && !player.onGround() && !player.isInWater() && !player.onClimbable()) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			if (platform) {
				SlimeGloves.platform(serverPlayer);
			} else {
				SlimeGloves.superJump(serverPlayer);
			}
			player.getCooldowns().addCooldown(player.getItemInHand(hand), platform ? PLATFORM_COOLDOWN : JUMP_COOLDOWN);
		}
		return InteractionResult.SUCCESS;
	}
}
