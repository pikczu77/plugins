package io.github.pikczu77.hpsize.rec;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.hpsize.HpSize;

/**
 * Keeps players in place (they can still look around), e.g. before the start of a challenge.
 */
public final class Freeze {
	private static final Identifier SPEED_ID = HpSize.id("freeze_speed");
	private static final Identifier JUMP_ID = HpSize.id("freeze_jump");
	private static final Map<UUID, Spot> FROZEN = new HashMap<>();

	private record Spot(ResourceKey<Level> dimension, Vec3 position) {
	}

	private Freeze() {
	}

	public static boolean freeze(ServerPlayer player) {
		if (FROZEN.containsKey(player.getUUID())) {
			return false;
		}

		FROZEN.put(player.getUUID(), new Spot(player.level().dimension(), player.position()));
		applyModifiers(player);
		return true;
	}

	public static boolean unfreeze(ServerPlayer player) {
		if (FROZEN.remove(player.getUUID()) == null) {
			return false;
		}

		removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
		removeModifier(player, Attributes.JUMP_STRENGTH, JUMP_ID);
		return true;
	}

	public static boolean isFrozen(ServerPlayer player) {
		return FROZEN.containsKey(player.getUUID());
	}

	public static void reset() {
		FROZEN.clear();
	}

	public static void tick(MinecraftServer server) {
		if (FROZEN.isEmpty()) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Spot spot = FROZEN.get(player.getUUID());

			if (spot == null) {
				continue;
			}

			// Modifiers are lost when the player respawns or relogs.
			applyModifiers(player);

			if (player.level().dimension() != spot.dimension()) {
				FROZEN.put(player.getUUID(), new Spot(player.level().dimension(), player.position()));
				continue;
			}

			double dx = player.getX() - spot.position().x;
			double dz = player.getZ() - spot.position().z;
			boolean rose = player.getY() - spot.position().y > 1.25;

			if (dx * dx + dz * dz > 0.04 || rose) {
				// Keep the rotation relative, so the player can still look around freely.
				player.teleportTo((ServerLevel) player.level(), spot.position().x, Math.min(player.getY(), spot.position().y), spot.position().z,
						EnumSet.of(Relative.X_ROT, Relative.Y_ROT), 0.0F, 0.0F, false);
			}
		}
	}

	private static void applyModifiers(ServerPlayer player) {
		addModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
		addModifier(player, Attributes.JUMP_STRENGTH, JUMP_ID);
	}

	private static void addModifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id) {
		AttributeInstance instance = player.getAttribute(attribute);

		if (instance != null && instance.getModifier(id) == null) {
			instance.addTransientModifier(new AttributeModifier(id, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	private static void removeModifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id) {
		AttributeInstance instance = player.getAttribute(attribute);

		if (instance != null) {
			instance.removeModifier(id);
		}
	}
}
