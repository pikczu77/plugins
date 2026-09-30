package io.github.pikczu77.hpsize.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.hpsize.rec.CamMode;
import io.github.pikczu77.hpsize.rec.Countdown;
import io.github.pikczu77.hpsize.rec.Freeze;
import io.github.pikczu77.hpsize.rec.RecMode;
import io.github.pikczu77.hpsize.util.Msg;
import io.github.pikczu77.hpsize.util.Screens;

/**
 * General commands that make recording a video easier.
 */
public final class RecCommands {
	private RecCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		registerCountdown(dispatcher);
		registerFreeze(dispatcher);
		registerPlayerHelpers(dispatcher);
		registerRecMode(dispatcher);
		registerCleanup(dispatcher);
		registerAnnounce(dispatcher);
	}

	// /countdown, /go

	private static void registerCountdown(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("countdown")
				.requires(Perms.gamemaster())
				.then(Commands.literal("cancel").executes(context -> Countdown.cancel(context.getSource().getServer())
						? Msg.ok(context.getSource(), "Odliczanie przerwane.")
						: Msg.fail(context.getSource(), "Nie ma aktywnego odliczania.")))
				.then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
						.executes(context -> countdown(context, null))
						.then(Commands.argument("message", StringArgumentType.greedyString())
								.executes(context -> countdown(context, StringArgumentType.getString(context, "message"))))));

		dispatcher.register(Commands.literal("go")
				.requires(Perms.gamemaster())
				.executes(context -> go(context.getSource(), 3, null))
				.then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
						.executes(context -> go(context.getSource(), IntegerArgumentType.getInteger(context, "seconds"), null))
						.then(Commands.argument("message", StringArgumentType.greedyString())
								.executes(context -> go(context.getSource(), IntegerArgumentType.getInteger(context, "seconds"),
										StringArgumentType.getString(context, "message"))))));
	}

	private static int countdown(CommandContext<CommandSourceStack> context, String message) {
		int seconds = IntegerArgumentType.getInteger(context, "seconds");
		Countdown.start(seconds, message == null ? null : Component.literal(Msg.colors(message)), null);
		return Msg.ok(context.getSource(), "Odliczanie: " + seconds + " s.");
	}

	/**
	 * Freeze everyone, count down, then release the players - one command to start a challenge.
	 */
	private static int go(CommandSourceStack source, int seconds, String message) {
		MinecraftServer server = source.getServer();
		List<ServerPlayer> frozen = new ArrayList<>();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isSpectator() && Freeze.freeze(player)) {
				frozen.add(player);
			}
		}

		Countdown.start(seconds, message == null ? null : Component.literal(Msg.colors(message)), () -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (frozen.stream().anyMatch(p -> p.getUUID().equals(player.getUUID()))) {
					Freeze.unfreeze(player);
				}
			}
		});

		return Msg.ok(source, "Start za " + seconds + " s, zamrożono graczy: " + frozen.size() + ".");
	}

	// /freeze, /unfreeze

	private static void registerFreeze(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("freeze")
				.requires(Perms.gamemaster())
				.executes(context -> freeze(context.getSource(), everyoneExcept(context.getSource()), true))
				.then(Commands.argument("targets", EntityArgument.players())
						.executes(context -> freeze(context.getSource(), EntityArgument.getPlayers(context, "targets"), true))));

		dispatcher.register(Commands.literal("unfreeze")
				.requires(Perms.gamemaster())
				.executes(context -> freeze(context.getSource(), context.getSource().getServer().getPlayerList().getPlayers(), false))
				.then(Commands.argument("targets", EntityArgument.players())
						.executes(context -> freeze(context.getSource(), EntityArgument.getPlayers(context, "targets"), false))));
	}

	private static List<ServerPlayer> everyoneExcept(CommandSourceStack source) {
		List<ServerPlayer> players = new ArrayList<>(source.getServer().getPlayerList().getPlayers());
		players.remove(source.getPlayer());
		return players;
	}

	private static int freeze(CommandSourceStack source, Collection<ServerPlayer> players, boolean freeze) {
		int changed = 0;

		for (ServerPlayer player : players) {
			if (freeze ? Freeze.freeze(player) : Freeze.unfreeze(player)) {
				changed++;
				Screens.actionBar(player, Component.literal(freeze ? "❄ Zamrożony - czekaj na start" : "Możesz się ruszać!")
						.withStyle(freeze ? ChatFormatting.AQUA : ChatFormatting.GREEN));
			}
		}

		if (changed == 0) {
			return Msg.fail(source, freeze ? "Nikt nowy nie został zamrożony." : "Nikt nie był zamrożony.");
		}

		return Msg.ok(source, (freeze ? "Zamrożono" : "Odmrożono") + " graczy: " + changed + ".");
	}

	// /nv, /heal, /cam

	private static void registerPlayerHelpers(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("nv")
				.requires(Perms.gamemaster())
				.executes(context -> nightVision(context.getSource(), List.of(context.getSource().getPlayerOrException())))
				.then(Commands.argument("targets", EntityArgument.players())
						.executes(context -> nightVision(context.getSource(), EntityArgument.getPlayers(context, "targets")))));

		dispatcher.register(Commands.literal("heal")
				.requires(Perms.gamemaster())
				.executes(context -> heal(context.getSource(), List.of(context.getSource().getPlayerOrException())))
				.then(Commands.argument("targets", EntityArgument.entities())
						.executes(context -> heal(context.getSource(), EntityArgument.getEntities(context, "targets")))));

		dispatcher.register(Commands.literal("cam")
				.requires(Perms.gamemaster())
				.executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					return CamMode.toggle(player)
							? Msg.ok(context.getSource(), "Tryb kamery: WŁ (widz). Wpisz /cam, żeby wrócić na miejsce.")
							: Msg.ok(context.getSource(), "Tryb kamery: WYŁ. Wróciłeś na swoje miejsce.");
				}));
	}

	private static int nightVision(CommandSourceStack source, Collection<ServerPlayer> players) {
		int enabled = 0;

		for (ServerPlayer player : players) {
			if (player.hasEffect(MobEffects.NIGHT_VISION)) {
				player.removeEffect(MobEffects.NIGHT_VISION);
			} else {
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
				enabled++;
			}
		}

		return Msg.ok(source, "Noktowizja włączona: " + enabled + ", wyłączona: " + (players.size() - enabled) + ".");
	}

	private static int heal(CommandSourceStack source, Collection<? extends Entity> targets) {
		int healed = 0;

		for (Entity entity : targets) {
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				continue;
			}

			living.setHealth(living.getMaxHealth());
			living.clearFire();
			living.setAirSupply(living.getMaxAirSupply());

			for (MobEffectInstance effect : new ArrayList<>(living.getActiveEffects())) {
				if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
					living.removeEffect(effect.getEffect());
				}
			}

			if (living instanceof ServerPlayer player) {
				player.getFoodData().setFoodLevel(20);
				player.getFoodData().setSaturation(20.0F);
			}

			healed++;
		}

		return healed == 0 ? Msg.fail(source, "Nie znaleziono żywych celów.") : Msg.ok(source, "Uleczono: " + healed + ".");
	}

	// /recmode

	private static void registerRecMode(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("recmode")
				.requires(Perms.gamemaster())
				.executes(context -> Msg.ok(context.getSource(), "Tryb nagrywania: " + Msg.onOff(RecMode.isActive())
						+ ". /recmode on ukrywa komunikaty komend na czacie."))
				.then(Commands.literal("on").executes(context -> RecMode.enable(context.getSource().getServer())
						? Msg.ok(context.getSource(), "Tryb nagrywania WŁ: komunikaty komend nie pojawią się na czacie.")
						: Msg.fail(context.getSource(), "Tryb nagrywania jest już włączony.")))
				.then(Commands.literal("off").executes(context -> RecMode.disable(context.getSource().getServer())
						? Msg.ok(context.getSource(), "Tryb nagrywania WYŁ: przywrócono poprzednie ustawienia.")
						: Msg.fail(context.getSource(), "Tryb nagrywania nie był włączony."))));
	}

	// /cleanup

	private enum CleanupKind {
		ITEMS, MOBS, ALL
	}

	private static void registerCleanup(CommandDispatcher<CommandSourceStack> dispatcher) {
		var root = Commands.literal("cleanup").requires(Perms.gamemaster())
				.executes(context -> cleanup(context.getSource(), CleanupKind.ITEMS, 64))
				.then(Commands.argument("radius", IntegerArgumentType.integer(1, 512))
						.executes(context -> cleanup(context.getSource(), CleanupKind.ITEMS, IntegerArgumentType.getInteger(context, "radius"))));

		for (CleanupKind kind : CleanupKind.values()) {
			String name = kind.name().toLowerCase(java.util.Locale.ROOT);
			root.then(Commands.literal(name)
					.executes(context -> cleanup(context.getSource(), kind, 64))
					.then(Commands.argument("radius", IntegerArgumentType.integer(1, 512))
							.executes(context -> cleanup(context.getSource(), kind, IntegerArgumentType.getInteger(context, "radius")))));
		}

		dispatcher.register(root);
	}

	private static int cleanup(CommandSourceStack source, CleanupKind kind, int radius) {
		Vec3 center = source.getPosition();
		AABB area = AABB.ofSize(center, radius * 2.0, radius * 2.0, radius * 2.0);
		List<Entity> entities = source.getLevel().getEntities((Entity) null, area, entity -> shouldClean(entity, kind));
		entities.forEach(Entity::discard);
		return Msg.ok(source, "Usunięto " + entities.size() + " obiektów w promieniu " + radius + " bloków.");
	}

	private static boolean shouldClean(Entity entity, CleanupKind kind) {
		boolean clutter = entity instanceof ItemEntity || entity instanceof ExperienceOrb
				|| (entity instanceof AbstractArrow && !(entity instanceof ThrownTrident));
		boolean hostile = entity instanceof Mob mob && entity instanceof Enemy && !mob.hasCustomName() && !mob.isPersistenceRequired();

		return switch (kind) {
			case ITEMS -> clutter;
			case MOBS -> hostile;
			case ALL -> clutter || hostile;
		};
	}

	// /announce

	private static void registerAnnounce(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("announce")
				.requires(Perms.gamemaster())
				.then(Commands.argument("text", StringArgumentType.greedyString()).executes(context -> {
					String text = Msg.colors(StringArgumentType.getString(context, "text"));
					String[] parts = text.split("\\|", 2);
					List<ServerPlayer> players = context.getSource().getServer().getPlayerList().getPlayers();
					Component title = Component.literal(parts[0].trim()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
					Component subtitle = parts.length > 1 ? Component.literal(parts[1].trim()).withStyle(ChatFormatting.WHITE) : null;

					Screens.title(players, title, subtitle, 10, 70, 20);
					Screens.sound(players, Screens.sound(SoundEvents.PLAYER_LEVELUP), 0.6F, 0.8F);
					return Msg.ok(context.getSource(), "Wyświetlono napis dla " + players.size() + " graczy.");
				})));
	}
}
