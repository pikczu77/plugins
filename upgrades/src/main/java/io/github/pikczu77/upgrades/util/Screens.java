package io.github.pikczu77.upgrades.util;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Titles, action bar texts and sounds sent straight to players (works with vanilla clients).
 */
public final class Screens {
	private Screens() {
	}

	public static void title(Collection<ServerPlayer> players, Component title, @Nullable Component subtitle, int fadeIn, int stay, int fadeOut) {
		for (ServerPlayer player : players) {
			player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
			// Always send a subtitle, otherwise the previous one would stay on screen.
			player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle == null ? Component.empty() : subtitle));
			player.connection.send(new ClientboundSetTitleTextPacket(title));
		}
	}

	public static void actionBar(ServerPlayer player, Component text) {
		player.connection.send(new ClientboundSetActionBarTextPacket(text));
	}

	public static void sound(Collection<ServerPlayer> players, Holder<SoundEvent> sound, float volume, float pitch) {
		for (ServerPlayer player : players) {
			player.connection.send(new ClientboundSoundPacket(sound, SoundSource.MASTER,
					player.getX(), player.getEyeY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
		}
	}

	// SoundEvents mixes plain SoundEvent fields and Holder fields, these overloads accept both.
	public static Holder<SoundEvent> sound(SoundEvent sound) {
		return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
	}

	public static Holder<SoundEvent> sound(Holder<SoundEvent> sound) {
		return sound;
	}
}
