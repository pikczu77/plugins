package io.github.pikczu77.upgrades.ability;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import io.github.pikczu77.upgrades.Upgrades;
import io.github.pikczu77.upgrades.upgrade.Bonus;

/**
 * Stat and lasting-effect bonuses: an attribute modifier per bonus, and potion effects refreshed every few seconds.
 */
public final class BonusPassives {
	/** Long enough that night vision does not start flickering before the next refresh. */
	private static final int EFFECT_TICKS = 400;

	private BonusPassives() {
	}

	private static Identifier modifierId(Bonus bonus) {
		return Upgrades.id("bonus/" + bonus.id());
	}

	public static void apply(ServerPlayer player, long[] bits) {
		for (Bonus bonus : Bonus.VALUES) {
			Bonus.Effect effect = bonus.effect;

			if (effect.attribute() == null) {
				continue;
			}

			AttributeInstance instance = player.getAttribute(effect.attribute());

			if (instance == null) {
				continue;
			}

			if (Bonus.has(bits, bonus)) {
				instance.addOrUpdateTransientModifier(new AttributeModifier(modifierId(bonus), effect.amount(), effect.operation()));
			} else {
				instance.removeModifier(modifierId(bonus));
			}
		}

		// Losing max health bonuses must not leave the player above the new maximum.
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}

		tick(player, bits, true);
	}

	public static void tick(ServerPlayer player, long[] bits, boolean force) {
		if (!force && player.tickCount % 100 != 0) {
			return;
		}

		for (Bonus bonus : Bonus.VALUES) {
			if (bonus.effect.potion() != null && Bonus.has(bits, bonus)) {
				player.addEffect(new MobEffectInstance(bonus.effect.potion(), EFFECT_TICKS, 0, true, false, true));
			}
		}
	}
}
