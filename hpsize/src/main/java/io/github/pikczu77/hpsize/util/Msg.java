package io.github.pikczu77.hpsize.util;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import io.github.pikczu77.hpsize.rec.RecMode;

/**
 * Chat helpers. All user facing text is Polish.
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
	 * Sends a success message. While the recording mode is active (command feedback hidden),
	 * the executing player gets it on the action bar instead, so the chat stays clean.
	 */
	public static int ok(CommandSourceStack source, String text) {
		MutableComponent message = info(text);

		if (RecMode.isActive() && source.getEntity() instanceof ServerPlayer player) {
			Screens.actionBar(player, Component.literal(text).withStyle(ChatFormatting.GOLD));
		} else {
			source.sendSuccess(() -> message, false);
		}

		return 1;
	}

	public static int fail(CommandSourceStack source, String text) {
		source.sendFailure(Component.literal(text));
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

	public static String onOff(boolean value) {
		return value ? "WŁ" : "WYŁ";
	}
}
