package io.github.pikczu77.reckit.test;

import java.util.List;
import java.util.Map;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;

import io.github.pikczu77.reckit.PropItems;
import io.github.pikczu77.reckit.Reckit;

/**
 * Starts a real client and takes screenshots of every pack's creative tab and of every prop held in first and third
 * person. Screenshots land in the run directory (build/run/clientGameTest/screenshots).
 */
@SuppressWarnings("UnstableApiUsage")
public class ReckitClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			context.getInput().resizeWindow(1280, 720);
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamemode creative @a");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule spawn_monsters false");
			server.runCommand("fill -6 200 -6 6 200 6 minecraft:smooth_stone");
			server.runCommand("tp @a 0.5 201 0.5 0 0");
			singleplayer.getClientWorld().waitForChunksRender();
			context.runOnClient(minecraft -> minecraft.gui.getChat().clearMessages(false));

			for (Map.Entry<String, List<Item>> pack : PropItems.byPack().entrySet()) {
				creativeTab(context, pack.getKey());

				for (Item item : pack.getValue()) {
					held(context, server, pack.getKey(), BuiltInRegistries.ITEM.getKey(item).getPath());
				}
			}
		}
	}

	private static void creativeTab(ClientGameTestContext context, String pack) {
		context.setScreen(() -> {
			LocalPlayer player = Minecraft.getInstance().player;
			return new CreativeModeInventoryScreen(player, player.connection.enabledFeatures(), true);
		});
		context.runOnClient(minecraft -> {
			FabricCreativeInventoryScreen screen = (FabricCreativeInventoryScreen) minecraft.screen;
			screen.setSelectedItemGroup(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Reckit.id(pack)));
		});
		context.waitTicks(5);
		context.takeScreenshot("reckit-" + pack + "-00-tab");
		context.setScreen(() -> null);
	}

	private static void held(ClientGameTestContext context, TestServerContext server, String pack, String id) {
		server.runCommand("item replace entity @a weapon.mainhand with reckit:" + id);
		context.runOnClient(minecraft -> {
			minecraft.options.hideGui = false;
			minecraft.options.setCameraType(CameraType.FIRST_PERSON);
		});
		context.waitTicks(10);
		context.takeScreenshot("reckit-" + pack + "-" + id + "-1st");
		context.runOnClient(minecraft -> {
			minecraft.options.hideGui = true;
			minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
		});
		context.waitTicks(3);
		context.takeScreenshot("reckit-" + pack + "-" + id + "-3rd");
	}
}
