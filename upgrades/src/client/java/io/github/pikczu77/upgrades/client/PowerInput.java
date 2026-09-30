package io.github.pikczu77.upgrades.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import io.github.pikczu77.upgrades.Upgrades;
import io.github.pikczu77.upgrades.net.Payloads;

/**
 * Empty-hand right-clicks and the power key (V: next power, sneak + V: next sneak power).
 */
public final class PowerInput {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Upgrades.id("main"));
	public static final KeyMapping CYCLE = new KeyMapping("key.upgrades.cycle_power", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

	private PowerInput() {
	}

	public static void register() {
		KeyBindingHelper.registerKeyBinding(CYCLE);
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (CYCLE.consumeClick()) {
				if (minecraft.player != null && ClientPlayNetworking.canSend(Payloads.CyclePower.TYPE)) {
					ClientPlayNetworking.send(new Payloads.CyclePower(minecraft.player.isShiftKeyDown()));
				}
			}
		});
	}

	public static void emptyHandClick(LocalPlayer player, int target) {
		if (!player.getMainHandItem().isEmpty() || player.isSpectator() || ClientUpgrades.mask(player.getId()) == 0L
				|| !ClientPlayNetworking.canSend(Payloads.UsePower.TYPE)) {
			return;
		}

		ClientPlayNetworking.send(new Payloads.UsePower(player.isShiftKeyDown(), target));
	}
}
