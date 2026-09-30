package io.github.pikczu77.hpsize.util;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LookTarget {
	private LookTarget() {
	}

	/**
	 * Finds the living entity the player is looking at. Uses the (possibly giant) bounding boxes,
	 * so a mob can be picked from far away or even when the player stands inside it.
	 */
	public static @Nullable LivingEntity find(ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		// Stop at the first solid block, so mobs hidden behind walls are ignored.
		Vec3 end = player.level().clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
		AABB area = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(12.0);

		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;

		for (Entity entity : player.level().getEntities(player, area, entity -> entity instanceof LivingEntity && entity.isAlive()
				&& !entity.isSpectator() && entity.getType() != EntityType.ARMOR_STAND)) {
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
