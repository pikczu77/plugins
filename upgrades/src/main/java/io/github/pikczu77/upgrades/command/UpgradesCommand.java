package io.github.pikczu77.upgrades.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import io.github.pikczu77.upgrades.ability.Clones;
import io.github.pikczu77.upgrades.ability.PortalGun;
import io.github.pikczu77.upgrades.config.UpgradesConfig;
import io.github.pikczu77.upgrades.upgrade.Bonus;
import io.github.pikczu77.upgrades.upgrade.Unlockable;
import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

/**
 * {@code /upgrades}: list, give and take upgrades, and change the settings.
 */
public final class UpgradesCommand {
	private static final SuggestionProvider<CommandSourceStack> UPGRADES = (context, builder) -> {
		List<String> ids = new ArrayList<>(List.of("all", "main", "bonus"));

		for (Upgrade upgrade : Upgrade.VALUES) {
			ids.add(upgrade.id());
		}

		for (Bonus bonus : Bonus.VALUES) {
			ids.add(bonus.id());
		}

		return SharedSuggestionProvider.suggest(ids, builder);
	};

	private UpgradesCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("upgrades")
				.executes(context -> list(context.getSource(), context.getSource().getPlayerOrException()))
				.then(Commands.literal("list")
						.executes(context -> list(context.getSource(), context.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
								.executes(context -> list(context.getSource(), EntityArgument.getPlayer(context, "player")))))
				.then(Commands.literal("give").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("players", EntityArgument.players())
								.then(Commands.argument("upgrade", StringArgumentType.word()).suggests(UPGRADES)
										.executes(context -> change(context, true)))))
				.then(Commands.literal("take").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("players", EntityArgument.players())
								.then(Commands.argument("upgrade", StringArgumentType.word()).suggests(UPGRADES)
										.executes(context -> change(context, false)))))
				.then(Commands.literal("bonuses")
						.executes(context -> listBonuses(context.getSource(), context.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
								.executes(context -> listBonuses(context.getSource(), EntityArgument.getPlayer(context, "player"))))
						.then(Commands.literal("on").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(context -> apply(context.getSource(), config -> config.bonuses = true,
										"Bonusowe ulepszenia (pozostałe osiągnięcia): WŁ")))
						.then(Commands.literal("off").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(context -> apply(context.getSource(), config -> config.bonuses = false,
										"Bonusowe ulepszenia (pozostałe osiągnięcia): WYŁ — tylko ulepszenia z filmu"))))
				.then(Commands.literal("settings").executes(context -> settings(context.getSource())))
				.then(setting("on", config -> config.enabled = true, "Ulepszenia włączone"))
				.then(setting("off", config -> config.enabled = false, "Ulepszenia wyłączone (osiągnięcia zostają)"))
				.then(Commands.literal("enable").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("upgrade", StringArgumentType.word()).suggests(UPGRADES)
								.executes(context -> toggle(context, true))))
				.then(Commands.literal("disable").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("upgrade", StringArgumentType.word()).suggests(UPGRADES)
								.executes(context -> toggle(context, false))))
				.then(Commands.literal("powers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(setting("select", config -> config.powerMode = UpgradesConfig.PowerMode.SELECT,
								"Jedna moc na przycisk (najnowsza, klawisz V zmienia)"))
						.then(setting("all", config -> config.powerMode = UpgradesConfig.PowerMode.ALL,
								"Wszystkie moce na przycisku odpalają naraz")))
				.then(Commands.literal("titles").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
							boolean value = BoolArgumentType.getBool(context, "value");
							return apply(context.getSource(), config -> config.titles = value, "Napis NOWE ULEPSZENIE: " + onOff(value));
						})))
				.then(Commands.literal("veinminer").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("sneak").then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
							boolean value = BoolArgumentType.getBool(context, "value");
							return apply(context.getSource(), config -> config.veinSneakSingle = value,
									"Kucanie = kopanie pojedynczego bloku: " + onOff(value));
						})))
						.then(Commands.literal("sizes")
								.then(Commands.argument("lv1", IntegerArgumentType.integer(1, 4096))
										.then(Commands.argument("lv2", IntegerArgumentType.integer(1, 4096))
												.then(Commands.argument("lv3", IntegerArgumentType.integer(1, 4096))
														.then(Commands.argument("max", IntegerArgumentType.integer(1, 4096)).executes(context -> {
															int[] sizes = {IntegerArgumentType.getInteger(context, "lv1"), IntegerArgumentType.getInteger(context, "lv2"),
																	IntegerArgumentType.getInteger(context, "lv3"), IntegerArgumentType.getInteger(context, "max")};
															return apply(context.getSource(), config -> config.veinSizes = sizes,
																	"Vein Miner: " + sizes[0] + " / " + sizes[1] + " / " + sizes[2] + " / " + sizes[3] + " bloków");
														})))))))
				.then(Commands.literal("drops").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("min", IntegerArgumentType.integer(1, 64))
								.then(Commands.argument("max", IntegerArgumentType.integer(1, 64)).executes(context -> {
									int min = IntegerArgumentType.getInteger(context, "min");
									int max = Math.max(min, IntegerArgumentType.getInteger(context, "max"));
									return apply(context.getSource(), config -> {
										config.dropsMin = min;
										config.dropsMax = max;
									}, "Zaklęte dropy: ×" + min + "–×" + max);
								}))))
				.then(Commands.literal("clones").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("clear").executes(context -> clearClones(context.getSource(), null))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(context -> clearClones(context.getSource(), EntityArgument.getPlayer(context, "player").getUUID()))))
						.then(Commands.literal("max").then(Commands.argument("count", IntegerArgumentType.integer(0, 1000)).executes(context -> {
							int count = IntegerArgumentType.getInteger(context, "count");
							return apply(context.getSource(), config -> config.maxClones = count, "Maks. klonów bojowych na gracza: " + count);
						})))
						.then(Commands.literal("multiplicity").then(Commands.argument("count", IntegerArgumentType.integer(0, 5000)).executes(context -> {
							int count = IntegerArgumentType.getInteger(context, "count");
							return apply(context.getSource(), config -> config.multiplicityLimit = count,
									"Maks. klonów z Następnego Pokolenia na gracza: " + count);
						}))))
				.then(Commands.literal("portals").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("clear").executes(context -> {
							int removed = PortalGun.clear(context.getSource().getServer(), null);
							return ok(context.getSource(), "Usunięto portale: " + removed);
						})))
				.then(Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(context -> {
							UpgradesConfig config = UpgradesConfig.get();
							UpgradesConfig defaults = new UpgradesConfig();
							config.enabled = defaults.enabled;
							config.bonuses = defaults.bonuses;
							config.disabled = defaults.disabled;
							config.powerMode = defaults.powerMode;
							config.titles = defaults.titles;
							config.veinSizes = defaults.veinSizes;
							config.veinSneakSingle = defaults.veinSneakSingle;
							config.maxClones = defaults.maxClones;
							config.multiplicityLimit = defaults.multiplicityLimit;
							config.dropsMin = defaults.dropsMin;
							config.dropsMax = defaults.dropsMax;
							return apply(context.getSource(), c -> {
							}, "Przywrócono domyślne ustawienia");
						}));

		dispatcher.register(root);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> setting(String name, Consumer<UpgradesConfig> change, String message) {
		return Commands.literal(name).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(context -> apply(context.getSource(), change, message));
	}

	private static int apply(CommandSourceStack source, Consumer<UpgradesConfig> change, String message) {
		change.accept(UpgradesConfig.get());
		UpgradesConfig.save();

		for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
			UpgradeManager.refresh(player);
			UpgradeManager.sync(player);
		}

		return ok(source, message);
	}

	private static int list(CommandSourceStack source, ServerPlayer player) {
		long mask = UpgradeManager.mask(player);
		int count = Long.bitCount(mask);
		source.sendSuccess(() -> prefix().append(Component.literal("Ulepszenia gracza " + player.getScoreboardName() + ": " + count + "/" + Upgrade.VALUES.size())
				.withStyle(ChatFormatting.GRAY)), false);

		for (Upgrade upgrade : Upgrade.VALUES) {
			boolean has = Upgrade.has(mask, upgrade);
			boolean disabled = !UpgradesConfig.get().isEnabled(upgrade);
			MutableComponent line = Component.literal(has ? " ✔ " : " ✘ ").withStyle(has ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
					.append(Component.literal(upgrade.displayName).withStyle(has ? UpgradeManager.style(upgrade) : net.minecraft.network.chat.Style.EMPTY
							.withColor(ChatFormatting.GRAY)))
					.append(Component.literal("  (" + upgrade.id() + (disabled ? ", wyłączone" : "") + ")").withStyle(ChatFormatting.DARK_GRAY));
			source.sendSuccess(() -> line, false);
		}

		int bonuses = Bonus.count(UpgradeManager.bonuses(player));
		source.sendSuccess(() -> Component.literal(" + bonusy za pozostałe osiągnięcia: " + bonuses + "/" + Bonus.VALUES.size()
				+ " (/upgrades bonuses)").withStyle(ChatFormatting.DARK_AQUA), false);
		return count;
	}

	private static int listBonuses(CommandSourceStack source, ServerPlayer player) {
		long[] bits = UpgradeManager.bonuses(player);
		int count = Bonus.count(bits);
		source.sendSuccess(() -> prefix().append(Component.literal("Bonusy gracza " + player.getScoreboardName() + ": " + count + "/"
				+ Bonus.VALUES.size()).withStyle(ChatFormatting.GRAY)), false);
		MutableComponent line = Component.empty();
		int inLine = 0;

		for (Bonus bonus : Bonus.VALUES) {
			boolean has = Bonus.has(bits, bonus);
			MutableComponent name = Component.literal(bonus.displayName())
					.withStyle(has ? UpgradeManager.style(bonus).withBold(false) : net.minecraft.network.chat.Style.EMPTY.withColor(ChatFormatting.DARK_GRAY));

			if (has) {
				name.withStyle(style -> style.withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
						UpgradeManager.line(bonus, bonus.lines().getFirst()))));
			}

			line.append(inLine == 0 ? Component.literal(" ") : Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY)).append(name);

			if (++inLine == 6) {
				MutableComponent full = line;
				source.sendSuccess(() -> full, false);
				line = Component.empty();
				inLine = 0;
			}
		}

		if (inLine > 0) {
			MutableComponent full = line;
			source.sendSuccess(() -> full, false);
		}

		return count;
	}

	private static int change(CommandContext<CommandSourceStack> context, boolean give) throws CommandSyntaxException {
		Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
		List<? extends Unlockable> upgrades = parse(context);

		if (upgrades.isEmpty()) {
			return fail(context.getSource(), "Nie ma takiego ulepszenia: " + StringArgumentType.getString(context, "upgrade"));
		}

		int changed = 0;

		for (ServerPlayer player : players) {
			for (Unlockable upgrade : upgrades) {
				if (UpgradeManager.setAdvancement(player, upgrade, give)) {
					changed++;
				}
			}

			UpgradeManager.refresh(player);
		}

		return ok(context.getSource(), (give ? "Nadano" : "Zabrano") + " ulepszeń: " + changed
				+ " (przez osiągnięcia, więc działa też /advancement)");
	}

	private static int toggle(CommandContext<CommandSourceStack> context, boolean enable) {
		List<? extends Unlockable> upgrades = parse(context);

		if (upgrades.isEmpty()) {
			return fail(context.getSource(), "Nie ma takiego ulepszenia: " + StringArgumentType.getString(context, "upgrade"));
		}

		return apply(context.getSource(), config -> {
			for (Unlockable upgrade : upgrades) {
				if (enable) {
					config.disabled.remove(upgrade.id());
				} else {
					config.disabled.add(upgrade.id());
				}
			}
		}, (enable ? "Włączono: " : "Wyłączono: ") + (upgrades.size() == 1 ? upgrades.getFirst().displayName() : upgrades.size() + " ulepszeń"));
	}

	private static List<? extends Unlockable> parse(CommandContext<CommandSourceStack> context) {
		String id = StringArgumentType.getString(context, "upgrade");

		switch (id) {
			case "all" -> {
				List<Unlockable> all = new ArrayList<>(Upgrade.VALUES);
				all.addAll(Bonus.VALUES);
				return all;
			}
			case "main" -> {
				return Upgrade.VALUES;
			}
			case "bonus" -> {
				return Bonus.VALUES;
			}
			default -> {
			}
		}

		Upgrade upgrade = Upgrade.byId(id);

		if (upgrade != null) {
			return List.of(upgrade);
		}

		Bonus bonus = Bonus.byId(id);
		return bonus == null ? List.of() : List.of(bonus);
	}

	private static int settings(CommandSourceStack source) {
		UpgradesConfig config = UpgradesConfig.get();
		ok(source, "Ulepszenia: " + onOff(config.enabled) + ", bonusy: " + onOff(config.bonuses) + ", moce: " + config.powerMode.description);
		ok(source, "Vein Miner: " + config.veinSizes[0] + " / " + config.veinSizes[1] + " / " + config.veinSizes[2] + " / "
				+ config.veinSizes[3] + " bloków, kucanie = 1 blok: " + onOff(config.veinSneakSingle));
		ok(source, "Klony: maks. " + config.maxClones + ", Następne Pokolenie: maks. " + config.multiplicityLimit
				+ ", dropy ×" + config.dropsMin + "–×" + config.dropsMax);
		return ok(source, "Wyłączone ulepszenia: " + (config.disabled.isEmpty() ? "brak" : String.join(", ", config.disabled)));
	}

	private static int clearClones(CommandSourceStack source, UUID owner) {
		int removed = Clones.clear(source.getServer(), owner);
		return ok(source, "Usunięto klonów: " + removed);
	}

	private static MutableComponent prefix() {
		return Component.literal("[Ulepszenia] ").withStyle(ChatFormatting.GREEN);
	}

	private static int ok(CommandSourceStack source, String text) {
		source.sendSuccess(() -> prefix().append(Component.literal(text).withStyle(ChatFormatting.GRAY)), false);
		return 1;
	}

	private static int fail(CommandSourceStack source, String text) {
		source.sendFailure(Component.literal(text));
		return 0;
	}

	private static String onOff(boolean value) {
		return value ? "WŁ" : "WYŁ";
	}
}
