package io.github.pikczu77.hpsize.util;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import io.github.pikczu77.hpsize.rec.RecMode;

/**
 * Chat helpers. Every text is written in English and Polish: players whose game language is Polish
 * get Polish, everyone else (and the server console) gets English. This works without the mod on the client.
 */
public final class Msg {
	private Msg() {
	}

	public static MutableComponent prefix() {
		return Component.literal("[HP Size] ").withStyle(ChatFormatting.GOLD);
	}

	public static MutableComponent info(String text) {
		return prefix().append(Component.literal(text).withStyle(ChatFormatting.GRAY));
	}

	/**
	 * Whether this entity is a player who plays Minecraft in Polish.
	 */
	public static boolean polish(Entity entity) {
		return entity instanceof ServerPlayer player && isPolish(player.clientInformation().language());
	}

	/**
	 * Whether a Minecraft language code (e.g. {@code pl_pl}) is Polish.
	 */
	public static boolean isPolish(String language) {
		return language != null && language.toLowerCase(Locale.ROOT).startsWith("pl");
	}

	public static boolean polish(CommandSourceStack source) {
		return polish(source.getEntity());
	}

	/**
	 * The English or the Polish text, depending on the language of the command's player.
	 */
	public static String tr(CommandSourceStack source, String english, String polish) {
		return polish(source) ? polish : english;
	}

	public static String tr(Entity entity, String english, String polish) {
		return polish(entity) ? polish : english;
	}

	/**
	 * Sends a success message. While the recording mode is active (command feedback hidden),
	 * the executing player gets it on the action bar instead, so the chat stays clean.
	 */
	public static int ok(CommandSourceStack source, String english, String polish) {
		String text = tr(source, english, polish);
		MutableComponent message = info(text);

		if (RecMode.isActive() && source.getEntity() instanceof ServerPlayer player) {
			Screens.actionBar(player, Component.literal(text).withStyle(ChatFormatting.GOLD));
		} else {
			source.sendSuccess(() -> message, false);
		}

		return 1;
	}

	public static int fail(CommandSourceStack source, String english, String polish) {
		source.sendFailure(Component.literal(tr(source, english, polish)));
		return 0;
	}

	/**
	 * Turns {@code &c}-style colour codes typed in commands into legacy formatting codes,
	 * which the client still renders inside text components.
	 */
	public static String colors(String text) {
		return text.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
	}

	public static String number(double value) {
		if (Math.abs(value - Math.rint(value)) < 0.005) {
			return String.valueOf((long) Math.rint(value));
		}

		return String.format(Locale.ROOT, value >= 10 ? "%.1f" : "%.2f", value);
	}

	public static String onOff(CommandSourceStack source, boolean value) {
		return value ? tr(source, "ON", "WŁ") : tr(source, "OFF", "WYŁ");
	}
}
