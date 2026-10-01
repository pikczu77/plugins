package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.ability.MagmaFist;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Magma Fist (from a magma block): every hit sets the target on fire, right-click is a fire nova around
 * you. While you hold it you are fireproof and can walk on lava.
 */
public class MagmaFistItem extends Item {
	private static final int NOVA_COOLDOWN = 60;

	public MagmaFistItem(Properties properties) {
		super(properties);
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		MagmaFist.ignite(target);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			MagmaFist.nova(serverPlayer);
			player.getCooldowns().addCooldown(player.getItemInHand(hand), NOVA_COOLDOWN);
		}
		return InteractionResult.SUCCESS;
	}
}
