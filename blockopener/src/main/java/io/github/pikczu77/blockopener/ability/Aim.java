package io.github.pikczu77.blockopener.ability;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Long-range aiming helpers for abilities that work at a distance. */
public final class Aim {
	public static BlockHitResult block(Player player, double range, ClipContext.Fluid fluid) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(range));
		return player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, fluid, player));
	}

	/** First entity along the view ray (not behind walls), resolving dragon parts to the dragon. */
	public static @Nullable Entity entity(Player player, double range, Predicate<Entity> filter) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = eye.add(look.scale(range));
		BlockHitResult blockHit = block(player, range, ClipContext.Fluid.NONE);
		if (blockHit.getType() != HitResult.Type.MISS) {
			end = blockHit.getLocation();
		}
		AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(
			player, eye, end, box, entity -> !entity.isSpectator() && entity.isPickable() && filter.test(root(entity)), eye.distanceToSqr(end)
		);
		return hit == null ? null : root(hit.getEntity());
	}

	/** Where the player is looking: the entity, else the block, else the end of the ray. */
	public static Vec3 point(Player player, double range) {
		Entity entity = entity(player, range, e -> e != player);
		if (entity != null) {
			return entity.position();
		}
		BlockHitResult hit = block(player, range, ClipContext.Fluid.NONE);
		return hit.getLocation();
	}

	public static Entity root(Entity entity) {
		return entity instanceof EnderDragonPart part ? part.parentMob : entity;
	}

	private Aim() {
	}
}
