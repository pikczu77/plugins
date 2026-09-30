package io.github.pikczu77.hpsize.rec;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Camera operator mode: switches to spectator and later brings the player back
 * to the exact place and game mode they left.
 */
public final class CamMode {
	private static final Map<UUID, Saved> SAVED = new HashMap<>();

	private record Saved(GameType gameType, ResourceKey<Level> dimension, Vec3 position, float yRot, float xRot) {
	}

	private CamMode() {
	}

	/**
	 * @return true if the camera mode was turned on, false if it was turned off
	 */
	public static boolean toggle(ServerPlayer player) {
		Saved saved = SAVED.remove(player.getUUID());

		if (saved == null) {
			SAVED.put(player.getUUID(), new Saved(player.gameMode.getGameModeForPlayer(), player.level().dimension(),
					player.position(), player.getYRot(), player.getXRot()));
			player.setGameMode(GameType.SPECTATOR);
			return true;
		}

		ServerLevel level = player.level().getServer().getLevel(saved.dimension());

		if (level == null) {
			level = (ServerLevel) player.level();
		}

		player.teleportTo(level, saved.position().x, saved.position().y, saved.position().z, Set.of(), saved.yRot(), saved.xRot(), true);
		player.setGameMode(saved.gameType());
		return false;
	}

	public static void reset() {
		SAVED.clear();
	}
}
