package io.github.pikczu77.blockopener.network;

import io.github.pikczu77.blockopener.BlockOpener;
import io.github.pikczu77.blockopener.ability.DiamondLeggings;
import io.github.pikczu77.blockopener.ability.SculkHelmet;
import io.github.pikczu77.blockopener.registry.ModAttachments;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class ModPayloads {
	public static void init() {
		PayloadTypeRegistry.playC2S().register(AbilityKeyPayload.TYPE, AbilityKeyPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(AbilityKeyPayload.TYPE, (payload, context) -> handle(context.player(), payload.ability()));
	}

	private static void handle(ServerPlayer player, Ability ability) {
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}
		switch (ability) {
			case TOGGLE_FLIGHT -> DiamondLeggings.toggleFlight(player);
			case FIREBALL -> DiamondLeggings.fireball(player);
			case SONIC_BOOM -> SculkHelmet.sonicBoom(player);
			case TOGGLE_HUD -> player.setAttached(ModAttachments.TRACKER_HUD, !player.getAttachedOrElse(ModAttachments.TRACKER_HUD, true));
		}
	}

	/** Keys the client reports: the server validates everything (armor worn, cooldowns...). */
	public enum Ability {
		TOGGLE_FLIGHT,
		FIREBALL,
		SONIC_BOOM,
		TOGGLE_HUD
	}

	public record AbilityKeyPayload(Ability ability) implements CustomPacketPayload {
		public static final Type<AbilityKeyPayload> TYPE = new Type<>(BlockOpener.id("ability_key"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AbilityKeyPayload> CODEC = ByteBufCodecs.VAR_INT
			.<RegistryFriendlyByteBuf>cast()
			.map(id -> new AbilityKeyPayload(Ability.values()[Math.floorMod(id, Ability.values().length)]), payload -> payload.ability().ordinal());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private ModPayloads() {
	}
}
