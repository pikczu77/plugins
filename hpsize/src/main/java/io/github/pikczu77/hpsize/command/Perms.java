package io.github.pikczu77.hpsize.command;

import java.util.function.Predicate;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class Perms {
	private Perms() {
	}

	/**
	 * Same permission as vanilla /gamemode or /time (operators, or singleplayer with cheats on).
	 */
	static Predicate<CommandSourceStack> gamemaster() {
		return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
	}
}
