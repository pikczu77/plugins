package io.github.pikczu77.upgrades.test;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import io.github.pikczu77.upgrades.client.ClientUpgrades;
import io.github.pikczu77.upgrades.entity.CombatClone;
import io.github.pikczu77.upgrades.upgrade.Upgrade;

/**
 * Starts a real client, unlocks the upgrades one by one and takes screenshots of the body from the front and the
 * back. Screenshots land in the run directory (build/run/clientGameTest/screenshots).
 */
@SuppressWarnings("UnstableApiUsage")
public class UpgradesClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			context.getInput().resizeWindow(1280, 720);
			singleplayer.getClientWorld().waitForChunksRender();

			server.runCommand("time set noon");
			server.runCommand("gamerule spawn_monsters false");
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 0");
			context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT));

			// First few upgrades, like the start of the video. The unlock banner slides down from the top.
			server.runCommand("upgrades give @a fist_pickaxe");
			context.waitTicks(18);
			context.takeScreenshot("upgrades-00-banner");
			server.runCommand("upgrades give @a vein_miner");
			server.runCommand("upgrades give @a green_thumb");
			context.waitTicks(40);
			context.takeScreenshot("upgrades-01-first-three");
			// Clean shots of the body from here on (no chat, no toasts).
			context.runOnClient(minecraft -> minecraft.options.hideGui = true);
			context.takeScreenshot("upgrades-01b-first-three-clean");

			long mask = context.computeOnClient(minecraft -> ClientUpgrades.mask(minecraft.player.getId()));

			if (!Upgrade.has(mask, Upgrade.GREEN_THUMB) || !Upgrade.has(mask, Upgrade.VEIN_MINER)) {
				throw new AssertionError("The client did not get the upgrades, mask: " + Long.toBinaryString(mask));
			}

			// Banner with one of the mod's own part models, and a bonus banner.
			context.runOnClient(minecraft -> minecraft.options.hideGui = false);
			context.waitTicks(80);
			server.runCommand("upgrades give @a golem_arm");
			context.waitTicks(18);
			context.takeScreenshot("upgrades-00b-banner-golem");
			context.waitTicks(70);
			server.runCommand("upgrades give @a terrible_fortress");
			context.waitTicks(18);
			context.takeScreenshot("upgrades-00c-banner-bonus");
			context.waitTicks(70);
			context.runOnClient(minecraft -> minecraft.options.hideGui = true);

			// Rabbit feet (double jump) and the pocket totem (second life).
			server.runCommand("gamemode survival @a");
			server.runCommand("upgrades give @a trials_edition");
			server.runCommand("upgrades give @a crying_obsidian");
			context.waitTicks(20);
			context.takeScreenshot("upgrades-00d-feet-and-totem");
			context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(5);
			context.takeScreenshot("upgrades-00d-feet-back");
			context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			doubleJump(context);
			secondLife(context, server);
			server.runCommand("gamemode creative @a");

			// The middle of the video.
			for (String id : new String[] {"golem_arm", "sword_boot", "clone_1", "nap", "deal_sniffer", "trigger_finger", "mini_shields",
					"vein_miner_2", "vein_miner_3", "clone_2", "hot_hands"}) {
				server.runCommand("upgrades give @a " + id);
			}

			context.waitTicks(60);
			context.takeScreenshot("upgrades-02-front-middle");
			context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(10);
			context.takeScreenshot("upgrades-03-back-middle");

			// Everything (the end of the video).
			server.runCommand("upgrades give @a all");
			context.waitTicks(80);
			context.takeScreenshot("upgrades-04-back-all");
			context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(10);
			context.takeScreenshot("upgrades-05-front-all");

			long all = context.computeOnClient(minecraft -> ClientUpgrades.mask(minecraft.player.getId()));

			if (Long.bitCount(all) != Upgrade.VALUES.size()) {
				throw new AssertionError("Not every upgrade reached the client: " + Long.bitCount(all) + "/" + Upgrade.VALUES.size());
			}

			// The dragon egg upgrade breeds little clones around the player.
			int clones = context.computeOnClient(UpgradesClientGameTest::countClones);

			if (clones < 1) {
				throw new AssertionError("The next generation did not spawn any clones");
			}

			context.runOnClient(minecraft -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 180 20");
			context.waitTicks(20);
			context.takeScreenshot("upgrades-06-clones");
			context.runOnClient(minecraft -> {
				minecraft.options.setCameraType(CameraType.FIRST_PERSON);
				minecraft.options.hideGui = false;
			});
		}
	}

	/** A second jump in the air must lift the player well above a normal jump (about 1.25 blocks). */
	private static void doubleJump(ClientGameTestContext context) {
		double start = context.computeOnClient(minecraft -> minecraft.player.getY());
		context.getInput().holdKeyFor(options -> options.keyJump, 2);
		context.waitTicks(8);
		context.getInput().holdKeyFor(options -> options.keyJump, 2);
		double highest = start;

		for (int i = 0; i < 25; i++) {
			context.waitTick();
			highest = Math.max(highest, context.computeOnClient(minecraft -> minecraft.player.getY()));
		}

		if (highest - start < 2.0) {
			throw new AssertionError("The double jump did not work, highest point " + (highest - start) + " blocks up");
		}

		context.waitTicks(20);
	}

	/** Deadly damage uses the second life: the player lives and the pocket totem disappears until it recharges. */
	private static void secondLife(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("damage @p 100 minecraft:mob_attack");
		context.waitTicks(10);
		context.takeScreenshot("upgrades-00e-second-life");
		boolean alive = context.computeOnClient(minecraft -> minecraft.player.isAlive() && minecraft.player.getHealth() > 0.0F);
		boolean hidden = context.computeOnClient(minecraft -> !ClientUpgrades.secondLifeReady(minecraft.player.getId()));

		if (!alive || !hidden) {
			throw new AssertionError("The second life did not save the player (alive " + alive + ", totem hidden " + hidden + ")");
		}

		server.runCommand("effect clear @a");
		server.runCommand("effect give @a minecraft:instant_health 1 10");
		context.waitTicks(10);
	}

	private static int countClones(Minecraft minecraft) {
		int count = 0;

		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity instanceof CombatClone) {
				count++;
			}
		}

		return count;
	}
}
