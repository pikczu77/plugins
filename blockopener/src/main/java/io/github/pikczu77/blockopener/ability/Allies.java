package io.github.pikczu77.blockopener.ability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Mobs released from the Mob Cage fight for whoever released them: they never target their owner,
 * attack whatever the owner hits (or whatever hits the owner) and follow the owner around.
 */
public final class Allies {
	private static final List<Ally> ACTIVE = new ArrayList<>();

	public static void add(Mob mob, ServerPlayer owner) {
		ACTIVE.removeIf(ally -> ally.mob == mob);
		ACTIVE.add(new Ally(mob, owner.getUUID()));
		calm(mob, owner);
	}

	public static boolean isAllyOf(Player player, Entity entity) {
		for (Ally ally : ACTIVE) {
			if (ally.mob == entity && ally.owner.equals(player.getUUID())) {
				return true;
			}
		}
		return false;
	}

	static void tick(MinecraftServer server) {
		Iterator<Ally> iterator = ACTIVE.iterator();
		while (iterator.hasNext()) {
			Ally ally = iterator.next();
			Mob mob = ally.mob;
			if (!mob.isAlive()) {
				iterator.remove();
				continue;
			}
			ServerPlayer owner = server.getPlayerList().getPlayer(ally.owner);
			if (owner == null || owner.level() != mob.level() || mob.tickCount % 5 != 0) {
				continue;
			}
			if (mob.getTarget() == owner) {
				calm(mob, owner);
			}
			LivingEntity enemy = enemyOf(owner, mob);
			if (enemy != null && mob.getTarget() != enemy) {
				mob.setTarget(enemy);
				if (mob instanceof Warden warden) {
					warden.increaseAngerAt(enemy, 150, false);
				}
			}
			if (mob.getTarget() == null && mob.distanceToSqr(owner) > 10 * 10) {
				mob.getNavigation().moveTo(owner, 1.2);
			}
		}
	}

	private static @Nullable LivingEntity enemyOf(ServerPlayer owner, Mob self) {
		for (LivingEntity candidate : new LivingEntity[] {owner.getLastHurtMob(), owner.getLastHurtByMob()}) {
			if (candidate != null && candidate.isAlive() && candidate != self && candidate.level() == owner.level()
				&& !isAllyOf(owner, candidate) && candidate.distanceToSqr(owner) < 32 * 32) {
				return candidate;
			}
		}
		return null;
	}

	private static void calm(Mob mob, ServerPlayer owner) {
		mob.setTarget(null);
		if (mob instanceof NeutralMob neutral) {
			neutral.stopBeingAngry();
		}
		if (mob instanceof Warden warden) {
			warden.clearAnger(owner);
		}
	}

	static void clear() {
		ACTIVE.clear();
	}

	private record Ally(Mob mob, UUID owner) {
	}

	private Allies() {
	}
}
