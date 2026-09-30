package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.settings.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Explosions caused by the custom items. They never hurt or push the player who caused them and never
 * destroy dropped loot ("do not blow up my items"), and they only break terrain when the
 * {@code explosions_break_blocks} setting is on.
 */
public final class Explosions {
	/** Entity tag for projectiles/TNT spawned by an ability, so their explosions spare their owner. */
	public static final String OWNED_TAG = "blockopener_owned";

	public static void explode(ServerLevel level, @Nullable Entity owner, Vec3 at, float power) {
		explode(level, owner, at, power, ModSettings.get(level.getServer()).explosionsBreakBlocks());
	}

	public static void explode(ServerLevel level, @Nullable Entity owner, Vec3 at, float power, boolean breakBlocks) {
		level.explode(
			owner,
			level.damageSources().explosion(owner, owner),
			new OwnerSafe(owner, breakBlocks),
			at.x, at.y, at.z,
			power,
			false,
			breakBlocks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE
		);
	}

	public static void markOwned(Entity entity) {
		entity.addTag(OWNED_TAG);
	}

	/** True when {@code victim} is being hit by the explosion of one of its own abilities. */
	public static boolean isOwnAbilityExplosion(LivingEntity victim, DamageSource source) {
		if (!source.is(DamageTypeTags.IS_EXPLOSION) || source.getEntity() != victim) {
			return false;
		}
		Entity direct = source.getDirectEntity();
		return direct == null || direct == victim || direct.getTags().contains(OWNED_TAG);
	}

	private static final class OwnerSafe extends ExplosionDamageCalculator {
		private final @Nullable Entity owner;
		private final boolean breakBlocks;

		OwnerSafe(@Nullable Entity owner, boolean breakBlocks) {
			this.owner = owner;
			this.breakBlocks = breakBlocks;
		}

		@Override
		public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
			return this.breakBlocks;
		}

		@Override
		public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
			return entity != this.owner && !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb);
		}

		@Override
		public float getKnockbackMultiplier(Entity entity) {
			return entity == this.owner || entity instanceof ItemEntity ? 0.0F : 1.0F;
		}
	}

	private Explosions() {
	}
}
