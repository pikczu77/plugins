package io.github.pikczu77.upgrades;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import io.github.pikczu77.upgrades.ability.Abilities;
import io.github.pikczu77.upgrades.command.UpgradesCommand;
import io.github.pikczu77.upgrades.config.UpgradesConfig;
import io.github.pikczu77.upgrades.entity.ModEntities;
import io.github.pikczu77.upgrades.net.Payloads;
import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;
import io.github.pikczu77.upgrades.util.TempDisplays;

public class Upgrades implements ModInitializer {
	public static final String MOD_ID = "upgrades";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEntities.register();
		Payloads.register();

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> UpgradesCommand.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTING.register(UpgradesConfig::load);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			// Static state must not leak between singleplayer worlds.
			UpgradesConfig.unload();
			UpgradeManager.reset();
			Abilities.reset();
			TempDisplays.reset();
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> UpgradeManager.refresh(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			Abilities.forget(handler.player);
			UpgradeManager.forget(handler.player);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> UpgradeManager.refresh(newPlayer));
		EntityTrackingEvents.START_TRACKING.register((entity, watcher) -> {
			if (entity instanceof ServerPlayer player) {
				UpgradeManager.sendTo(watcher, player);
			}
		});
		ServerEntityEvents.ENTITY_LOAD.register(TempDisplays::onEntityLoad);

		ServerPlayNetworking.registerGlobalReceiver(Payloads.UsePower.TYPE, (payload, context) ->
				Abilities.usePower(context.player(), payload.sneaking(), payload.target()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.CyclePower.TYPE, (payload, context) ->
				UpgradeManager.cycle(context.player(), payload.sneaking() ? Upgrade.Group.SNEAK_CLICK : Upgrade.Group.CLICK));

		ServerTickEvents.END_SERVER_TICK.register(Abilities::tick);
		Abilities.register();

		LOGGER.info("Body Upgrades loaded - every advancement upgrades your body!");
	}
}
