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
			server.runCommand("execute as @a at @s positioned ^ ^ ^16 run spawnsized minecraft:cow 10");
			server.runCommand("execute as @a at @s positioned ^-7 ^ ^9 run spawnsized minecraft:chicken 4");
			server.runCommand("execute as @a at @s positioned ^5 ^ ^6 run spawnsized minecraft:silverfish 1");
			context.waitTicks(60);
			singleplayer.getClientWorld().waitForChunksRender();
			context.takeScreenshot("hpsize-01-giant-mobs");

			double cowScale = context.computeOnClient(HpSizeClientGameTest::largestCowScale);

			if (cowScale < 5.0) {
				throw new AssertionError("The client does not see a giant cow, largest scale: " + cowScale);
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

			// Less health = smaller cow (size = health).
			server.runCommand("mobhp @e[type=minecraft:cow] 3");
			context.waitTicks(40);
			double shrunk = context.computeOnClient(HpSizeClientGameTest::largestCowScale);
			context.takeScreenshot("hpsize-04-cow-shrunk");

			if (shrunk > 3.5) {
				throw new AssertionError("The cow did not shrink after losing health, scale: " + shrunk);
			}
		}
	}

	private static double largestCowScale(Minecraft minecraft) {
		double largest = 0.0;

		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity instanceof LivingEntity living && entity.getType() == EntityType.COW) {
				largest = Math.max(largest, living.getScale());
			}
		}

		return largest;
	}
}
