package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.ability.Aim;
import io.github.pikczu77.blockopener.ability.StormHammer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Storm Hammer (from a lightning rod): right-click calls lightning where you look; a jump attack
 * (hitting while falling) calls a ring of lightning around the target. Your own storm never hurts you.
 */
public class StormHammerItem extends Item {
	private static final int STRIKE_COOLDOWN = 20;
	private static final int RING_COOLDOWN = 40;

	public StormHammerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			StormHammer.strike(serverPlayer, Aim.point(player, 64.0));
			player.getCooldowns().addCooldown(player.getItemInHand(hand), STRIKE_COOLDOWN);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		if (attacker instanceof ServerPlayer player && !player.onGround() && player.fallDistance > 1.0
			&& !player.getCooldowns().isOnCooldown(stack)) {
			player.getCooldowns().addCooldown(stack, RING_COOLDOWN);
			StormHammer.ring(player, target.position());
		}
	}
}
