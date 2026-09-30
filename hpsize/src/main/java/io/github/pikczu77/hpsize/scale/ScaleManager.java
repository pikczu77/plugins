package io.github.pikczu77.hpsize.scale;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import io.github.pikczu77.hpsize.HpSize;
import io.github.pikczu77.hpsize.config.HpSizeConfig;

/**
 * Makes every living entity as big as its health.
 *
 * <p>The size is applied as a transient {@code minecraft:scale} modifier: it is never written to the world save,
 * so removing the mod (or turning it off) brings every mob back to its normal size.
 */
public final class ScaleManager {
	public static final Identifier MODIFIER_ID = HpSize.id("health_scale");

	private ScaleManager() {
	}

	public static void tick(MinecraftServer server) {
		HpSizeConfig config = HpSizeConfig.get();

		if (config == null) {
			return;
		}

		boolean glowCheck = config.enabled && config.glowTiny && server.getTickCount() % 5 == 0;

		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof LivingEntity living) {
					update(living, config, glowCheck);
				}
			}
		}
	}

	private static void update(LivingEntity entity, HpSizeConfig config, boolean glowCheck) {
		AttributeInstance scale = entity.getAttribute(Attributes.SCALE);

		if (scale == null) {
			return;
		}

		AttributeModifier modifier = scale.getModifier(MODIFIER_ID);

		if (!config.enabled || !config.affects(entity)) {
			if (modifier != null) {
				scale.removeModifier(MODIFIER_ID);
			}

			return;
		}

		if (!config.frozen) {
			double current = modifier == null ? 1.0 : 1.0 + modifier.amount();
			double target = config.targetScale(entity);
			double next = config.smooth <= 0 ? target : current + (target - current) / (config.smooth + 1);

			if (Math.abs(target - next) < 0.01) {
				next = target;
			}

			if (modifier == null || Math.abs(next - current) > 1.0E-4) {
				// ADD_MULTIPLIED_TOTAL keeps other changes of the scale (e.g. /attribute) working.
				scale.addOrUpdateTransientModifier(new AttributeModifier(MODIFIER_ID, next - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		}

		if (glowCheck && entity.isAlive() && scale.getValue() < config.glowTinyBelow) {
			entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 15, 0, false, false, false));
		}
	}

	/**
	 * Current size multiplier applied by this mod (1.0 when not scaled).
	 */
	public static double appliedScale(LivingEntity entity) {
		AttributeInstance scale = entity.getAttribute(Attributes.SCALE);

		if (scale == null) {
			return 1.0;
		}

		AttributeModifier modifier = scale.getModifier(MODIFIER_ID);
		return modifier == null ? 1.0 : 1.0 + modifier.amount();
	}

	/**
	 * Optionally stops scaled mobs from suffocating inside blocks (which in the video made them shrink).
	 */
	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		HpSizeConfig config = HpSizeConfig.get();

		return config == null
				|| config.suffocation
				|| !config.enabled
				|| !source.is(DamageTypes.IN_WALL)
				|| !config.affects(entity);
	}
}
