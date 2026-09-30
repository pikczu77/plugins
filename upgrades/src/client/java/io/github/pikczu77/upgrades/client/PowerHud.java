package io.github.pikczu77.upgrades.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import io.github.pikczu77.upgrades.Upgrades;
import io.github.pikczu77.upgrades.upgrade.Upgrade;

/**
 * With an empty hand, a small line above the hotbar shows which power the right-click (and sneak + right-click) fires.
 */
public final class PowerHud {
	private PowerHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Upgrades.id("powers"), (graphics, deltaTracker) -> render(graphics));
	}

	private static void render(GuiGraphics graphics) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.options.hideGui || !minecraft.player.getMainHandItem().isEmpty() || minecraft.player.isSpectator()) {
			return;
		}

		int id = minecraft.player.getId();
		Upgrade click = ClientUpgrades.selected(id, Upgrade.Group.CLICK);
		Upgrade sneak = ClientUpgrades.selected(id, Upgrade.Group.SNEAK_CLICK);

		if (click == null && sneak == null) {
			return;
		}

		MutableComponent line = Component.empty();

		if (click != null) {
			line.append(Component.literal("PPM: ").withStyle(ChatFormatting.GRAY))
					.append(Component.literal(click.displayName).withStyle(style -> style.withColor(click.color)));
		}

		if (sneak != null) {
			if (click != null) {
				line.append(Component.literal("   "));
			}

			line.append(Component.literal("Kucnij+PPM: ").withStyle(ChatFormatting.GRAY))
					.append(Component.literal(sneak.displayName).withStyle(style -> style.withColor(sneak.color)));
		}

		line.append(Component.literal("   [").withStyle(ChatFormatting.DARK_GRAY))
				.append(PowerInput.CYCLE.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.DARK_GRAY))
				.append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));

		int width = minecraft.font.width(line);
		int x = (graphics.guiWidth() - width) / 2;
		int y = graphics.guiHeight() - 59 - (minecraft.player.getArmorValue() > 0 ? 10 : 0);
		graphics.drawString(minecraft.font, line, x, y, 0xFFFFFFFF, true);
	}
}
