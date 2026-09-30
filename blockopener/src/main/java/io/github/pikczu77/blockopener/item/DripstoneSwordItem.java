package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.ability.Aim;
import io.github.pikczu77.blockopener.ability.DripstoneRain;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Dripstone Sword (from a dripstone block): 9 attack damage. Sneak + attack calls Dripstone Rain on the
 * target; right-click calls it wherever you are looking.
 */
public class DripstoneSwordItem extends Item {
	private static final int COOLDOWN = 40;

	public DripstoneSwordItem(Properties properties) {
		super(properties);
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		if (attacker instanceof ServerPlayer player && player.isShiftKeyDown() && !player.getCooldowns().isOnCooldown(stack)) {
			player.getCooldowns().addCooldown(stack, COOLDOWN);
			DripstoneRain.start(player, target.position());
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN);
			DripstoneRain.start(serverPlayer, Aim.point(player, 48.0));
		}
		return InteractionResult.SUCCESS;
	}
}
