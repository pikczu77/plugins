package io.github.pikczu77.hpsize.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.CommonComponents;
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
 * Optional client part: the clean HUD toggle (the server side works without it).
 */
public class HpSizeClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(HpSize.id("main"));
	public static final KeyMapping CLEAN_HUD = new KeyMapping("key.hpsize.clean_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

	@Override
	public void onInitializeClient() {
		ClientConfig.load();
		KeyBindingHelper.registerKeyBinding(CLEAN_HUD);
		CleanHud.register();

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (CLEAN_HUD.consumeClick()) {
				toggleCleanHud(minecraft);
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> registerCommands(dispatcher));
	}

	private static void toggleCleanHud(Minecraft minecraft) {
		CleanHud.setEnabled(!CleanHud.isEnabled());

		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(Component.translatable("hpsize.hud.clean", CommonComponents.optionStatus(CleanHud.isEnabled()))
					.withStyle(ChatFormatting.GOLD), true);
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
					return feedback(context.getSource(), Component.translatable("hpsize.hud.clean", CommonComponents.OPTION_ON));
				}))
				.then(ClientCommandManager.literal("off").executes(context -> {
					CleanHud.setEnabled(false);
					return feedback(context.getSource(), Component.translatable("hpsize.hud.clean", CommonComponents.OPTION_OFF));
				}))
				.then(ClientCommandManager.literal("list").executes(context -> {
					for (String name : CleanHud.names()) {
						boolean hidden = ClientConfig.get().hiddenHud.contains(name);
						context.getSource().sendFeedback(Component.literal(" " + name + ": ").withStyle(ChatFormatting.GRAY)
								.append(Component.translatable(hidden ? "hpsize.hud.hidden" : "hpsize.hud.visible")
										.withStyle(hidden ? ChatFormatting.RED : ChatFormatting.GREEN)));
					}

					return 1;
				}))
				.then(ClientCommandManager.literal("hide").then(ClientCommandManager.argument("element", StringArgumentType.word())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(CleanHud.names(), builder))
						.executes(context -> setHidden(context.getSource(), StringArgumentType.getString(context, "element"), true))))
				.then(ClientCommandManager.literal("show").then(ClientCommandManager.argument("element", StringArgumentType.word())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(CleanHud.names(), builder))
						.executes(context -> setHidden(context.getSource(), StringArgumentType.getString(context, "element"), false)))));
	}

	private static int setHidden(FabricClientCommandSource source, String element, boolean hidden) {
		if (!CleanHud.names().contains(element)) {
			source.sendError(Component.translatable("hpsize.hud.unknown"));
			return 0;
		}

		ClientConfig config = ClientConfig.get();
		config.hiddenHud.remove(element);

		if (hidden) {
			config.hiddenHud.add(element);
		}

		config.save();
		return feedback(source, Component.translatable(hidden ? "hpsize.hud.will_hide" : "hpsize.hud.will_show", element));
	}

	private static int feedback(FabricClientCommandSource source, Component text) {
		source.sendFeedback(Msg.prefix().append(text.copy().withStyle(ChatFormatting.GRAY)));
		return 1;
	}
}
