package io.github.pikczu77.blockopener.item;

import io.github.pikczu77.blockopener.ability.Bombs;
import io.github.pikczu77.blockopener.ability.ModAbilities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Piston Launcher (from a piston).
 * <ul>
 *   <li>Sneak + right-click: Turbo Launch, spammable, no fall damage on landing.</li>
 *   <li>Right-click a mob: Mega Kick (see {@code MegaKick}).</li>
 *   <li>Right-click the air: TNT Cannon, fires TNT from your inventory (free in creative).</li>
 * </ul>
 */
public class PistonLauncherItem extends Item {
	private static final int LAUNCH_COOLDOWN = 5;
	private static final int CANNON_COOLDOWN = 12;

	public PistonLauncherItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		ServerLevel serverLevel = serverPlayer.level();
		Vec3 look = player.getViewVector(1.0F);
		if (player.isShiftKeyDown()) {
			Vec3 boost = look.scale(2.6).add(0.0, 0.35, 0.0);
			player.setDeltaMovement(boost);
			player.hurtMarked = true;
			ModAbilities.markLaunched(serverPlayer);
			player.getCooldowns().addCooldown(stack, LAUNCH_COOLDOWN);
			serverLevel.playSound(null, player.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0F, 1.3F);
			serverLevel.playSound(null, player.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.7F, 1.2F);
			serverLevel.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, player.getX(), player.getY(), player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			return InteractionResult.SUCCESS;
		}

		if (!player.getAbilities().instabuild && !consumeTnt(player)) {
			player.displayClientMessage(Component.translatable("blockopener.piston_launcher.no_tnt").withStyle(ChatFormatting.RED), true);
			serverLevel.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.2F);
			player.getCooldowns().addCooldown(stack, 10);
			return InteractionResult.FAIL;
		}
		Vec3 start = player.getEyePosition().add(look.scale(1.2)).subtract(0.0, 0.3, 0.0);
		Bombs.spawn(serverLevel, player, start, look.scale(1.9), Blocks.TNT.defaultBlockState(), 50, 4.0F);
		player.getCooldowns().addCooldown(stack, CANNON_COOLDOWN);
		serverLevel.playSound(null, player.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0F, 0.8F);
		serverLevel.playSound(null, player.blockPosition(), SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 1.0F, 1.0F);
		return InteractionResult.SUCCESS;
	}

	private static boolean consumeTnt(Player player) {
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack candidate = inventory.getItem(slot);
			if (candidate.is(Items.TNT)) {
				candidate.shrink(1);
				return true;
			}
		}
		return false;
	}
}
