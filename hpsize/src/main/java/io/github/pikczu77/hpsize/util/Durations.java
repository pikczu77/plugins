package io.github.pikczu77.hpsize.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.network.chat.Component;

/**
 * Parses and formats durations such as {@code 90}, {@code 90s}, {@code 5m}, {@code 1h30m} or {@code 10:00}.
 */
public final class Durations {
	private static final Pattern PART = Pattern.compile("(\\d+(?:[.,]\\d+)?)([hmst]?)");
	private static final SimpleCommandExceptionType INVALID = new SimpleCommandExceptionType(
			Component.literal("Nieprawidłowy czas. Przykłady: 90s, 5m, 1h30m, 10:00"));

	private Durations() {
	}

	/**
	 * @return the duration in ticks (20 ticks = 1 second)
	 */
	public static long parseTicks(String input) throws CommandSyntaxException {
		String text = input.trim().toLowerCase(Locale.ROOT).replace(" ", "");

		if (text.isEmpty()) {
			throw INVALID.create();
		}

		if (text.contains(":")) {
			String[] parts = text.split(":");

			if (parts.length > 3) {
				throw INVALID.create();
			}

			long seconds = 0;

			for (String part : parts) {
				if (part.isEmpty() || !part.chars().allMatch(Character::isDigit)) {
					throw INVALID.create();
				}

				seconds = seconds * 60 + Long.parseLong(part);
			}

			return seconds * 20;
		}

		Matcher matcher = PART.matcher(text);
		int position = 0;
		double seconds = 0;

		while (position < text.length() && matcher.find(position) && matcher.start() == position) {
			double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
			seconds += switch (matcher.group(2)) {
				case "h" -> value * 3600;
				case "m" -> value * 60;
				case "t" -> value / 20.0;
				default -> value;
			};
			position = matcher.end();
		}

		if (position != text.length()) {
			throw INVALID.create();
		}

		return Math.round(seconds * 20);
	}

	/**
	 * Formats ticks as {@code m:ss} or {@code h:mm:ss}.
	 *
	 * @param roundUp round partial seconds up (useful for countdowns, which should hit 0:00 exactly at the end)
	 */
	public static String format(long ticks, boolean roundUp) {
		long totalSeconds = roundUp ? (ticks + 19) / 20 : ticks / 20;
		long hours = totalSeconds / 3600;
		long minutes = totalSeconds / 60 % 60;
		long seconds = totalSeconds % 60;

		if (hours > 0) {
			return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
		}

		return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
	}
}
