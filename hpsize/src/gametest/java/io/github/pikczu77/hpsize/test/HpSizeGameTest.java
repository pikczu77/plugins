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
import io.github.pikczu77.hpsize.util.Msg;

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
				"hpsize preset fair", "hpsize preset light", "hpsize preset smooth", "hpsize preset film", "hpsize normal 10",
				"hpsize min 0.1", "hpsize max 8", "hpsize smooth 3", "hpsize players on", "hpsize players off",
				"hpsize suffocation off", "hpsize suffocation on", "hpsize sound on", "hpsize sound off", "hpsize glowtiny on",
				"hpsize glowtiny below 0.5", "hpsize glowtiny off", "hpsize freeze", "hpsize unfreeze",
				"hpsize exclude minecraft:pig", "hpsize include minecraft:pig", "hpsize reset")) {
			run(console, command);
		}

		HpSizeConfig config = HpSizeConfig.get();
		helper.assertTrue(config != null, "Config is not loaded");

		// Natural sizes like in the video: 20 HP (a player) = normal size, less health = smaller.
		run(console, "hpsize preset film");
		LivingEntity chicken = helper.spawn(EntityType.CHICKEN, new BlockPos(1, 1, 1));
		LivingEntity cow = helper.spawn(EntityType.COW, new BlockPos(2, 1, 2));

		helper.runAtTickTime(5, () -> {
			assertScale(helper, chicken, 0.2); // 4 HP / 20
			assertScale(helper, cow, 0.5); // 10 HP / 20
			// Bigger, deterministic sizes for the rest of the test.
			config.normalHp = 5;
			config.sanitize();
		});
		helper.runAtTickTime(10, () -> {
			assertScale(helper, cow, 2.0); // 10 HP / 5
			cow.setHealth(5.0F);
		});
		helper.runAtTickTime(15, () -> {
			assertScale(helper, cow, 1.0); // 5 HP / 5
			// A real hit with the "deflating" sound on must not break anything.
			config.shrinkSound = true;
			run(console, "damage @e[type=minecraft:cow,distance=..8,limit=1] 1");
			config.shrinkSound = false;
			run(console, "mobhp @e[type=minecraft:cow,distance=..8] max 20");
		});
		helper.runAtTickTime(20, () -> {
			assertScale(helper, cow, 4.0); // 20 HP / 5
			config.mode = HpSizeConfig.Mode.PERCENT;
			cow.setHealth(10.0F);
		});
		helper.runAtTickTime(25, () -> {
			assertScale(helper, cow, 2.0); // 20 / 5 = x4 at full health, half health -> x2
			config.frozen = true;
			cow.setHealth(20.0F);
		});
		helper.runAtTickTime(30, () -> {
			assertScale(helper, cow, 2.0); // frozen
			config.frozen = false;
			config.enabled = false;
		});
		helper.runAtTickTime(35, () -> {
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

		// Messages follow the player's game language: Polish for pl_pl, English for everyone else and the console.
		helper.assertTrue(Msg.isPolish("pl_pl") && !Msg.isPolish("en_us") && !Msg.isPolish("de_de"), "Wrong language detection");
		helper.assertTrue(Msg.tr(source, "Healed", "Uleczono").equals("Healed"), "An en_us player did not get English");
		helper.assertTrue(Msg.tr(helper.getLevel().getServer().createCommandSourceStack(), "Healed", "Uleczono").equals("Healed"),
				"The console did not get English");

		for (String command : List.of("announce &cTytuł|Podtytuł", "countdown 3 Szukajcie diamentów!", "countdown cancel",
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

		// /go: freeze -> countdown -> START.
		run(source, "go 1 &aPowodzenia!");
		helper.assertTrue(Countdown.isRunning(), "/go did not start the countdown");

		helper.runAtTickTime(40, () -> {
			helper.assertTrue(!Countdown.isRunning(), "countdown did not finish");
			helper.succeed();
		});
	}
}
