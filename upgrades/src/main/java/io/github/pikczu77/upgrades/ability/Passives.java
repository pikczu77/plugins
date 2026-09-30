package io.github.pikczu77.upgrades.ability;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;

import io.github.pikczu77.upgrades.Upgrades;
import io.github.pikczu77.upgrades.upgrade.Upgrade;

/**
 * Always-on effects: the golem arm damage bonus and the dragon wing flight.
 */
public final class Passives {
	private static final Identifier GOLEM_DAMAGE = Upgrades.id("golem_arm");
	/** Extra melee damage of the iron golem arm. */
	public static final double GOLEM_ARM_DAMAGE = 3.0;

	private Passives() {
	}

	public static void apply(ServerPlayer player, long mask) {
		AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);

		if (damage != null) {
			if (Upgrade.has(mask, Upgrade.GOLEM_ARM)) {
				damage.addOrUpdateTransientModifier(new AttributeModifier(GOLEM_DAMAGE, GOLEM_ARM_DAMAGE, AttributeModifier.Operation.ADD_VALUE));
			} else {
				damage.removeModifier(GOLEM_DAMAGE);
			}
		}

		updateFlight(player, mask);
	}

	public static void tick(ServerPlayer player, long mask) {
		// Changing the game mode or dying resets the abilities, keep the wings working.
		if (Upgrade.has(mask, Upgrade.DRAGON_WING) && !player.getAbilities().mayfly) {
			updateFlight(player, mask);
		}
	}

	private static void updateFlight(ServerPlayer player, long mask) {
		Abilities abilities = player.getAbilities();
		GameType mode = player.gameMode();
		boolean creativeFlight = mode == GameType.CREATIVE || mode == GameType.SPECTATOR;
		boolean wings = Upgrade.has(mask, Upgrade.DRAGON_WING);

		if (wings && !abilities.mayfly) {
			abilities.mayfly = true;
			player.onUpdateAbilities();
		} else if (!wings && !creativeFlight && abilities.mayfly) {
			abilities.mayfly = false;
			abilities.flying = false;
			player.onUpdateAbilities();
		}
	}
}
