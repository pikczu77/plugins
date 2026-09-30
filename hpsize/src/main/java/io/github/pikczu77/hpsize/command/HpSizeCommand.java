package io.github.pikczu77.hpsize.command;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import io.github.pikczu77.hpsize.config.HpSizeConfig;
import io.github.pikczu77.hpsize.scale.HealthBars;
import io.github.pikczu77.hpsize.util.Msg;

/**
 * /hpsize - controls the "mobs are as big as their health" mechanic.
 */
public final class HpSizeCommand {
	private HpSizeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("hpsize")
				.requires(Perms.gamemaster())
				.executes(context -> status(context.getSource()));

		root.then(Commands.literal("on").executes(context -> change(context.getSource(), config -> config.enabled = true,
				"Mechanika WŁ: moby są tak duże, jak dużo mają życia.")));
		root.then(Commands.literal("off").executes(context -> change(context.getSource(), config -> config.enabled = false,
				"Mechanika WYŁ: wszystkie moby wracają do normalnego rozmiaru.")));

		var mode = Commands.literal("mode");

		for (HpSizeConfig.Mode value : HpSizeConfig.Mode.values()) {
			mode.then(Commands.literal(value.id).executes(context -> change(context.getSource(), config -> config.mode = value,
					"Tryb: " + value.id + " (" + value.description + ").")));
		}

		root.then(mode);

		root.then(Commands.literal("preset").then(Commands.argument("preset", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(HpSizeConfig.presets(), builder))
				.executes(context -> {
					String preset = StringArgumentType.getString(context, "preset");

					if (!HpSizeConfig.presets().contains(preset)) {
						return Msg.fail(context.getSource(), "Nieznany preset. Dostępne: " + String.join(", ", HpSizeConfig.presets()));
					}

					return change(context.getSource(), config -> config.applyPreset(preset), "Preset " + preset + " ustawiony. " + presetDescription(preset));
				})));

		root.then(Commands.literal("factor").then(Commands.argument("value", DoubleArgumentType.doubleArg(0.001, 100))
				.executes(context -> {
					double value = DoubleArgumentType.getDouble(context, "value");
					return change(context.getSource(), config -> config.factor = value, "Mnożnik rozmiaru: ×" + Msg.number(value) + ".");
				})));
		root.then(Commands.literal("min").then(Commands.argument("value", DoubleArgumentType.doubleArg(HpSizeConfig.SCALE_MIN, HpSizeConfig.SCALE_MAX))
				.executes(context -> {
					double value = DoubleArgumentType.getDouble(context, "value");
					return change(context.getSource(), config -> config.minScale = value, "Minimalny rozmiar: ×" + Msg.number(value) + ".");
				})));
		root.then(Commands.literal("max").then(Commands.argument("value", DoubleArgumentType.doubleArg(HpSizeConfig.SCALE_MIN, HpSizeConfig.SCALE_MAX))
				.executes(context -> {
					double value = DoubleArgumentType.getDouble(context, "value");
					return change(context.getSource(), config -> config.maxScale = value, "Maksymalny rozmiar: ×" + Msg.number(value) + ".");
				})));
		root.then(Commands.literal("smooth").then(Commands.argument("ticks", IntegerArgumentType.integer(0, 100))
				.executes(context -> {
					int value = IntegerArgumentType.getInteger(context, "ticks");
					return change(context.getSource(), config -> config.smooth = value,
							value == 0 ? "Rozmiar zmienia się natychmiast (jak w filmie)." : "Płynność zmian rozmiaru: " + value + ".");
				})));

		root.then(toggle("players", "Gracze też zmieniają rozmiar", (config, value) -> config.players = value));
		root.then(toggle("suffocation", "Duszenie się mobów w blokach (przez nie maleją w jaskiniach)", (config, value) -> config.suffocation = value));
		root.then(toggle("bar", "Pasek HP i rozmiaru moba, na którego patrzysz", (config, value) -> config.healthBar = value));
		root.then(toggle("sound", "Dźwięk „sflaczenia”, gdy mob maleje od ciosu", (config, value) -> config.shrinkSound = value));
		root.then(toggle("glowtiny", "Podświetlanie malutkich mobów", (config, value) -> config.glowTiny = value)
				.then(Commands.literal("below").then(Commands.argument("scale", DoubleArgumentType.doubleArg(HpSizeConfig.SCALE_MIN, HpSizeConfig.SCALE_MAX))
						.executes(context -> {
							double value = DoubleArgumentType.getDouble(context, "scale");
							return change(context.getSource(), config -> {
								config.glowTiny = true;
								config.glowTinyBelow = value;
							}, "Podświetlanie mobów mniejszych niż ×" + Msg.number(value) + ".");
						}))));

		root.then(Commands.literal("freeze").executes(context -> change(context.getSource(), config -> config.frozen = true,
				"Rozmiary zamrożone - nie zmienią się, dopóki nie wpiszesz /hpsize unfreeze.")));
		root.then(Commands.literal("unfreeze").executes(context -> change(context.getSource(), config -> config.frozen = false,
				"Rozmiary znowu podążają za życiem.")));

