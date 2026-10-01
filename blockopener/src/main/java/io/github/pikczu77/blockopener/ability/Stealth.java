package io.github.pikczu77.blockopener.ability;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;

/** Makes mobs forget about a player (pumpkin disguise, tiny size, Ender Gloves). */
final class Stealth {
	static void loseTrack(ServerPlayer player, double minDistance, Predicate<Mob> which) {
		for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0),
			mob -> mob.getTarget() == player && mob.distanceToSqr(player) > minDistance * minDistance && which.test(mob))) {
			mob.setTarget(null);
			if (mob instanceof NeutralMob neutral) {
				neutral.stopBeingAngry();
			}
		}
	}

	private Stealth() {
	}
}
