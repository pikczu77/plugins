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
 * Explosions caused by the custom items. They never hurt or push the player who caused them.
 * Like vanilla TNT they blow up dropped items and break terrain, unless the
 * {@code explosions_destroy_items} / {@code explosions_break_blocks} settings are turned off.
 */
public final class Explosions {
	/** Entity tag for projectiles/TNT spawned by an ability, so their explosions spare their owner. */
	public static final String OWNED_TAG = "blockopener_owned";

	public static void explode(ServerLevel level, @Nullable Entity owner, Vec3 at, float power) {
		ModSettings settings = ModSettings.get(level.getServer());
		explode(level, owner, at, power, settings.explosionsBreakBlocks(), settings.explosionsDestroyItems());
	}

	public static void explode(ServerLevel level, @Nullable Entity owner, Vec3 at, float power, boolean breakBlocks, boolean destroyItems) {
		level.explode(
			owner,
			level.damageSources().explosion(owner, owner),
			new OwnerSafe(owner, breakBlocks, destroyItems),
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
		private final boolean destroyItems;

		OwnerSafe(@Nullable Entity owner, boolean breakBlocks, boolean destroyItems) {
			this.owner = owner;
			this.breakBlocks = breakBlocks;
			this.destroyItems = destroyItems;
		}

		private boolean spared(Entity entity) {
			return !this.destroyItems && (entity instanceof ItemEntity || entity instanceof ExperienceOrb);
		}

		@Override
		public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
			return this.breakBlocks;
		}

		@Override
		public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
			return entity != this.owner && !this.spared(entity);
		}

		@Override
		public float getKnockbackMultiplier(Entity entity) {
			return entity == this.owner || this.spared(entity) ? 0.0F : 1.0F;
		}
	}

	private Explosions() {
	}
}
