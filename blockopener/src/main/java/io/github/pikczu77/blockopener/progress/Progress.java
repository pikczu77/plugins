package io.github.pikczu77.blockopener.progress;

import io.github.pikczu77.blockopener.registry.ModAttachments;
import io.github.pikczu77.blockopener.settings.ModSettings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Which secret items each player has found, plus the reveal fanfare. */
public final class Progress {
	public static List<String> found(ServerPlayer player) {
		return player.getAttachedOrElse(ModAttachments.FOUND_SECRETS, List.of());
	}

	public static boolean hasFound(ServerPlayer player, SecretItem secret) {
		return found(player).contains(secret.id());
	}

	/** The items in play on this server: the video's 10, or 22 in extended mode. */
	public static List<SecretItem> active(MinecraftServer server) {
		return SecretItem.active(ModSettings.get(server).extendedItems());
	}

	public static int total(ServerPlayer player) {
		return active(player.level().getServer()).size();
	}

	/** Found items that count in the current mode. */
	public static int count(ServerPlayer player) {
		List<String> found = found(player);
		return (int) active(player.level().getServer()).stream().filter(secret -> found.contains(secret.id())).count();
	}

	/** @return true if this was new for the player */
	public static boolean markFound(ServerPlayer player, SecretItem secret) {
		List<String> found = found(player);
		if (found.contains(secret.id())) {
			return false;
		}
		List<String> updated = new ArrayList<>(found);
		updated.add(secret.id());
		player.setAttached(ModAttachments.FOUND_SECRETS, List.copyOf(updated));
		return true;
	}

	public static void reset(ServerPlayer player) {
		player.removeAttached(ModAttachments.FOUND_SECRETS);
	}

	public static void setAll(ServerPlayer player) {
		List<String> all = new ArrayList<>(found(player));
		for (SecretItem secret : active(player.level().getServer())) {
			if (!all.contains(secret.id())) {
				all.add(secret.id());
			}
		}
		player.setAttached(ModAttachments.FOUND_SECRETS, List.copyOf(all));
	}

	/** Called when a secret item pops out of a block the player opened. */
	public static void onSecretFound(ServerPlayer player, SecretItem secret, Vec3 at) {
		boolean first = markFound(player, secret);
		ServerLevel level = player.level();
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, at.x, at.y + 0.3, at.z, first ? 60 : 20, 0.3, 0.4, 0.3, 0.45);
		if (!first) {
			level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
			return;
		}
		reveal(player, secret, true);
	}

	/**
	 * Title + sound + chat line for a newly found item. Also used by {@code /blockopener reveal}
	 * to stage a find for the camera.
	 */
	public static void reveal(ServerPlayer player, SecretItem secret, boolean announce) {
		int count = count(player);
		int total = total(player);
		Component name = itemName(secret);
		player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
		player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("blockopener.reveal.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
		player.connection.send(new ClientboundSetSubtitleTextPacket(
			Component.empty().append(name).append(Component.literal("  " + count + "/" + total).withStyle(ChatFormatting.GRAY))
		));
		player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.9F, 1.0F);

		if (announce && ModSettings.get(player.level().getServer()).announceFinds()) {
			MutableComponent line = Component.literal("✦ ").withColor(secret.color())
				.append(Component.translatable("blockopener.reveal.chat", player.getDisplayName(), name, count, total).withStyle(ChatFormatting.WHITE));
			player.level().getServer().getPlayerList().broadcastSystemMessage(line, false);
		}
		if (count == total) {
			allFound(player, total);
		}
	}

	private static void allFound(ServerPlayer player, int total) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
		player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("blockopener.reveal.all_title", total).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
		player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("blockopener.reveal.all_subtitle").withStyle(ChatFormatting.YELLOW)));
		player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.5F, 1.4F);
		if (ModSettings.get(player.level().getServer()).announceFinds()) {
			player.level().getServer().getPlayerList().broadcastSystemMessage(
				Component.translatable("blockopener.reveal.all_chat", player.getDisplayName(), total).withStyle(ChatFormatting.LIGHT_PURPLE), false
			);
		}
	}

	public static Component itemName(SecretItem secret) {
		return Component.translatable(secret.item().getDescriptionId()).withColor(secret.color()).withStyle(ChatFormatting.BOLD);
	}

	private Progress() {
	}
}
