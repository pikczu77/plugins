package io.github.pikczu77.upgrades.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import io.github.pikczu77.upgrades.Upgrades;

/**
 * Packets between the server and clients that have the mod. Vanilla clients can still join, they just do not see the
 * body parts and cannot use the empty-hand powers.
 */
public final class Payloads {
	private Payloads() {
	}

	/**
	 * Server → client: the upgrades of one player (the body parts to draw, and the fist mining tier of the local player).
	 * {@code selected} holds the selected power of each input group (ordinal + 1, 0 = none) packed per 8 bits, the two
	 * bonus longs hold the unlocked bonuses, and {@code secondLifeReady} hides the pocket totem while it recharges.
	 */
	public record SyncUpgrades(int entityId, long mask, int selected, long bonusesLow, long bonusesHigh, boolean secondLifeReady)
			implements CustomPacketPayload {
		public static final Type<SyncUpgrades> TYPE = new Type<>(Upgrades.id("sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SyncUpgrades> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, SyncUpgrades::entityId,
				ByteBufCodecs.VAR_LONG, SyncUpgrades::mask,
				ByteBufCodecs.VAR_INT, SyncUpgrades::selected,
				ByteBufCodecs.LONG, SyncUpgrades::bonusesLow,
				ByteBufCodecs.LONG, SyncUpgrades::bonusesHigh,
				ByteBufCodecs.BOOL, SyncUpgrades::secondLifeReady,
				SyncUpgrades::new);

		@Override
		public Type<SyncUpgrades> type() {
			return TYPE;
		}
	}

	/** Server → client: show the unlock banner for an upgrade ({@code count} of {@code total} unlocked now). */
	public record UnlockBanner(String upgrade, int count, int total) implements CustomPacketPayload {
		public static final Type<UnlockBanner> TYPE = new Type<>(Upgrades.id("unlock_banner"));
		public static final StreamCodec<RegistryFriendlyByteBuf, UnlockBanner> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, UnlockBanner::upgrade,
				ByteBufCodecs.VAR_INT, UnlockBanner::count,
				ByteBufCodecs.VAR_INT, UnlockBanner::total,
				UnlockBanner::new);

		@Override
		public Type<UnlockBanner> type() {
			return TYPE;
		}
	}

	/**
	 * Client → server: right-click with an empty hand that vanilla did not use for anything.
	 * {@code target} is the looked-at entity id, or -1.
	 */
	public record UsePower(boolean sneaking, int target) implements CustomPacketPayload {
		public static final Type<UsePower> TYPE = new Type<>(Upgrades.id("use_power"));
		public static final StreamCodec<RegistryFriendlyByteBuf, UsePower> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, UsePower::sneaking,
				ByteBufCodecs.VAR_INT, UsePower::target,
				UsePower::new);

		@Override
		public Type<UsePower> type() {
			return TYPE;
		}
	}

	/** Client → server: the power key was pressed, select the next power of the group. */
	public record CyclePower(boolean sneaking) implements CustomPacketPayload {
		public static final Type<CyclePower> TYPE = new Type<>(Upgrades.id("cycle_power"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CyclePower> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, CyclePower::sneaking,
				CyclePower::new);

		@Override
		public Type<CyclePower> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(SyncUpgrades.TYPE, SyncUpgrades.CODEC);
		PayloadTypeRegistry.playS2C().register(UnlockBanner.TYPE, UnlockBanner.CODEC);
		PayloadTypeRegistry.playC2S().register(UsePower.TYPE, UsePower.CODEC);
		PayloadTypeRegistry.playC2S().register(CyclePower.TYPE, CyclePower.CODEC);
	}
}
