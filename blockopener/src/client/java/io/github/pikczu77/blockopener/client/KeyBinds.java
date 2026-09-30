package io.github.pikczu77.blockopener.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.pikczu77.blockopener.BlockOpener;
import io.github.pikczu77.blockopener.network.ModPayloads.Ability;
import io.github.pikczu77.blockopener.network.ModPayloads.AbilityKeyPayload;
import io.github.pikczu77.blockopener.registry.ModAttachments;
import io.github.pikczu77.blockopener.registry.ModItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

/** Ability keys. Everything is validated server side; the client only reports the key press. */
final class KeyBinds {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(BlockOpener.id("main"));

	static final KeyMapping FLIGHT = register("flight", GLFW.GLFW_KEY_M);
	static final KeyMapping SONIC_BOOM = register("sonic_boom", GLFW.GLFW_KEY_G);
	static final KeyMapping TRACKER = register("tracker", GLFW.GLFW_KEY_J);

	private static KeyMapping register(String name, int key) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping("key.blockopener." + name, InputConstants.Type.KEYSYM, key, CATEGORY));
	}

	static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (FLIGHT.consumeClick()) {
				send(Ability.TOGGLE_FLIGHT);
			}
			while (SONIC_BOOM.consumeClick()) {
				send(Ability.SONIC_BOOM);
			}
			while (TRACKER.consumeClick()) {
				send(Ability.TOGGLE_HUD);
			}
		});

		// "Empty hand swing shoots fireballs during flight."
		ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
			boolean flying = player.getAbilities().flying
				&& player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.DIAMOND_LEGGINGS)
				&& Boolean.TRUE.equals(player.getAttached(ModAttachments.DIAMOND_FLIGHT));
			if ((clickCount > 0 || player.tickCount % 8 == 0) && flying && player.getMainHandItem().isEmpty()
				&& (client.hitResult == null || client.hitResult.getType() == HitResult.Type.MISS)) {
				send(Ability.FIREBALL);
			}
			return false;
		});
	}

	private static void send(Ability ability) {
		if (Minecraft.getInstance().getConnection() != null && ClientPlayNetworking.canSend(AbilityKeyPayload.TYPE)) {
			ClientPlayNetworking.send(new AbilityKeyPayload(ability));
		}
	}

	private KeyBinds() {
	}
}
