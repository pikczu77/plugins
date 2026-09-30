package io.github.pikczu77.hpsize.rec;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;

import io.github.pikczu77.hpsize.util.Durations;
import io.github.pikczu77.hpsize.util.Screens;

/**
 * Stopwatch / countdown timer shown to every player on a boss bar or the action bar.
 * It counts game ticks, so it pauses together with the game (Esc in singleplayer, /tick freeze).
 */
public final class RecTimer {
	public enum Display {
		BOSSBAR, ACTIONBAR
	}

	private static boolean visible;
	private static boolean running;
	private static boolean countdown;
	private static long ticks;
	private static long total;
	private static Display display = Display.BOSSBAR;
	private static String label = "";
	private static @Nullable ServerBossEvent bar;
	private static long lastShown = -1;

	private RecTimer() {
	}

	public static void startStopwatch() {
		countdown = false;
		ticks = 0;
		total = 0;
		running = true;
		visible = true;
		lastShown = -1;
	}

	public static void startCountdown(long durationTicks) {
		countdown = true;
		ticks = durationTicks;
		total = durationTicks;
		running = true;
		visible = true;
		lastShown = -1;
	}

	public static boolean pause() {
		if (!visible || !running) {
			return false;
		}

		running = false;
		lastShown = -1;
		return true;
	}

	public static boolean resume() {
		if (!visible || running || (countdown && ticks <= 0)) {
			return false;
		}

		running = true;
		lastShown = -1;
		return true;
	}

	public static void stop() {
		visible = false;
		running = false;
		hideBar();
	}

	public static void add(long deltaTicks) {
		ticks = Math.max(0, ticks + deltaTicks);

		if (countdown) {
			total = Math.max(total, ticks);
		}

		lastShown = -1;
	}

	public static void set(long valueTicks) {
		ticks = Math.max(0, valueTicks);

		if (countdown) {
			total = Math.max(1, ticks);
		}

		lastShown = -1;
	}

	public static void setDisplay(Display newDisplay) {
		display = newDisplay;
		hideBar();
		lastShown = -1;
	}

	public static void setLabel(String newLabel) {
		label = newLabel;
		lastShown = -1;
	}

	public static boolean isVisible() {
		return visible;
	}

	public static String describe() {
		if (!visible) {
			return "Timer wyłączony.";
		}

		return (countdown ? "Odliczanie: " : "Stoper: ") + Durations.format(ticks, countdown)
				+ (running ? "" : " (pauza)") + (label.isEmpty() ? "" : " | etykieta: " + label)
				+ " | wyświetlanie: " + (display == Display.BOSSBAR ? "pasek bossa" : "pasek akcji");
	}

	public static void reset() {
		stop();
		display = Display.BOSSBAR;
		label = "";
		ticks = 0;
		total = 0;
	}

	public static void removePlayer(ServerPlayer player) {
		if (bar != null) {
			bar.removePlayer(player);
		}
	}

	public static void tick(MinecraftServer server) {
		if (!visible) {
			return;
		}

		if (running && server.tickRateManager().runsNormally()) {
			if (countdown) {
				ticks--;

				if (ticks <= 0) {
					ticks = 0;
					running = false;
					finished(server);
				}
			} else {
				ticks++;
			}
		}

		long shown = countdown ? (ticks + 19) / 20 : ticks / 20;
		// Refresh once a second anyway: keeps the action bar visible and adds players who just joined.
		if (shown != lastShown || server.getTickCount() % 20 == 0) {
			lastShown = shown;
			render(server);
		}
	}

	private static void render(MinecraftServer server) {
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		MutableComponent text = Component.empty();

		if (!label.isEmpty()) {
			text.append(Component.literal(label + " ").withStyle(ChatFormatting.WHITE));
		}

		ChatFormatting color = !running ? ChatFormatting.GRAY
				: countdown && ticks <= 200 ? ChatFormatting.RED
				: countdown && ticks * 4 <= total ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
		text.append(Component.literal("⌚ " + Durations.format(ticks, countdown)).withStyle(color, ChatFormatting.BOLD));

		if (!running) {
			text.append(Component.literal(" (pauza)").withStyle(ChatFormatting.GRAY));
		}

		if (display == Display.ACTIONBAR) {
			hideBar();

			for (ServerPlayer player : players) {
				Screens.actionBar(player, text);
			}

			return;
		}

		if (bar == null) {
			bar = new ServerBossEvent(text, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
		}

		bar.setName(text);

		if (countdown) {
			float progress = total <= 0 ? 0.0F : (float) ticks / total;
			bar.setProgress(Math.max(0.0F, Math.min(1.0F, progress)));
			bar.setColor(progress > 0.5F ? BossEvent.BossBarColor.GREEN : progress > 0.2F ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED);
		} else {
			bar.setProgress((ticks % 1200) / 1200.0F);
			bar.setColor(BossEvent.BossBarColor.YELLOW);
		}

		for (ServerPlayer player : new ArrayList<>(bar.getPlayers())) {
			if (!players.contains(player)) {
				bar.removePlayer(player);
			}
		}

		for (ServerPlayer player : players) {
			if (!bar.getPlayers().contains(player)) {
				bar.addPlayer(player);
			}
		}
	}

	private static void finished(MinecraftServer server) {
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		Screens.title(players, Component.literal("KONIEC CZASU!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				label.isEmpty() ? null : Component.literal(label), 5, 50, 20);
		Screens.sound(players, Screens.sound(SoundEvents.RAID_HORN), 1.0F, 1.0F);
	}

	private static void hideBar() {
		if (bar != null) {
			bar.removeAllPlayers();
			bar = null;
		}
	}
}
