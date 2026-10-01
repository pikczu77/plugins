package io.github.pikczu77.blockopener.gametest;

import io.github.pikczu77.blockopener.ability.IceWand;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.zombie.Zombie;

/**
 * Starts a real client, stages the typical recording shots with the mod's commands and saves
 * screenshots to {@code build/run/clientGameTest/screenshots}. Run with {@code ./gradlew runClientGameTest}.
 */
public class BlockOpenerClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		// Keep it light: CI machines render in software.
		context.runOnClient(client -> {
			client.options.renderDistance().set(4);
			client.options.simulationDistance().set(5);
			client.options.menuBackgroundBlurriness().set(0);
		});
		context.getInput().resizeWindow(960, 540);
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getClientWorld().waitForChunksRender();
			var server = singleplayer.getServer();
			server.runCommand("gamerule doDaylightCycle false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 15");
			context.waitTicks(20);

			// 1. Showcase of the 10 items, like the intro of the video.
			server.runCommand("execute as @a at @s run bo showcase 600");
			context.waitTicks(30);
			context.takeScreenshot("blockopener_showcase");
			server.runCommand("bo showcase clear");

			// 2. The row of secret blocks and an opening in progress.
			server.runCommand("execute as @a at @s run bo testrow");
			context.waitTicks(10);
			context.takeScreenshot("blockopener_testrow");
			server.runCommand("execute as @a at @s run setblock ~-2 ~ ~2 minecraft:dirt");
			context.waitTicks(5);
			server.runCommand("execute as @a at @s run bo open ~-2 ~ ~2");
			context.waitTicks(4);
			context.takeScreenshot("blockopener_opening_dirt");
			context.waitTicks(20);
			server.runCommand("execute as @a at @s run bo open ~ ~ ~3");
			context.waitTicks(8);
			context.takeScreenshot("blockopener_opening_lid");
			context.waitTicks(10);
			context.takeScreenshot("blockopener_opening_reveal");
			context.waitTicks(30);
			context.takeScreenshot("blockopener_found_title");

			// 3. Tracker HUD with a few items found.
			server.runCommand("bo reveal @p bee_drill");
			server.runCommand("bo reveal @p mossphere");
			context.waitTicks(80);
			context.takeScreenshot("blockopener_tracker");

			// 4. Tooltip with the powers.
			server.runCommand("clear @a");
			server.runCommand("bo give @a all");
			context.waitTicks(5);
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(5);
			// Hover the 5th hotbar slot (Diamond Leggings) of the 176x166 inventory.
			double[] cursor = context.computeOnClient(client -> {
				var window = client.getWindow();
				double scale = window.getGuiScale();
				double left = (window.getGuiScaledWidth() - 176) / 2.0;
				double top = (window.getGuiScaledHeight() - 166) / 2.0;
				return new double[] {(left + 8 + 4 * 18 + 8) * scale, (top + 142 + 8) * scale};
			});
			context.getInput().setCursorPos(cursor[0], cursor[1]);
			context.waitTicks(3);
			context.takeScreenshot("blockopener_tooltip");
			context.getInput().holdShift();
			context.waitTicks(3);
			context.takeScreenshot("blockopener_tooltip_shift");
			context.getInput().releaseShift();
			context.setScreen(() -> null);

			// 5. Extended mode: the 12 extra items, a second tracker row and a frozen zombie.
			server.runCommand("bo settings extendedItems true");
			server.runCommand("bo reveal @p storm_hammer");
			server.runCommand("bo reveal @p ice_wand");
			server.runCommand("bo reveal @p cake_of_life");
			context.waitTicks(90);
			context.takeScreenshot("blockopener_extended_tracker");
			server.runCommand("execute as @a at @s run bo showcase 600");
			context.waitTicks(30);
			context.takeScreenshot("blockopener_extended_showcase");
			server.runCommand("bo showcase clear");
			server.runOnServer(minecraft -> {
				ServerPlayer player = minecraft.getPlayerList().getPlayers().getFirst();
				Zombie zombie = new Zombie(player.level());
				zombie.snapTo(player.getX() + 1.0, player.getY(), player.getZ() + 6.0, 180.0F, 0.0F);
				zombie.setNoAi(true);
				player.level().addFreshEntity(zombie);
				IceWand.freeze(player, zombie);
			});
			context.waitTicks(10);
			context.takeScreenshot("blockopener_ice_wand");
			server.runCommand("give @a blockopener:mob_cage[minecraft:custom_data={blockopener_mob:{id:\"minecraft:zombie\"}}]");
			context.waitTicks(5);
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitTicks(5);
			context.takeScreenshot("blockopener_extended_inventory");
			context.setScreen(() -> null);

			// 6. The custom armor on the player (third person).
			server.runCommand("item replace entity @a armor.head with blockopener:sculk_helmet");
			server.runCommand("item replace entity @a armor.chest with blockopener:anvil_chestplate");
			server.runCommand("item replace entity @a armor.legs with blockopener:diamond_leggings");
			server.runCommand("item replace entity @a armor.feet with blockopener:pumpkin_boots");
			server.runCommand("item replace entity @a weapon.mainhand with blockopener:dripstone_sword");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(10);
			context.takeScreenshot("blockopener_armor");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

			// 7. Void water.
			server.runCommand("execute as @a at @s run setblock ~ ~-1 ~2 blockopener:void_water");
			context.waitTicks(40);
			context.takeScreenshot("blockopener_void_water");
		}
	}
}
