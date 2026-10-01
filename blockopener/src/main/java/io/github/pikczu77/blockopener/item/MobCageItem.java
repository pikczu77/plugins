package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.ability.MobCage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Mob Cage (from a spawner): right-click a mob to catch it, right-click a block (or the air) to release it
 * as your ally. Catching is handled in {@code ModAbilities} through the entity use event.
 */
public class MobCageItem extends Item {
	public MobCageItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		if (!MobCage.isFull(stack)) {
			return InteractionResult.PASS;
		}
		if (context.getPlayer() instanceof ServerPlayer player) {
			Vec3 at = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
			if (!MobCage.release(player, stack, at)) {
				return InteractionResult.FAIL;
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!MobCage.isFull(stack)) {
			if (!level.isClientSide()) {
				player.displayClientMessage(Component.translatable("blockopener.mob_cage.empty").withStyle(ChatFormatting.GRAY), true);
			}
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			Vec3 look = player.getViewVector(1.0F);
			Vec3 at = player.position().add(look.x * 2.5, 0.0, look.z * 2.5);
			if (!MobCage.release(serverPlayer, stack, at)) {
				return InteractionResult.FAIL;
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return MobCage.isFull(stack) || super.isFoil(stack);
	}
}
