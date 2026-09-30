package io.github.pikczu77.upgrades.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Description lines of upgrades: the {highlighted} parts are drawn in the upgrade colour. Shared by the chat message
 * (server) and the unlock banner (client).
 */
public final class Texts {
	private Texts() {
	}

	/** The line with "+ " in front, like in the chat. */
	public static MutableComponent line(Unlockable upgrade, String text) {
		return Component.literal("+ ").withStyle(style -> style.withColor(upgrade.color())).append(description(upgrade, text));
	}

	/** Just the line, gray with the highlights in the upgrade colour. */
	public static MutableComponent description(Unlockable upgrade, String text) {
		MutableComponent line = Component.empty();
		StringBuilder part = new StringBuilder();
		boolean highlighted = false;

		for (char c : text.toCharArray()) {
			if (c == '{' || c == '}') {
				append(line, part, highlighted, upgrade);
				highlighted = c == '{';
			} else {
				part.append(c);
			}
		}

		append(line, part, highlighted, upgrade);
		return line;
	}

	private static void append(MutableComponent line, StringBuilder part, boolean highlighted, Unlockable upgrade) {
		if (part.isEmpty()) {
			return;
		}

		line.append(Component.literal(part.toString()).withStyle(highlighted ? style(upgrade) : Style.EMPTY.withColor(ChatFormatting.GRAY)));
		part.setLength(0);
	}

	public static Style style(Unlockable upgrade) {
		return Style.EMPTY.withColor(TextColor.fromRgb(upgrade.color())).withBold(true);
	}

	public static Unlockable byId(String id) {
		Upgrade upgrade = Upgrade.byId(id);
		return upgrade != null ? upgrade : Bonus.byId(id);
	}
}
