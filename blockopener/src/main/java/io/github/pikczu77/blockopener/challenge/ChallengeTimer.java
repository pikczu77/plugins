package io.github.pikczu77.blockopener.challenge;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import org.jspecify.annotations.Nullable;

/**
 * Countdown boss bar for timed challenges ("20 minutes to open as many blocks as you can, then fight").
 * Warns at 5 min / 1 min, counts the last 10 seconds out loud and ends with a big title.
 */
public final class ChallengeTimer {
	private static @Nullable ServerBossEvent bar;
	private static int total;
	private static int remaining;
	private static boolean paused;
	private static int lingerTicks;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ChallengeTimer::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (bar != null) {
				bar.addPlayer(handler.getPlayer());
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> stop());
	}

	public static void start(MinecraftServer server, int seconds) {
		stop();
		total = seconds * 20;
		remaining = total;
		paused = false;
		lingerTicks = 0;
		bar = new ServerBossEvent(title(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			bar.addPlayer(player);
		}
		broadcastTitle(server, Component.translatable("blockopener.timer.go").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
			Component.translatable("blockopener.timer.go_subtitle", format(total)).withStyle(ChatFormatting.WHITE));
		playToAll(server, SoundEvents.RAID_HORN.value(), 0.6F, 1.0F);
	}

	public static boolean isRunning() {
		return bar != null && lingerTicks == 0;
	}

	public static boolean togglePause() {
		paused = !paused;
		if (bar != null) {
			bar.setName(title());
		}
		return paused;
	}

	public static void addSeconds(int seconds) {
		remaining = Math.max(20, remaining + seconds * 20);
		total = Math.max(total, remaining);
	}

	public static void stop() {
		if (bar != null) {
			bar.removeAllPlayers();
			bar = null;
		}
	}

	public static int remainingSeconds() {
		return remaining / 20;
	}

	private static void tick(MinecraftServer server) {
		if (bar == null) {
			return;
		}
		if (lingerTicks > 0) {
			if (--lingerTicks == 0) {
				stop();
			}
			return;
		}
		if (paused) {
			return;
		}
		remaining--;
		float progress = total == 0 ? 0.0F : (float) remaining / total;
		bar.setProgress(Math.max(0.0F, progress));
		if (remaining % 20 == 0) {
			bar.setName(title());
			bar.setColor(progress > 0.5F ? BossEvent.BossBarColor.GREEN : progress > 0.2F ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED);
			int seconds = remaining / 20;
			if (seconds == 300 || seconds == 60) {
				server.getPlayerList().broadcastSystemMessage(
					Component.translatable("blockopener.timer.left", format(remaining)).withStyle(ChatFormatting.YELLOW), false);
				playToAll(server, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, 1.0F);
			} else if (seconds > 0 && seconds <= 10) {
				broadcastTitle(server, Component.literal(String.valueOf(seconds)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), Component.empty());
				playToAll(server, SoundEvents.NOTE_BLOCK_HAT.value(), 1.0F, seconds <= 3 ? 1.6F : 1.0F);
			}
		}
		if (remaining <= 0) {
			bar.setName(Component.translatable("blockopener.timer.over").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
			bar.setProgress(0.0F);
			broadcastTitle(server, Component.translatable("blockopener.timer.over").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				Component.translatable("blockopener.timer.over_subtitle").withStyle(ChatFormatting.GOLD));
			playToAll(server, SoundEvents.ENDER_DRAGON_GROWL, 0.6F, 1.0F);
			lingerTicks = 100;
		}
	}

	private static Component title() {
		Component time = Component.literal(format(remaining)).withStyle(ChatFormatting.BOLD);
		return paused
			? Component.translatable("blockopener.timer.paused", time).withStyle(ChatFormatting.GRAY)
			: Component.translatable("blockopener.timer.bar", time).withStyle(ChatFormatting.WHITE);
	}

	private static String format(int ticks) {
		int seconds = Math.max(0, ticks / 20);
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}

	private static void broadcastTitle(MinecraftServer server, Component title, Component subtitle) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 30, 10));
			player.connection.send(new ClientboundSetTitleTextPacket(title));
			player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		}
	}

	/** Plays a sound for each player only (so it is not heard once per nearby player). */
	public static void playToAll(MinecraftServer server, SoundEvent sound, float volume, float pitch) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.MASTER,
				player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
		}
	}

	private ChallengeTimer() {
	}
}