		root.then(Commands.literal("exclude").then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
				.executes(context -> {
					String id = entityId(ResourceArgument.getEntityType(context, "entity"));
					return change(context.getSource(), config -> {
						if (!config.excluded.contains(id)) {
							config.excluded.add(id);
						}
					}, id + " zachowa normalny rozmiar.");
				})));
		root.then(Commands.literal("include").then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
				.executes(context -> {
					String id = entityId(ResourceArgument.getEntityType(context, "entity"));
					return change(context.getSource(), config -> config.excluded.remove(id), id + " znowu zmienia rozmiar.");
				})));

		root.then(Commands.literal("info")
				.executes(context -> {
					LivingEntity target = HealthBars.findLookTarget(context.getSource().getPlayerOrException(), 64);

					if (target == null) {
						return Msg.fail(context.getSource(), "Nie patrzysz na żadnego moba.");
					}

					return info(context.getSource(), List.of(target));
				})
				.then(Commands.argument("targets", EntityArgument.entities())
						.executes(context -> info(context.getSource(), EntityArgument.getEntities(context, "targets")))));

		root.then(Commands.literal("reset").executes(context -> change(context.getSource(), config -> {
			HpSizeConfig defaults = new HpSizeConfig();
			config.enabled = defaults.enabled;
			config.mode = defaults.mode;
			config.factor = defaults.factor;
			config.minScale = defaults.minScale;
			config.maxScale = defaults.maxScale;
			config.smooth = defaults.smooth;
			config.players = defaults.players;
			config.suffocation = defaults.suffocation;
			config.frozen = defaults.frozen;
			config.healthBar = defaults.healthBar;
			config.glowTiny = defaults.glowTiny;
			config.glowTinyBelow = defaults.glowTinyBelow;
			config.shrinkSound = defaults.shrinkSound;
			config.excluded = defaults.excluded;
		}, "Przywrócono ustawienia domyślne.")));

		dispatcher.register(root);
	}

	private interface Setter {
		void set(HpSizeConfig config, boolean value);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, String description, Setter setter) {
		return Commands.literal(name)
				.then(Commands.literal("on").executes(context -> change(context.getSource(), config -> setter.set(config, true), description + ": WŁ.")))
				.then(Commands.literal("off").executes(context -> change(context.getSource(), config -> setter.set(config, false), description + ": WYŁ.")));
	}

	private static int change(CommandSourceStack source, Consumer<HpSizeConfig> change, String message) {
		HpSizeConfig config = HpSizeConfig.get();

		if (config == null) {
			return Msg.fail(source, "Ustawienia nie są jeszcze wczytane.");
		}

		change.accept(config);
		config.save();
		return Msg.ok(source, message);
	}

	private static String entityId(Holder.Reference<EntityType<?>> type) {
		return type.key().identifier().toString();
	}

	private static String presetDescription(String preset) {
		return switch (preset) {
			case "film" -> "Dokładnie jak w filmie: rozmiar = HP, zmiana natychmiastowa.";
			case "smooth" -> "Jak w filmie, ale moby płynnie maleją.";
			case "fair" -> "Każdy cios zmniejsza moba - także bossów z ogromnym HP.";
			case "light" -> "Mniejsi giganci (maks. ×6) - mniej lagów.";
			default -> "";
		};
	}

	private static int status(CommandSourceStack source) {
		HpSizeConfig config = HpSizeConfig.get();

		if (config == null) {
			return Msg.fail(source, "Ustawienia nie są jeszcze wczytane.");
		}

		source.sendSuccess(() -> Msg.prefix().append(Component.literal("Moby są tak duże, jak dużo mają życia").withStyle(ChatFormatting.WHITE)), false);
		line(source, "Mechanika", Msg.onOff(config.enabled) + (config.frozen ? " (rozmiary zamrożone)" : ""));
		line(source, "Tryb", config.mode.id + " - " + config.mode.description);
		line(source, "Mnożnik", "×" + Msg.number(config.factor));
		line(source, "Rozmiar", "od ×" + Msg.number(config.minScale) + " do ×" + Msg.number(config.maxScale));
		line(source, "Płynność", config.smooth == 0 ? "natychmiast" : String.valueOf(config.smooth));
		line(source, "Gracze", Msg.onOff(config.players));
		line(source, "Duszenie w blokach", Msg.onOff(config.suffocation));
		line(source, "Pasek HP", Msg.onOff(config.healthBar));
		line(source, "Dźwięk malenia", Msg.onOff(config.shrinkSound));
		line(source, "Podświetlanie małych", Msg.onOff(config.glowTiny) + " (poniżej ×" + Msg.number(config.glowTinyBelow) + ")");
		line(source, "Wykluczone", config.excluded.isEmpty() ? "-" : String.join(", ", config.excluded));
		return 1;
	}

	private static void line(CommandSourceStack source, String name, String value) {
		source.sendSuccess(() -> Component.literal(" " + name + ": ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(value).withStyle(ChatFormatting.YELLOW)), false);
	}

	private static int info(CommandSourceStack source, Collection<? extends Entity> targets) {
		HpSizeConfig config = HpSizeConfig.get();
		int shown = 0;

		for (Entity entity : targets) {
			if (!(entity instanceof LivingEntity living) || shown >= 10) {
				continue;
			}

			String target = config != null && config.enabled && config.affects(living) ? "×" + Msg.number(config.targetScale(living)) : "bez zmian";
			source.sendSuccess(() -> Component.empty().append(living.getDisplayName()).append(Component.literal(
					": ❤ " + Msg.number(living.getHealth()) + "/" + Msg.number(living.getMaxHealth())
							+ ", rozmiar ×" + Msg.number(living.getScale()) + " (docelowo " + target + ")").withStyle(ChatFormatting.GRAY)), false);
			shown++;
		}

		return shown == 0 ? Msg.fail(source, "Brak żywych mobów.") : shown;
	}
}
