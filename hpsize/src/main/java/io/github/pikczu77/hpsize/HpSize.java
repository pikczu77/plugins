package io.github.pikczu77.hpsize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import io.github.pikczu77.hpsize.command.HpSizeCommand;
import io.github.pikczu77.hpsize.command.MobCommands;
import io.github.pikczu77.hpsize.command.RecCommands;
import io.github.pikczu77.hpsize.config.HpSizeConfig;
import io.github.pikczu77.hpsize.rec.CamMode;
import io.github.pikczu77.hpsize.rec.Countdown;
import io.github.pikczu77.hpsize.rec.Freeze;
import io.github.pikczu77.hpsize.rec.RecMode;
import io.github.pikczu77.hpsize.rec.RecTimer;
import io.github.pikczu77.hpsize.scale.HealthBars;
import io.github.pikczu77.hpsize.scale.ScaleManager;

public class HpSize implements ModInitializer {
	public static final String MOD_ID = "hpsize";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			HpSizeCommand.register(dispatcher, buildContext);
			MobCommands.register(dispatcher, buildContext);
			RecCommands.register(dispatcher);
		});

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			HpSizeConfig.load(server);
			RecMode.load(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			// Static state must not leak between singleplayer worlds.
			HpSizeConfig.unload();
			RecMode.unload();
			Countdown.reset();
			RecTimer.reset();
			Freeze.reset();
			CamMode.reset();
			HealthBars.reset();
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ScaleManager.tick(server);
			HealthBars.tick(server);
			Countdown.tick(server);
			RecTimer.tick(server);
			Freeze.tick(server);
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register(ScaleManager::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (source.getEntity() instanceof ServerPlayer player) {
				HealthBars.remember(player, entity);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> HealthBars.forget(oldPlayer));
		ServerPlayerEvents.LEAVE.register(player -> {
			HealthBars.forget(player);
			RecTimer.removePlayer(player);
		});

		LOGGER.info("HP Size loaded - mobs are as big as their health!");
	}
}
