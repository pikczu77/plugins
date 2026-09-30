package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.ability.Aim;
import io.github.pikczu77.blockopener.ability.BeeSwarm;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Bee Drill (from a beehive). Right-click summons bees; right-click while aiming at a mob sends the
 * whole swarm after it ("Bees, attack!"). Aiming works from far away, not only point blank.
 */
public class BeeDrillItem extends Item {
	private static final double AIM_RANGE = 40.0;

	public BeeDrillItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			Entity aimed = Aim.entity(player, AIM_RANGE, entity -> entity instanceof LivingEntity && entity != player && !BeeSwarm.isOwnBee(player, entity));
			if (aimed instanceof LivingEntity target) {
				BeeSwarm.attack(serverPlayer, target);
				player.displayClientMessage(Component.translatable("blockopener.bee_drill.attack").withStyle(ChatFormatting.GOLD), true);
				player.getCooldowns().addCooldown(player.getItemInHand(hand), 5);
			} else {
				int summoned = BeeSwarm.summon(serverPlayer);
				player.displayClientMessage(summoned > 0
					? Component.translatable("blockopener.bee_drill.summon", BeeSwarm.size(serverPlayer), BeeSwarm.MAX_BEES).withStyle(ChatFormatting.YELLOW)
					: Component.translatable("blockopener.bee_drill.full").withStyle(ChatFormatting.GRAY), true);
				player.getCooldowns().addCooldown(player.getItemInHand(hand), 20);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
