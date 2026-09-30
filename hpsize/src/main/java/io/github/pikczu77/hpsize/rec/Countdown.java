package io.github.pikczu77.hpsize.rec;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

import io.github.pikczu77.hpsize.util.Screens;

/**
 * On-screen "3... 2... 1... START!" countdown for every player.
 */
public final class Countdown {
	private static @Nullable Countdown active;

	private final int seconds;
	private final @Nullable Component message;
	private final @Nullable Runnable onFinish;
	private int ticks;

	private Countdown(int seconds, @Nullable Component message, @Nullable Runnable onFinish) {
		this.seconds = seconds;
		this.message = message;
		this.onFinish = onFinish;
	}

	public static void start(int seconds, @Nullable Component message, @Nullable Runnable onFinish) {
		active = new Countdown(seconds, message, onFinish);
	}

	public static boolean cancel(MinecraftServer server) {
		if (active == null) {
			return false;
		}

		active = null;
		Screens.title(server.getPlayerList().getPlayers(), Component.empty(), null, 0, 1, 0);
		return true;
	}

	public static boolean isRunning() {
		return active != null;
	}

	public static void reset() {
		active = null;
	}

	public static void tick(MinecraftServer server) {
		Countdown countdown = active;

		if (countdown == null) {
			return;
		}

		if (countdown.ticks % 20 == 0) {
			List<ServerPlayer> players = server.getPlayerList().getPlayers();
			int left = countdown.seconds - countdown.ticks / 20;
			Component subtitle = countdown.message == null ? null : countdown.message.copy().withStyle(ChatFormatting.GRAY);

			if (left > 0) {
				ChatFormatting color = switch (left) {
					case 3 -> ChatFormatting.RED;
					case 2 -> ChatFormatting.GOLD;
					case 1 -> ChatFormatting.YELLOW;
					default -> ChatFormatting.WHITE;
				};
				Screens.title(players, Component.literal(String.valueOf(left)).withStyle(color, ChatFormatting.BOLD), subtitle, 0, 22, 3);
				Screens.sound(players, Screens.sound(SoundEvents.NOTE_BLOCK_PLING), 1.0F, left <= 3 ? 1.0F : 0.8F);
			} else {
				Component finalSubtitle = countdown.message == null ? null : countdown.message.copy().withStyle(ChatFormatting.WHITE);
				Screens.title(players, Component.literal("START!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), finalSubtitle, 0, 30, 15);
				Screens.sound(players, Screens.sound(SoundEvents.NOTE_BLOCK_PLING), 1.0F, 2.0F);
				Screens.sound(players, Screens.sound(SoundEvents.PLAYER_LEVELUP), 0.7F, 1.2F);
				active = null;

				if (countdown.onFinish != null) {
					countdown.onFinish.run();
				}

				return;
			}
		}

		countdown.ticks++;
	}
}
