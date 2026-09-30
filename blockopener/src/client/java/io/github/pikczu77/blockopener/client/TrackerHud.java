package io.github.pikczu77.blockopener.client;

import io.github.pikczu77.blockopener.BlockOpener;
import io.github.pikczu77.blockopener.progress.SecretItem;
import io.github.pikczu77.blockopener.registry.ModAttachments;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

/**
 * The collection tracker from the video's overlay: the 10 secret items as black silhouettes that
 * light up once found. Newly found items pulse for a moment. Toggle with J or {@code /bo hud}.
 */
final class TrackerHud implements HudElement {
	private static final int SLOT = 18;
	private static final int PADDING = 3;
	private static final long PULSE_MILLIS = 2500L;

	private final Map<String, Long> foundAt = new HashMap<>();
	private List<String> lastFound = List.of();
	private boolean initialized;

	@Override
	public void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.options.hideGui || !player.getAttachedOrElse(ModAttachments.TRACKER_HUD, true)) {
			return;
		}
		List<String> found = player.getAttachedOrElse(ModAttachments.FOUND_SECRETS, List.of());
		trackNewFinds(found);

		int x = 4;
		int y = 4;
		int width = PADDING * 2 + SLOT * SecretItem.COUNT;
		graphics.fill(x, y, x + width, y + SLOT + PADDING * 2 + 10, 0x66000000);
		Component label = Component.translatable("blockopener.hud.title", found.size(), SecretItem.COUNT);
		graphics.drawString(minecraft.font, found.size() == SecretItem.COUNT ? Tooltips.rainbow(label.getString()) : label.copy().withStyle(ChatFormatting.GOLD),
			x + PADDING, y + PADDING, 0xFFFFFFFF, true);

		long now = Util.getMillis();
		SecretItem[] secrets = SecretItem.values();
		for (int i = 0; i < secrets.length; i++) {
			SecretItem secret = secrets[i];
			int slotX = x + PADDING + i * SLOT + 1;
			int slotY = y + PADDING + 10 + 1;
			if (found.contains(secret.id())) {
				Long at = this.foundAt.get(secret.id());
				if (at != null && now - at < PULSE_MILLIS) {
					float pulse = 1.0F - (now - at) / (float) PULSE_MILLIS;
					int alpha = (int) (pulse * (0.5F + 0.5F * Mth.sin((now - at) / 90.0F)) * 200) & 0xFF;
					graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, (alpha << 24) | secret.color());
				}
				graphics.renderItem(new ItemStack(secret.item()), slotX, slotY);
			} else {
				Identifier sprite = BlockOpener.id("tracker/" + secret.id());
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, slotX, slotY, 16, 16);
			}
		}
	}

	private void trackNewFinds(List<String> found) {
		if (found.equals(this.lastFound)) {
			return;
		}
		long now = Util.getMillis();
		if (this.initialized) {
			for (String id : found) {
				if (!this.lastFound.contains(id)) {
					this.foundAt.put(id, now);
				}
			}
		}
		this.initialized = true;
		this.foundAt.keySet().retainAll(found);
		this.lastFound = found;
	}
}
