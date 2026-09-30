package io.github.pikczu77.blockopener.ability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Primed TNT look-alikes (explosive pumpkins, TNT cannon shots) that explode through {@link Explosions}
 * a tick before vanilla would, so they spare their owner and follow the mod settings.
 */
public final class Bombs {
	private static final List<Bomb> ACTIVE = new ArrayList<>();

	public static PrimedTnt spawn(ServerLevel level, LivingEntity owner, Vec3 at, Vec3 velocity, BlockState look, int fuse, float power) {
		PrimedTnt tnt = new PrimedTnt(level, at.x, at.y, at.z, owner);
		tnt.setDeltaMovement(velocity);
		tnt.setFuse(fuse);
		tnt.setBlockState(look);
		Explosions.markOwned(tnt);
		level.addFreshEntity(tnt);
		ACTIVE.add(new Bomb(tnt, owner, power));
		return tnt;
	}

	static void tick() {
		Iterator<Bomb> iterator = ACTIVE.iterator();
		while (iterator.hasNext()) {
			Bomb bomb = iterator.next();
			PrimedTnt tnt = bomb.tnt;
			if (tnt.isRemoved()) {
				iterator.remove();
			} else if (tnt.getFuse() <= 1) {
				iterator.remove();
				tnt.discard();
				Explosions.explode((ServerLevel) tnt.level(), bomb.owner, tnt.position().add(0.0, 0.0625, 0.0), bomb.power);
			}
		}
	}

	static void clear() {
		ACTIVE.clear();
	}

	private record Bomb(PrimedTnt tnt, LivingEntity owner, float power) {
	}

	private Bombs() {
	}
}
