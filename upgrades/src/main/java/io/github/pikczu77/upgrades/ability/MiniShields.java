package io.github.pikczu77.upgrades.ability;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

/**
 * The shields on the shoulders block every projectile and send it straight back at whoever shot it.
 */
public final class MiniShields {
	private MiniShields() {
	}

	/** Deflection that aims the projectile back at its shooter and gives it to the shielded player. */
	private record Reflect(ServerPlayer player) implements ProjectileDeflection {
		@Override
		public void deflect(Projectile projectile, @Nullable Entity entity, RandomSource random) {
			double speed = Math.max(0.8, projectile.getDeltaMovement().length());
			Entity shooter = projectile.getOwner();
			Vec3 direction;

			if (shooter != null && shooter != this.player && shooter.isAlive()) {
				direction = shooter.getBoundingBox().getCenter().subtract(projectile.position()).normalize();
			} else {
				direction = projectile.getDeltaMovement().scale(-1.0).normalize();
			}

			projectile.setDeltaMovement(direction.scale(speed));
			projectile.needsSync = true;
			this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), SoundEvents.SHIELD_BLOCK.value(),
					SoundSource.PLAYERS, 1.0F, 1.2F + random.nextFloat() * 0.3F);
		}
	}

	public static @Nullable ProjectileDeflection deflection(Entity target, Projectile projectile) {
		if (target instanceof ServerPlayer player && UpgradeManager.has(player, Upgrade.MINI_SHIELDS) && projectile.getOwner() != player
				&& !player.isSpectator()) {
			return new Reflect(player);
		}

		return null;
	}

	public static void afterDeflect(Projectile projectile, ProjectileDeflection deflection) {
		if (deflection instanceof Reflect(ServerPlayer player) && !projectile.level().isClientSide()) {
			projectile.setOwner(player);
		}
	}
}
