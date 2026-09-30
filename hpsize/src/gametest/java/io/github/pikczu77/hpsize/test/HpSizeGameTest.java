package io.github.pikczu77.hpsize.test;

import java.util.List;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.pikczu77.hpsize.config.HpSizeConfig;
import io.github.pikczu77.hpsize.rec.Countdown;
import io.github.pikczu77.hpsize.rec.Freeze;
import io.github.pikczu77.hpsize.rec.RecMode;
import io.github.pikczu77.hpsize.rec.RecTimer;

public class HpSizeGameTest {
	private static int run(CommandSourceStack source, String command) {
		try {
			int result = source.getServer().getCommands().getDispatcher().execute(command, source);

			if (result <= 0) {
				throw new AssertionError("/" + command + " returned " + result);
			}

			return result;
		} catch (CommandSyntaxException e) {
			throw new AssertionError("/" + command + " failed: " + e.getMessage(), e);
		}
	}

	private static CommandSourceStack source(GameTestHelper helper, ServerPlayer player) {
		ServerLevel level = helper.getLevel();
		return level.getServer().createCommandSourceStack()
				.withLevel(level)
				.withEntity(player)
				.withPosition(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
	}

	private static void assertScale(GameTestHelper helper, LivingEntity entity, double expected) {
		helper.assertTrue(Math.abs(entity.getScale() - expected) < 0.05,
				"Expected scale x" + expected + " but was x" + entity.getScale() + " (HP " + entity.getHealth() + ")");
	}

	@GameTest(maxTicks = 200)
	public void scaleFollowsHealth(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		CommandSourceStack console = server.createCommandSourceStack().withLevel(helper.getLevel())
				.withPosition(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)));

		// Every /hpsize sub command must parse and succeed.
		for (String command : List.of("hpsize", "hpsize off", "hpsize on", "hpsize mode percent", "hpsize mode sqrt", "hpsize mode max",
				"hpsize preset fair", "hpsize preset light", "hpsize preset smooth", "hpsize preset film", "hpsize factor 0.5",
				"hpsize min 0.1", "hpsize max 8", "hpsize smooth 3", "hpsize players on", "hpsize players off",
				"hpsize suffocation off", "hpsize suffocation on", "hpsize bar off", "hpsize bar on", "hpsize sound on", "hpsize sound off", "hpsize glowtiny on",
				"hpsize glowtiny below 0.5", "hpsize glowtiny off", "hpsize freeze", "hpsize unfreeze",
				"hpsize exclude minecraft:pig", "hpsize include minecraft:pig", "hpsize reset")) {
			run(console, command);
		}

		HpSizeConfig config = HpSizeConfig.get();
		helper.assertTrue(config != null, "Config is not loaded");
		// Small, deterministic sizes so the cow fits into the test area.
		config.mode = HpSizeConfig.Mode.HEALTH;
		config.factor = 0.2;
		config.smooth = 0;
		config.sanitize();

		LivingEntity cow = helper.spawn(EntityType.COW, new BlockPos(2, 1, 2));

		helper.runAtTickTime(5, () -> {
			assertScale(helper, cow, 2.0); // 10 HP x 0.2
			cow.setHealth(5.0F);
		});
		helper.runAtTickTime(10, () -> {
			assertScale(helper, cow, 1.0); // 5 HP x 0.2
			// A real hit with the "deflating" sound on must not break anything.
			config.shrinkSound = true;
			run(console, "damage @e[type=minecraft:cow,distance=..8,limit=1] 1");
			config.shrinkSound = false;
			run(console, "mobhp @e[type=minecraft:cow,distance=..8] max 20");
		});
		helper.runAtTickTime(15, () -> {
			assertScale(helper, cow, 4.0); // 20 HP x 0.2
			config.mode = HpSizeConfig.Mode.PERCENT;
			cow.setHealth(10.0F);
		});
		helper.runAtTickTime(20, () -> {
			assertScale(helper, cow, 2.0); // 20 x 0.2 = x4 at full health, half health -> x2
			config.frozen = true;
			cow.setHealth(20.0F);
		});
		helper.runAtTickTime(25, () -> {
			assertScale(helper, cow, 2.0); // frozen
			config.frozen = false;
			config.enabled = false;
		});
		helper.runAtTickTime(30, () -> {
			assertScale(helper, cow, 1.0); // turned off -> normal size
			run(console, "hpsize reset");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 300)
	public void recordingCommands(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = FakePlayer.get(level);
		player.snapTo(helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)), 0.0F, 0.0F);
		CommandSourceStack source = source(helper, player);

		for (String command : List.of("timer start", "timer pause", "timer resume", "timer add 1m", "timer remove 30s",
				"timer set 5:00", "timer display actionbar", "timer display bossbar", "timer label &eDzień 1", "timer label",
				"timer", "timer stop", "announce &cTytuł|Podtytuł", "countdown 3 Szukajcie diamentów!", "countdown cancel",
				"spawnsized minecraft:cow 20 2", "spawnsized minecraft:chicken 4", "mobhp @e[type=minecraft:cow,distance=..10] 5",
				"glow @e[type=minecraft:cow,distance=..10] 5", "glow @e[type=minecraft:cow,distance=..10] 0",
				"mute @e[type=minecraft:chicken,distance=..10]", "unmute @e[type=minecraft:chicken,distance=..10]",
				"heal @e[type=minecraft:cow,distance=..10]", "cleanup all 16", "recmode")) {
			run(source, command);
		}

		helper.assertTrue(level.getEntitiesOfClass(LivingEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16),
				entity -> entity.getType() == EntityType.COW).size() >= 2, "/spawnsized did not spawn the cows");

		// Recording mode hides command feedback and restores it afterwards.
		run(source, "recmode on");
		helper.assertTrue(RecMode.isActive() && !level.getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK), "recmode on did not apply");
		run(source, "recmode off");
		helper.assertTrue(!RecMode.isActive() && level.getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK), "recmode off did not restore");

		// Player helpers.
		run(source, "nv");
		helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "/nv did not give night vision");
		run(source, "nv");
		helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "/nv did not toggle night vision off");
		run(source, "heal");

		helper.assertTrue(Freeze.freeze(player), "freeze failed");
		helper.assertTrue(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) <= 0.0001, "frozen player can still walk");
		helper.assertTrue(Freeze.unfreeze(player), "unfreeze failed");
		helper.assertTrue(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) > 0.0001, "unfrozen player cannot walk");

		GameType before = player.gameMode.getGameModeForPlayer();
		run(source, "cam");
		helper.assertTrue(player.isSpectator(), "/cam did not switch to spectator");
		run(source, "cam");
		helper.assertTrue(player.gameMode.getGameModeForPlayer() == before, "/cam did not restore the game mode");

		// Countdown + timer: /go 1 10s -> after the countdown a 10 s timer runs.
		run(source, "go 1 10s");
		helper.assertTrue(Countdown.isRunning(), "/go did not start the countdown");

		helper.runAtTickTime(40, () -> {
			helper.assertTrue(!Countdown.isRunning(), "countdown did not finish");
			helper.assertTrue(RecTimer.isVisible() && RecTimer.describe().startsWith("Odliczanie"), "timer did not start after /go: " + RecTimer.describe());
			run(source, "timer stop");
			helper.succeed();
		});
	}
}
