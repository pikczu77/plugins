package io.github.pikczu77.hpsize.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

import io.github.pikczu77.hpsize.client.CleanHud;

/**
 * Sets up the scenes of the Modrinth gallery and takes Full HD screenshots of them
 * ({@code gallery-*.png} in the client game test run directory).
 */
@SuppressWarnings("UnstableApiUsage")
public class HpSizeShowcaseClientGameTest implements FabricClientGameTest {
	/** Tag of the mobs placed for the current scene. */
	private static final String TAG = "hpsize_show";

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			singleplayer.getClientWorld().waitForChunksRender();

			// A real Full HD window, so the GUI (titles) is laid out like on a normal screen.
			context.getInput().resizeWindow(1920, 1080);
			// Hostile mobs must not be removed in peaceful.
			server.runCommand("difficulty easy");
			server.runCommand("hpsize reset");
			server.runCommand("hpsize suffocation off");
			server.runCommand("weather clear");
			// No "Your game mode has been updated" and similar messages in the chat.
			server.runCommand("recmode on");
			// Spectator camera with the clean HUD: no hand, hotbar or crosshair, but name tags stay visible (unlike F1).
			server.runCommand("gamemode spectator @a");
			context.runOnClient(minecraft -> CleanHud.setEnabled(true));

			lineup(context, singleplayer, server);
			giant(context, singleplayer, server);
			shrinking(context, singleplayer, server);
			countdown(context, singleplayer, server);
		}
	}

	/** Mobs from the smallest to the biggest health, each with its HP on a name tag. */
	private static void lineup(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server) {
		scene(server, 0, 5);
		// Minecraft stops drawing tiny entities a few blocks away (the limit follows the hitbox size), so the rabbit stands close.
		mob(server, "rabbit", "Rabbit", 3, -1.6, -2.8);
		mob(server, "chicken", "Chicken", 4, -1.4, -4.4);
		mob(server, "pig", "Pig", 10, 0.0, -5.5);
		mob(server, "creeper", "Creeper", 20, 1.8, -7.5);
		mob(server, "enderman", "Enderman", 40, 4.5, -11.0);
		mob(server, "iron_golem", "Iron Golem", 100, 19.0, -22.0);
		shot(context, singleplayer, "gallery-1-lineup");
	}

	/** A Warden at the size limit of the game (x16) next to a player-sized creeper. */
	private static void giant(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server) {
		scene(server, 0, -22);
		mob(server, "warden", "Warden", 500, 0.0, -42.0);
		mob(server, "creeper", "Creeper", 20, -3.5, -8.0);
		shot(context, singleplayer, "gallery-2-giant");
	}

	/** Every hit shrinks the mob: iron golems with less and less health. */
	private static void shrinking(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server) {
		scene(server, 0, 0);
		mob(server, "iron_golem", "Iron Golem", 100, -9.0, -22.0);
		mob(server, "iron_golem", "Iron Golem", 50, 1.0, -16.0);
		mob(server, "iron_golem", "Iron Golem", 20, 4.0, -9.0);
		mob(server, "iron_golem", "Iron Golem", 5, 4.5, -5.0);
		shot(context, singleplayer, "gallery-3-shrinking");
	}

	/** The recording tools: an on-screen countdown with the clean HUD. */
	private static void countdown(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server) {
		scene(server, 0, 5);
		mob(server, "chicken", "Chicken", 4, -1.0, -4.5);
		mob(server, "cow", "Cow", 10, 1.2, -6.0);
		mob(server, "iron_golem", "Iron Golem", 100, 10.0, -28.0);
		// Survival-like first person view: the hand stays, the clean HUD hides the rest.
		server.runCommand("gamemode creative @a");
		context.waitTicks(60);
		server.runCommand("countdown 3 Minecraft, but mobs are as big as their health");
		context.waitTicks(4);
		singleplayer.getClientWorld().waitForChunksRender();
		context.runOnClient(minecraft -> minecraft.gui.getChat().clearMessages(false));
		context.takeScreenshot(TestScreenshotOptions.of("gallery-4-countdown").disableCounterPrefix());
		context.runOnClient(minecraft -> CleanHud.setEnabled(false));
	}

	/** Removes the previous scene and points the player (the camera) north. */
	private static void scene(TestServerContext server, float yawOffset, float pitch) {
		// Into the void far below: no death animation or smoke in the next shot.
		server.runCommand("tp @e[tag=" + TAG + "] 0.5 -300 0.5");
		server.runCommand("time set noon");
		server.runCommand("execute as @a run tp @s 0.5 -60 0.5 " + (180 + yawOffset) + " " + pitch);
	}

	private static void mob(TestServerContext server, String id, String name, int hp, double x, double z) {
		String label = "{text:\"" + name + "  \",extra:[{text:\"❤ " + hp + " HP\",color:\"red\"}]}";
		server.runCommand("summon minecraft:" + id + " " + (0.5 + x) + " -60 " + (0.5 + z)
				+ " {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"" + TAG + "\"],Rotation:[0f,0f],CustomNameVisible:1b,CustomName:" + label + "}");
		server.runCommand("mobhp @e[tag=" + TAG + ",type=minecraft:" + id + ",limit=1,sort=nearest,x=" + (0.5 + x) + ",y=-60,z=" + (0.5 + z)
				+ "] max " + hp);
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String name) {
		// Let the sizes settle (smooth growth) and the chunks render.
		context.waitTicks(60);
		singleplayer.getClientWorld().waitForChunksRender();
		context.runOnClient(minecraft -> minecraft.gui.getChat().clearMessages(false));
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}
}
