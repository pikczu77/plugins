package io.github.pikczu77.hpsize.command;

import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.hpsize.util.Msg;

/**
 * Commands for setting up shots with mobs of an exact size (size = health).
 */
public final class MobCommands {
	private MobCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
		// /mobhp <targets> <hp>          - sets the health (and therefore the size)
		// /mobhp <targets> max <hp>      - sets the max health and heals to full
		dispatcher.register(Commands.literal("mobhp")
				.requires(Perms.gamemaster())
				.then(Commands.argument("targets", EntityArgument.entities())
						.then(Commands.literal("max").then(Commands.argument("hp", FloatArgumentType.floatArg(0.1F, 1024.0F))
								.executes(context -> setHealth(context, true))))
						.then(Commands.argument("hp", FloatArgumentType.floatArg(0.1F, 1024.0F))
								.executes(context -> setHealth(context, false)))));

		// /spawnsized <mob> <hp> [count]
		dispatcher.register(Commands.literal("spawnsized")
				.requires(Perms.gamemaster())
				.then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
						.then(Commands.argument("hp", FloatArgumentType.floatArg(0.1F, 1024.0F))
								.executes(context -> spawn(context, 1))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
										.executes(context -> spawn(context, IntegerArgumentType.getInteger(context, "count")))))));

		// /glow <targets> [seconds]  (0 seconds = turn off)
		dispatcher.register(Commands.literal("glow")
				.requires(Perms.gamemaster())
				.then(Commands.argument("targets", EntityArgument.entities())
						.executes(context -> glow(context.getSource(), EntityArgument.getEntities(context, "targets"), 60))
						.then(Commands.argument("seconds", IntegerArgumentType.integer(0, 100000))
								.executes(context -> glow(context.getSource(), EntityArgument.getEntities(context, "targets"),
										IntegerArgumentType.getInteger(context, "seconds"))))));

		// /mute <targets>, /unmute <targets>
		dispatcher.register(Commands.literal("mute")
				.requires(Perms.gamemaster())
				.then(Commands.argument("targets", EntityArgument.entities())
						.executes(context -> silence(context.getSource(), EntityArgument.getEntities(context, "targets"), true))));
		dispatcher.register(Commands.literal("unmute")
				.requires(Perms.gamemaster())
				.then(Commands.argument("targets", EntityArgument.entities())
						.executes(context -> silence(context.getSource(), EntityArgument.getEntities(context, "targets"), false))));
	}

	private static int setHealth(CommandContext<CommandSourceStack> context, boolean max) throws CommandSyntaxException {
		float hp = FloatArgumentType.getFloat(context, "hp");
		int changed = 0;

		for (Entity entity : EntityArgument.getEntities(context, "targets")) {
			if (entity instanceof LivingEntity living && living.isAlive()) {
				applyHealth(living, hp, max);
				changed++;
			}
		}

		if (changed == 0) {
			return Msg.fail(context.getSource(), "Nie znaleziono żywych mobów.");
		}

		return Msg.ok(context.getSource(), (max ? "Maks. HP" : "HP") + " = " + Msg.number(hp) + " dla " + changed + " mobów.");
	}

	private static void applyHealth(LivingEntity living, float hp, boolean max) {
		AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);

		if (maxHealth != null && (max || hp > living.getMaxHealth())) {
			maxHealth.setBaseValue(hp);
		}

		living.setHealth(hp);
	}

	private static int spawn(CommandContext<CommandSourceStack> context, int count) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		Holder.Reference<EntityType<?>> type = ResourceArgument.getSummonableEntityType(context, "entity");
		float hp = FloatArgumentType.getFloat(context, "hp");
		ServerLevel level = source.getLevel();
		Vec3 position = source.getPosition();
		int spawned = 0;

		for (int i = 0; i < count; i++) {
			Entity entity = type.value().create(level, EntitySpawnReason.COMMAND);

			if (entity == null) {
				break;
			}

			double spread = count > 1 ? 2.0 + Math.sqrt(count) : 0.0;
			entity.snapTo(position.x + (level.getRandom().nextDouble() - 0.5) * spread, position.y,
					position.z + (level.getRandom().nextDouble() - 0.5) * spread, level.getRandom().nextFloat() * 360.0F, 0.0F);

			if (entity instanceof Mob mob) {
				mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.COMMAND, null);
			}

			if (entity instanceof LivingEntity living) {
				applyHealth(living, hp, true);
			}

			level.addFreshEntityWithPassengers(entity);
			spawned++;
		}

		if (spawned == 0) {
			return Msg.fail(source, "Nie udało się przywołać tego moba.");
		}

		return Msg.ok(source, "Przywołano " + spawned + "× " + type.value().getDescription().getString() + " z " + Msg.number(hp) + " HP.");
	}

	private static int glow(CommandSourceStack source, Collection<? extends Entity> targets, int seconds) {
		int changed = 0;

		for (Entity entity : targets) {
			if (entity instanceof LivingEntity living) {
				if (seconds == 0) {
					living.removeEffect(MobEffects.GLOWING);
				} else {
					living.addEffect(new MobEffectInstance(MobEffects.GLOWING, seconds * 20, 0, false, false, false));
				}

				changed++;
			}
		}

		return changed == 0 ? Msg.fail(source, "Nie znaleziono mobów.")
				: Msg.ok(source, (seconds == 0 ? "Wyłączono podświetlenie: " : "Podświetlono na " + seconds + " s: ") + changed + ".");
	}

	private static int silence(CommandSourceStack source, Collection<? extends Entity> targets, boolean silent) {
		targets.forEach(entity -> entity.setSilent(silent));
		return Msg.ok(source, (silent ? "Wyciszono: " : "Przywrócono dźwięk: ") + targets.size() + ".");
	}
}
