package io.github.pikczu77.hpsize.client;

import java.util.List;

import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/**
 * "Clean HUD": hides chosen HUD parts (hearts, hotbar, crosshair...) but, unlike F1,
 * keeps the hand, chat, titles and boss bars (the timer and the mob health bar) visible.
 */
public final class CleanHud {
	public static final List<Identifier> ELEMENTS = List.of(
			VanillaHudElements.MISC_OVERLAYS, VanillaHudElements.CROSSHAIR, VanillaHudElements.SPECTATOR_MENU,
			VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR, VanillaHudElements.HEALTH_BAR,
			VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH,
			VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL, VanillaHudElements.HELD_ITEM_TOOLTIP,
			VanillaHudElements.SPECTATOR_TOOLTIP, VanillaHudElements.STATUS_EFFECTS, VanillaHudElements.BOSS_BAR,
			VanillaHudElements.SLEEP, VanillaHudElements.DEMO_TIMER, VanillaHudElements.SCOREBOARD,
			VanillaHudElements.OVERLAY_MESSAGE, VanillaHudElements.TITLE_AND_SUBTITLE, VanillaHudElements.CHAT,
			VanillaHudElements.PLAYER_LIST, VanillaHudElements.SUBTITLES);

	private static boolean enabled;

	private CleanHud() {
	}

	public static void register() {
		for (Identifier id : ELEMENTS) {
			String name = id.getPath();
			HudElementRegistry.replaceElement(id, original -> (HudElement) (graphics, deltaTracker) -> {
				if (!isHidden(name)) {
					original.render(graphics, deltaTracker);
				}
			});
		}
	}

	public static boolean isHidden(String element) {
		return enabled && ClientConfig.get().hiddenHud.contains(element);
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}

	public static List<String> names() {
		return ELEMENTS.stream().map(Identifier::getPath).toList();
	}
}
