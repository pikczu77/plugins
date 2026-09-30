package io.github.pikczu77.hpsize.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import io.github.pikczu77.hpsize.HpSize;
import io.github.pikczu77.hpsize.util.Msg;

/**
 * Optional client part: clean HUD toggle and zoom (the server side works without it).
 */
public class HpSizeClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(HpSize.id("main"));
	public static final KeyMapping CLEAN_HUD = new KeyMapping("key.hpsize.clean_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
	public static final KeyMapping ZOOM = new KeyMapping("key.hpsize.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);

	@Override
	public void onInitializeClient() {
		ClientConfig.load();
		KeyBindingHelper.registerKeyBinding(CLEAN_HUD);
		KeyBindingHelper.registerKeyBinding(ZOOM);
		CleanHud.register();

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (CLEAN_HUD.consumeClick()) {
				toggleCleanHud(minecraft);
			}

			Zoom.setActive(minecraft, minecraft.player != null && minecraft.screen == null && ZOOM.isDown());
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> registerCommands(dispatcher));
	}

	private static void toggleCleanHud(Minecraft minecraft) {
		CleanHud.setEnabled(!CleanHud.isEnabled());

		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(Component.literal("Czysty HUD: " + Msg.onOff(CleanHud.isEnabled())).withStyle(ChatFormatting.GOLD), true);
		}
	}

	private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommandManager.literal("hud")
				.executes(context -> {
					toggleCleanHud(context.getSource().getClient());
					return 1;
				})
				.then(ClientCommandManager.literal("on").executes(context -> {
					CleanHud.setEnabled(true);
					return feedback(context.getSource(), "Czysty HUD: WŁ.");
				}))
				.then(ClientCommandManager.literal("off").executes(context -> {
					CleanHud.setEnabled(false);
					return feedback(context.getSource(), "Czysty HUD: WYŁ.");
				}))
				.then(ClientCommandManager.literal("list").executes(context -> {
					for (String name : CleanHud.names()) {
						boolean hidden = ClientConfig.get().hiddenHud.contains(name);
						context.getSource().sendFeedback(Component.literal(" " + name + ": ").withStyle(ChatFormatting.GRAY)
								.append(Component.literal(hidden ? "ukryty" : "widoczny").withStyle(hidden ? ChatFormatting.RED : ChatFormatting.GREEN)));
					}

					return 1;
				}))
				.then(ClientCommandManager.literal("hide").then(ClientCommandManager.argument("element", StringArgumentType.word())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(CleanHud.names(), builder))
						.executes(context -> setHidden(context.getSource(), StringArgumentType.getString(context, "element"), true))))
				.then(ClientCommandManager.literal("show").then(ClientCommandManager.argument("element", StringArgumentType.word())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(CleanHud.names(), builder))
						.executes(context -> setHidden(context.getSource(), StringArgumentType.getString(context, "element"), false)))));

		dispatcher.register(ClientCommandManager.literal("zoom")
				.executes(context -> feedback(context.getSource(), "Zoom: ×" + Msg.number(ClientConfig.get().zoomFactor)
						+ ". Przytrzymaj klawisz zoomu (domyślnie Z), kółkiem myszy zmieniasz przybliżenie."))
				.then(ClientCommandManager.argument("factor", DoubleArgumentType.doubleArg(1.1, 50.0)).executes(context -> {
					ClientConfig.get().zoomFactor = DoubleArgumentType.getDouble(context, "factor");
					ClientConfig.get().save();
					return feedback(context.getSource(), "Domyślny zoom: ×" + Msg.number(ClientConfig.get().zoomFactor) + ".");
				})));
	}

	private static int setHidden(FabricClientCommandSource source, String element, boolean hidden) {
		if (!CleanHud.names().contains(element)) {
			source.sendError(Component.literal("Nieznany element HUD. Lista: /hud list"));
			return 0;
		}

		ClientConfig config = ClientConfig.get();
		config.hiddenHud.remove(element);

		if (hidden) {
			config.hiddenHud.add(element);
		}

		config.save();
		return feedback(source, element + (hidden ? " będzie ukryty" : " będzie widoczny") + " w trybie czystego HUD.");
	}

	private static int feedback(FabricClientCommandSource source, String text) {
		source.sendFeedback(Msg.info(text));
		return 1;
	}
}
