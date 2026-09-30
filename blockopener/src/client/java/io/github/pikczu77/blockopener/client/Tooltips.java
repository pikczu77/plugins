package io.github.pikczu77.blockopener.client;

import io.github.pikczu77.blockopener.progress.SecretItem;
import io.github.pikczu77.blockopener.registry.ModItems;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltips in the style of the video: animated rainbow names, a short description and the powers,
 * which are hidden behind "Press SHIFT to view the powers" so they do not spoil a first look on camera.
 */
final class Tooltips {
	private static final int MAX_POWER_LINES = 4;

	static void init() {
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			if (stack.is(ModItems.BLOCK_OPENER)) {
				decorate(stack, lines, "block_opener", 0xB36BFF, null);
				return;
			}
			Optional<SecretItem> secret = SecretItem.of(stack);
			secret.ifPresent(value -> decorate(stack, lines, value.id(), value.color(), value));
		});
	}

	private static void decorate(ItemStack stack, List<Component> lines, String id, int color, SecretItem secret) {
		if (!lines.isEmpty() && !stack.has(DataComponents.CUSTOM_NAME)) {
			lines.set(0, rainbow(lines.getFirst().getString()));
		}
		int insert = Math.min(1, lines.size());
		List<Component> extra = new java.util.ArrayList<>();
		extra.add(Component.translatable("tooltip.blockopener." + id + ".lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

		boolean shift = Minecraft.getInstance().hasShiftDown();
		if (shift || secret == null) {
			for (int i = 1; i <= MAX_POWER_LINES; i++) {
				String key = "tooltip.blockopener." + id + ".power." + i;
				if (!Language.getInstance().has(key)) {
					break;
				}
				extra.add(Component.literal("▶ ").withColor(color).append(Component.translatable(key,
					Component.keybind("key.blockopener.flight"), Component.keybind("key.blockopener.sonic_boom")).withColor(lighten(color))));
			}
		} else {
			extra.add(Component.translatable("tooltip.blockopener.shift", Component.literal("SHIFT").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
				.withStyle(ChatFormatting.DARK_GRAY));
		}
		if (secret != null) {
			extra.add(Component.translatable("tooltip.blockopener.source", secret.mainSourceBlock().getName())
				.withStyle(ChatFormatting.DARK_PURPLE));
		}
		lines.addAll(insert, extra);
	}

	/** Per-letter rainbow that slowly scrolls. */
	static MutableComponent rainbow(String text) {
		MutableComponent result = Component.empty();
		float offset = (Util.getMillis() % 4000L) / 4000.0F;
		for (int i = 0; i < text.length(); i++) {
			float hue = (offset + i / 14.0F) % 1.0F;
			result.append(Component.literal(String.valueOf(text.charAt(i))).withColor(Mth.hsvToRgb(hue, 0.65F, 1.0F)));
		}
		return result;
	}

	private static int lighten(int color) {
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		r = r + (255 - r) / 2;
		g = g + (255 - g) / 2;
		b = b + (255 - b) / 2;
		return (r << 16) | (g << 8) | b;
	}

	private Tooltips() {
	}
}
