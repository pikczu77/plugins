package io.github.pikczu77.blockopener;

import io.github.pikczu77.blockopener.ability.ModAbilities;
import io.github.pikczu77.blockopener.challenge.ChallengeTimer;
import io.github.pikczu77.blockopener.command.BlockOpenerCommand;
import io.github.pikczu77.blockopener.command.Markers;
import io.github.pikczu77.blockopener.network.ModPayloads;
import io.github.pikczu77.blockopener.opening.BlockOpening;
import io.github.pikczu77.blockopener.registry.ModAttachments;
import io.github.pikczu77.blockopener.registry.ModEntities;
import io.github.pikczu77.blockopener.registry.ModFluids;
import io.github.pikczu77.blockopener.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlockOpener implements ModInitializer {
	public static final String MOD_ID = "blockopener";
	public static final Logger LOGGER = LoggerFactory.getLogger("Block Opener");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModAttachments.init();
		ModFluids.init();
		ModItems.init();
		ModEntities.init();
		ModPayloads.init();
		BlockOpening.init();
		ModAbilities.init();
		ChallengeTimer.init();
		Markers.init();
		CommandRegistrationCallback.EVENT.register(BlockOpenerCommand::register);
		LOGGER.info("Every block can be opened now.");
	}
}
