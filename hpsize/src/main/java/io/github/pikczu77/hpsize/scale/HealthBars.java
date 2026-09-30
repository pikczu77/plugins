package io.github.pikczu77.hpsize.scale;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.hpsize.config.HpSizeConfig;
import io.github.pikczu77.hpsize.util.Msg;

/**
 * A boss bar showing the health and size of the mob the player is looking at (or has just hit),
 * so viewers can see why a mob is that big.
 */
public final class HealthBars {
	private static final double LOOK_RANGE = 64.0;
	private static final int KEEP_TICKS = 100;
	private static final Map<UUID, Tracker> TRACKERS = new HashMap<>();

	private HealthBars() {
	}

	private static final class Tracker {
		private @Nullable ServerBossEvent bar;
		private @Nullable UUID target;
		private long lastSeen;

		private void hide() {
			if (bar != null) {
				bar.removeAllPlayers();
				bar = null;
			}
		}
	}

	public static void remember(ServerPlayer player, LivingEntity target) {
		Tracker tracker = TRACKERS.computeIfAbsent(player.getUUID(), uuid -> new Tracker());
		tracker.target = target.getUUID();
		tracker.lastSeen = player.level().getGameTime();
	}

	public static void forget(ServerPlayer player) {
		Tracker tracker = TRACKERS.remove(player.getUUID());

		if (tracker != null) {
			tracker.hide();
		}
	}

	public static void reset() {
		TRACKERS.values().forEach(Tracker::hide);
		TRACKERS.clear();
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 2 != 0) {
			return;
		}

		HpSizeConfig config = HpSizeConfig.get();
		boolean enabled = config != null && config.enabled && config.healthBar;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Tracker tracker = TRACKERS.computeIfAbsent(player.getUUID(), uuid -> new Tracker());

			if (!enabled) {
				tracker.hide();
				continue;
			}

			ServerLevel level = (ServerLevel) player.level();
			long now = level.getGameTime();
			LivingEntity looked = findLookTarget(player, LOOK_RANGE);

			if (looked != null) {
				tracker.target = looked.getUUID();
				tracker.lastSeen = now;
			}

			LivingEntity target = null;

			if (tracker.target != null && now - tracker.lastSeen <= KEEP_TICKS) {
				Entity entity = level.getEntity(tracker.target);

				if (entity instanceof LivingEntity living && living.isAlive()) {
					target = living;
				}
			}

			if (target == null) {
				tracker.target = null;
				tracker.hide();
				continue;
			}

			show(player, tracker, target);
		}

		Iterator<Map.Entry<UUID, Tracker>> iterator = TRACKERS.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, Tracker> entry = iterator.next();

			if (server.getPlayerList().getPlayer(entry.getKey()) == null) {
				entry.getValue().hide();
				iterator.remove();
			}
		}
	}

	private static void show(ServerPlayer player, Tracker tracker, LivingEntity target) {
		float health = target.getHealth();
		float maxHealth = Math.max(0.001F, target.getMaxHealth());
		float progress = Math.max(0.0F, Math.min(1.0F, health / maxHealth));

		Component name = Component.empty()
				.append(target.getDisplayName())
				.append(Component.literal("  ❤ " + Msg.number(health) + "/" + Msg.number(maxHealth)).withStyle(ChatFormatting.RED))
				.append(Component.literal("  •  rozmiar ×" + Msg.number(target.getScale())).withStyle(ChatFormatting.AQUA));

		BossEvent.BossBarColor color = progress > 0.5F ? BossEvent.BossBarColor.GREEN
				: progress > 0.25F ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED;

		if (tracker.bar == null) {
			tracker.bar = new ServerBossEvent(name, color, BossEvent.BossBarOverlay.NOTCHED_10);
		}

		ServerBossEvent bar = tracker.bar;
		bar.setName(name);
		bar.setColor(color);
		bar.setProgress(progress);

		if (!bar.getPlayers().contains(player)) {
			bar.removeAllPlayers();
			bar.addPlayer(player);
		}
	}

	/**
	 * Finds the living entity the player is looking at. Uses the (possibly giant) bounding boxes,
	 * so a mob can be picked from far away or even when the player stands inside it.
	 */
	public static @Nullable LivingEntity findLookTarget(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = eye.add(look.scale(range));
		AABB area = player.getBoundingBox().expandTowards(look.scale(range)).inflate(12.0);

		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;

		for (Entity entity : player.level().getEntities(player, area, entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator())) {
			AABB box = entity.getBoundingBox().inflate(0.2);
			double distance;

			if (box.contains(eye)) {
				distance = 0.0;
			} else {
				Optional<Vec3> hit = box.clip(eye, end);

				if (hit.isEmpty()) {
					continue;
				}

				distance = eye.distanceToSqr(hit.get());
			}

			if (distance < bestDistance) {
				bestDistance = distance;
				best = (LivingEntity) entity;
			}
		}

		return best;
	}
}
