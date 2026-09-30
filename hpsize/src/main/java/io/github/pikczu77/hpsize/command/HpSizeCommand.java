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
import io.github.pikczu77.hpsize.util.LookTarget;
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
				"Mechanic ON: mobs are as big as their health.", "Mechanika WŁ: moby są tak duże, jak dużo mają życia.")));
		root.then(Commands.literal("off").executes(context -> change(context.getSource(), config -> config.enabled = false,
				"Mechanic OFF: all mobs go back to their normal size.", "Mechanika WYŁ: wszystkie moby wracają do normalnego rozmiaru.")));

		var mode = Commands.literal("mode");

		for (HpSizeConfig.Mode value : HpSizeConfig.Mode.values()) {
			mode.then(Commands.literal(value.id).executes(context -> change(context.getSource(), config -> config.mode = value,
					"Mode: " + value.id + " (" + value.english + ").", "Tryb: " + value.id + " (" + value.polish + ").")));
		}

		root.then(mode);

		root.then(Commands.literal("preset").then(Commands.argument("preset", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(HpSizeConfig.presets(), builder))
				.executes(context -> {
					String preset = StringArgumentType.getString(context, "preset");

					if (!HpSizeConfig.presets().contains(preset)) {
						return Msg.fail(context.getSource(), "Unknown preset. Available: " + String.join(", ", HpSizeConfig.presets()),
								"Nieznany preset. Dostępne: " + String.join(", ", HpSizeConfig.presets()));
					}

					return change(context.getSource(), config -> config.applyPreset(preset),
							"Preset " + preset + " set. " + presetDescription(preset, false), "Preset " + preset + " ustawiony. " + presetDescription(preset, true));
				})));

		root.then(Commands.literal("normal").then(Commands.argument("hp", DoubleArgumentType.doubleArg(1, 1000))
				.executes(context -> {
					double value = DoubleArgumentType.getDouble(context, "hp");
					String examples = " (chicken ×" + Msg.number(4 / value) + ", iron golem ×" + Msg.number(100 / value) + ").";
					return change(context.getSource(), config -> config.normalHp = value, "A mob with " + Msg.number(value) + " HP has its normal size" + examples,
							"Normalny rozmiar ma mob z " + Msg.number(value) + " HP" + examples.replace("chicken", "kurczak").replace("iron golem", "żelazny golem"));
				})));
		root.then(Commands.literal("min").then(Commands.argument("value", DoubleArgumentType.doubleArg(HpSizeConfig.SCALE_MIN, HpSizeConfig.SCALE_MAX))
				.executes(context -> {
					double value = DoubleArgumentType.getDouble(context, "value");
					return change(context.getSource(), config -> config.minScale = value, "Minimum size: ×" + Msg.number(value) + ".",
							"Minimalny rozmiar: ×" + Msg.number(value) + ".");
				})));
		root.then(Commands.literal("max").then(Commands.argument("value", DoubleArgumentType.doubleArg(HpSizeConfig.SCALE_MIN, HpSizeConfig.SCALE_MAX))
				.executes(context -> {
					double value = DoubleArgumentType.getDouble(context, "value");
					return change(context.getSource(), config -> config.maxScale = value, "Maximum size: ×" + Msg.number(value) + ".",
							"Maksymalny rozmiar: ×" + Msg.number(value) + ".");
				})));
		root.then(Commands.literal("smooth").then(Commands.argument("ticks", IntegerArgumentType.integer(0, 100))
				.executes(context -> {
					int value = IntegerArgumentType.getInteger(context, "ticks");
					return change(context.getSource(), config -> config.smooth = value,
							value == 0 ? "The size changes instantly (like in the video)." : "Size smoothing: " + value + ".",
							value == 0 ? "Rozmiar zmienia się natychmiast (jak w filmie)." : "Płynność zmian rozmiaru: " + value + ".");
				})));

		root.then(toggle("players", "Players change size too", "Gracze też zmieniają rozmiar", (config, value) -> config.players = value));
		root.then(toggle("suffocation", "Mobs suffocate in blocks (and shrink in caves)", "Duszenie się mobów w blokach (przez nie maleją w jaskiniach)",
				(config, value) -> config.suffocation = value));
		root.then(toggle("sound", "Deflating sound when a hit shrinks a mob", "Dźwięk „sflaczenia”, gdy mob maleje od ciosu",
				(config, value) -> config.shrinkSound = value));
		root.then(toggle("glowtiny", "Tiny mobs glow", "Podświetlanie malutkich mobów", (config, value) -> config.glowTiny = value)
				.then(Commands.literal("below").then(Commands.argument("scale", DoubleArgumentType.doubleArg(HpSizeConfig.SCALE_MIN, HpSizeConfig.SCALE_MAX))
						.executes(context -> {
							double value = DoubleArgumentType.getDouble(context, "scale");
							return change(context.getSource(), config -> {
								config.glowTiny = true;
								config.glowTinyBelow = value;
							}, "Mobs smaller than ×" + Msg.number(value) + " glow.", "Podświetlanie mobów mniejszych niż ×" + Msg.number(value) + ".");
						}))));

		root.then(Commands.literal("freeze").executes(context -> change(context.getSource(), config -> config.frozen = true,
				"Sizes frozen - they will not change until you type /hpsize unfreeze.",
				"Rozmiary zamrożone - nie zmienią się, dopóki nie wpiszesz /hpsize unfreeze.")));
		root.then(Commands.literal("unfreeze").executes(context -> change(context.getSource(), config -> config.frozen = false,
				"Sizes follow the health again.", "Rozmiary znowu podążają za życiem.")));

		root.then(Commands.literal("exclude").then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
				.executes(context -> {
					String id = entityId(ResourceArgument.getEntityType(context, "entity"));
					return change(context.getSource(), config -> {
						if (!config.excluded.contains(id)) {
							config.excluded.add(id);
						}
					}, id + " keeps its normal size.", id + " zachowa normalny rozmiar.");
				})));
		root.then(Commands.literal("include").then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
				.executes(context -> {
					String id = entityId(ResourceArgument.getEntityType(context, "entity"));
					return change(context.getSource(), config -> config.excluded.remove(id), id + " changes size again.", id + " znowu zmienia rozmiar.");
				})));

		root.then(Commands.literal("info")
				.executes(context -> {
					LivingEntity target = LookTarget.find(context.getSource().getPlayerOrException(), 64);

					if (target == null) {
						return Msg.fail(context.getSource(), "You are not looking at a mob.", "Nie patrzysz na żadnego moba.");
					}

					return info(context.getSource(), List.of(target));
				})
				.then(Commands.argument("targets", EntityArgument.entities())
						.executes(context -> info(context.getSource(), EntityArgument.getEntities(context, "targets")))));

		root.then(Commands.literal("reset").executes(context -> change(context.getSource(), config -> {
			HpSizeConfig defaults = new HpSizeConfig();
			config.enabled = defaults.enabled;
			config.mode = defaults.mode;
			config.normalHp = defaults.normalHp;
			config.minScale = defaults.minScale;
			config.maxScale = defaults.maxScale;
			config.smooth = defaults.smooth;
			config.players = defaults.players;
			config.suffocation = defaults.suffocation;
			config.frozen = defaults.frozen;
			config.glowTiny = defaults.glowTiny;
			config.glowTinyBelow = defaults.glowTinyBelow;
			config.shrinkSound = defaults.shrinkSound;
			config.excluded = defaults.excluded;
		}, "Default settings restored.", "Przywrócono ustawienia domyślne.")));

		dispatcher.register(root);
	}

	private interface Setter {
		void set(HpSizeConfig config, boolean value);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, String english, String polish, Setter setter) {
		return Commands.literal(name)
				.then(Commands.literal("on").executes(context -> change(context.getSource(), config -> setter.set(config, true),
						english + ": ON.", polish + ": WŁ.")))
				.then(Commands.literal("off").executes(context -> change(context.getSource(), config -> setter.set(config, false),
						english + ": OFF.", polish + ": WYŁ.")));
	}

	private static int change(CommandSourceStack source, Consumer<HpSizeConfig> change, String english, String polish) {
		HpSizeConfig config = HpSizeConfig.get();

		if (config == null) {
			return Msg.fail(source, "Settings are not loaded yet.", "Ustawienia nie są jeszcze wczytane.");
		}

		change.accept(config);
		config.save();
		return Msg.ok(source, english, polish);
	}

	private static String entityId(Holder.Reference<EntityType<?>> type) {
		return type.key().identifier().toString();
	}

	private static String presetDescription(String preset, boolean polish) {
		return switch (preset) {
			case "film" -> polish ? "Dokładnie jak w filmie: rozmiar według HP (20 HP = normalny), zmiana natychmiastowa."
					: "Exactly like in the video: size follows the HP (20 HP = normal), changes instantly.";
			case "smooth" -> polish ? "Jak w filmie, ale moby płynnie maleją." : "Like in the video, but mobs shrink smoothly.";
			case "fair" -> polish ? "Każdy cios zmniejsza moba - także bossów z ogromnym HP." : "Every hit shrinks the mob - bosses with huge HP too.";
			case "light" -> polish ? "Mniejsi giganci (maks. ×6) - mniej lagów." : "Smaller giants (max ×6) - less lag.";
			default -> "";
		};
	}

	private static int status(CommandSourceStack source) {
		HpSizeConfig config = HpSizeConfig.get();

		if (config == null) {
			return Msg.fail(source, "Settings are not loaded yet.", "Ustawienia nie są jeszcze wczytane.");
		}

		boolean pl = Msg.polish(source);
		source.sendSuccess(() -> Msg.prefix().append(Component.literal(pl ? "Moby są tak duże, jak dużo mają życia" : "Mobs are as big as their health")
				.withStyle(ChatFormatting.WHITE)), false);
		line(source, pl ? "Mechanika" : "Mechanic", Msg.onOff(source, config.enabled)
				+ (config.frozen ? (pl ? " (rozmiary zamrożone)" : " (sizes frozen)") : ""));
		line(source, pl ? "Tryb" : "Mode", config.mode.id + " - " + (pl ? config.mode.polish : config.mode.english));
		line(source, pl ? "Normalny rozmiar przy" : "Normal size at", Msg.number(config.normalHp) + " HP ("
				+ (pl ? "kurczak" : "chicken") + " ×" + Msg.number(4 / config.normalHp) + ")");
		line(source, pl ? "Rozmiar" : "Size", (pl ? "od ×" : "from ×") + Msg.number(config.minScale) + (pl ? " do ×" : " to ×") + Msg.number(config.maxScale));
		line(source, pl ? "Płynność" : "Smoothing", config.smooth == 0 ? (pl ? "natychmiast" : "instant") : String.valueOf(config.smooth));
		line(source, pl ? "Gracze" : "Players", Msg.onOff(source, config.players));
		line(source, pl ? "Duszenie w blokach" : "Suffocation in blocks", Msg.onOff(source, config.suffocation));
		line(source, pl ? "Dźwięk malenia" : "Shrink sound", Msg.onOff(source, config.shrinkSound));
		line(source, pl ? "Podświetlanie małych" : "Tiny mobs glow", Msg.onOff(source, config.glowTiny)
				+ (pl ? " (poniżej ×" : " (below ×") + Msg.number(config.glowTinyBelow) + ")");
		line(source, pl ? "Wykluczone" : "Excluded", config.excluded.isEmpty() ? "-" : String.join(", ", config.excluded));
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

			String target = config != null && config.enabled && config.affects(living) ? "×" + Msg.number(config.targetScale(living))
					: Msg.tr(source, "unchanged", "bez zmian");
			String size = Msg.tr(source, ", size ×", ", rozmiar ×") + Msg.number(living.getScale()) + Msg.tr(source, " (target ", " (docelowo ") + target + ")";
			source.sendSuccess(() -> Component.empty().append(living.getDisplayName()).append(Component.literal(
					": ❤ " + Msg.number(living.getHealth()) + "/" + Msg.number(living.getMaxHealth()) + size).withStyle(ChatFormatting.GRAY)), false);
			shown++;
		}

		return shown == 0 ? Msg.fail(source, "No living mobs.", "Brak żywych mobów.") : shown;
	}
}
