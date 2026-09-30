package io.github.pikczu77.hpsize.test;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.pikczu77.hpsize.client.CleanHud;
import io.github.pikczu77.hpsize.client.HpSizeClient;

/**
 * Starts a real client, creates a world and checks the mod end to end. Screenshots land in the run directory.
 */
@SuppressWarnings("UnstableApiUsage")
public class HpSizeClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getClientWorld().waitForChunksRender();

			server.runCommand("time set noon");
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 5");
			// The giant golem must not shrink from suffocating in nearby blocks during the test.
			server.runCommand("hpsize suffocation off");
			// Natural health: iron golem 100 HP -> x5, cow 10 HP -> x0.5, chicken 4 HP -> x0.2.
			server.runCommand("execute as @a at @s positioned ^ ^ ^24 run spawnsized minecraft:iron_golem 100");
			server.runCommand("execute as @a at @s positioned ^-2 ^ ^5 run spawnsized minecraft:cow 10");
			server.runCommand("execute as @a at @s positioned ^2 ^ ^4 run spawnsized minecraft:chicken 4");
			context.waitTicks(60);
			singleplayer.getClientWorld().waitForChunksRender();
			context.takeScreenshot("hpsize-01-natural-sizes");

			double golemScale = context.computeOnClient(minecraft -> largestScale(minecraft, EntityType.IRON_GOLEM));
			double cowScale = context.computeOnClient(minecraft -> largestScale(minecraft, EntityType.COW));
			double chickenScale = context.computeOnClient(minecraft -> largestScale(minecraft, EntityType.CHICKEN));

			if (golemScale < 4.5 || Math.abs(cowScale - 0.5) > 0.1 || Math.abs(chickenScale - 0.2) > 0.05) {
				throw new AssertionError("Wrong sizes on the client: golem x" + golemScale + ", cow x" + cowScale + ", chicken x" + chickenScale);
			}

			server.runCommand("countdown 3 Minecraft, ale moby są tak duże jak ich HP");
			context.waitTicks(6);
			context.takeScreenshot("hpsize-02-countdown");

			context.getInput().pressKey(HpSizeClient.CLEAN_HUD);
			context.waitTicks(3);

			if (!context.computeOnClient(minecraft -> CleanHud.isEnabled())) {
				throw new AssertionError("Clean HUD key did not toggle the clean HUD");
			}

			context.waitTicks(40);
			context.takeScreenshot("hpsize-03-clean-hud");
			context.getInput().pressKey(HpSizeClient.CLEAN_HUD);

			// More health = bigger cow, less health = smaller golem.
			server.runCommand("mobhp @e[type=minecraft:cow] max 40");
			server.runCommand("mobhp @e[type=minecraft:iron_golem] 20");
			context.waitTicks(60);
			double grown = context.computeOnClient(minecraft -> largestScale(minecraft, EntityType.COW));
			double shrunk = context.computeOnClient(minecraft -> largestScale(minecraft, EntityType.IRON_GOLEM));
			context.takeScreenshot("hpsize-04-cow-grown-golem-shrunk");

			if (grown < 1.8 || shrunk > 1.2) {
				throw new AssertionError("Sizes did not follow the health: cow x" + grown + ", golem x" + shrunk);
			}
		}
	}

	private static double largestScale(Minecraft minecraft, EntityType<?> type) {
		double largest = 0.0;

		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity instanceof LivingEntity living && entity.getType() == type) {
				largest = Math.max(largest, living.getScale());
			}
		}

		return largest;
	}
}
