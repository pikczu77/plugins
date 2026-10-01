package io.github.pikczu77.blockopener.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

/** Storm Hammer: lightning where you point, and a ring of lightning after a jump attack. */
public final class StormHammer {
	public static void strike(ServerPlayer owner, Vec3 at) {
		ServerLevel level = owner.level();
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt == null) {
			return;
		}
		bolt.snapTo(at.x, at.y, at.z);
		bolt.setCause(owner);
		level.addFreshEntity(bolt);
		ModAbilities.state(owner).stormSafeUntil = level.getGameTime() + 60;
	}

	public static void ring(ServerPlayer owner, Vec3 center) {
		strike(owner, center);
		for (int i = 0; i < 6; i++) {
			double angle = Math.PI * 2 * i / 6;
			strike(owner, center.add(Math.cos(angle) * 4.0, 0.0, Math.sin(angle) * 4.0));
		}
	}

	/** The thrower never gets zapped or burned by their own storm. */
	static boolean isOwnStorm(ServerPlayer player, DamageSource source) {
		if (!source.is(DamageTypeTags.IS_LIGHTNING) && !source.is(DamageTypeTags.IS_FIRE)) {
			return false;
		}
		if (player.level().getGameTime() <= ModAbilities.state(player).stormSafeUntil) {
			player.clearFire();
			return true;
		}
		return false;
	}

	private StormHammer() {
	}
}
