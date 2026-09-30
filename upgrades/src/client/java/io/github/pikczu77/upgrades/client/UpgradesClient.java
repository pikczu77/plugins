package io.github.pikczu77.upgrades.client;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.EntityType;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;

import io.github.pikczu77.upgrades.client.render.BodyPartsLayer;
import io.github.pikczu77.upgrades.client.render.CombatCloneRenderer;
import io.github.pikczu77.upgrades.entity.ModEntities;
import io.github.pikczu77.upgrades.net.Payloads;

/**
 * Client part: body parts on players, clone rendering, empty-hand powers and the power key.
 */
public class UpgradesClient implements ClientModInitializer {
	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void onInitializeClient() {
		ClientUpgrades.install();
		ClientPlayNetworking.registerGlobalReceiver(Payloads.SyncUpgrades.TYPE, (payload, context) -> ClientUpgrades.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.UnlockBanner.TYPE, (payload, context) -> UnlockBanner.accept(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientUpgrades.clear();
			UnlockBanner.clear();
		});

		EntityRenderers.register(ModEntities.COMBAT_CLONE, CombatCloneRenderer::new);
		EntityRenderers.register(ModEntities.THROWN_OBSIDIAN, context -> new ThrownItemRenderer<>(context, 2.0F, false));
		EntityRenderers.register(ModEntities.HOMING_FIREBALL, context -> new ThrownItemRenderer<>(context, 2.5F, true));

		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (type == EntityType.PLAYER && renderer instanceof AvatarRenderer<?> avatar) {
				helper.register(new BodyPartsLayer((AvatarRenderer) avatar));
			}
		});

		PowerInput.register();
		PowerHud.register();
		UnlockBanner.register();
	}
}
