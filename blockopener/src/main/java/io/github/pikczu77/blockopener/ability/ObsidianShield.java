package io.github.pikczu77.blockopener.ability;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Obsidian Shield's emergency dome: a hollow obsidian sphere around you for 10 seconds. */
public final class ObsidianShield {
	private static final int DOME_TICKS = 200;
	private static final int COOLDOWN = 20 * 30;

	public static boolean dome(ServerPlayer player) {
		ServerLevel level = player.level();
		PlayerState state = ModAbilities.state(player);
		long now = level.getGameTime();
		if (now < state.domeReadyAt) {
			player.displayClientMessage(Component.translatable("blockopener.obsidian_shield.cooldown", (state.domeReadyAt - now) / 20 + 1)
				.withStyle(ChatFormatting.DARK_PURPLE), true);
			return false;
		}
		state.domeReadyAt = now + COOLDOWN;
		Vec3 center = player.position().add(0.0, 1.0, 0.0);
		BlockPos origin = BlockPos.containing(center);
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -4, -4), origin.offset(4, 4, 4))) {
			double distance = Vec3.atCenterOf(pos).distanceTo(center);
			if (distance >= 2.6 && distance < 3.6 && level.mayInteract(player, pos)) {
				TempBlocks.place(level, pos, Blocks.OBSIDIAN.defaultBlockState(), DOME_TICKS + level.random.nextInt(10));
			}
		}
		level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0F, 0.7F);
		return true;
	}

	private ObsidianShield() {
	}
}
