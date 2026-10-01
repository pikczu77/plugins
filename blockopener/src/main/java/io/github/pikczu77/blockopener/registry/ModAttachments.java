package io.github.pikczu77.blockopener.registry;

import com.mojang.serialization.Codec;
import io.github.pikczu77.blockopener.BlockOpener;
import io.github.pikczu77.blockopener.settings.ModSettings;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

@SuppressWarnings("UnstableApiUsage")
public final class ModAttachments {
	/** Ids of the secret items this player has found (drives the tracker HUD). */
	public static final AttachmentType<List<String>> FOUND_SECRETS = AttachmentRegistry.create(
		BlockOpener.id("found_secrets"),
		builder -> builder
			.persistent(Codec.STRING.listOf())
			.copyOnDeath()
			.syncWith(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AttachmentSyncPredicate.targetOnly())
	);

	/** Whether the tracker HUD is shown for this player. Absent means shown. */
	public static final AttachmentType<Boolean> TRACKER_HUD = AttachmentRegistry.create(
		BlockOpener.id("tracker_hud"),
		builder -> builder
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
	);

	/** Player is currently disguised as a pumpkin (every client needs it to hide the player model). */
	public static final AttachmentType<Boolean> PUMPKIN_FORM = AttachmentRegistry.create(
		BlockOpener.id("pumpkin_form"),
		builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all())
	);

	/** Diamond Flight toggled on with the flight key. */
	public static final AttachmentType<Boolean> DIAMOND_FLIGHT = AttachmentRegistry.create(
		BlockOpener.id("diamond_flight"),
		builder -> builder
			.persistent(Codec.BOOL)
			.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
	);

	/** Mirror of the extendedItems setting for the client (tracker HUD layout). */
	public static final AttachmentType<Boolean> EXTENDED_MODE = AttachmentRegistry.create(
		BlockOpener.id("extended_mode"),
		builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
	);

	/** World-wide settings, stored on the overworld. */
	public static final AttachmentType<ModSettings> SETTINGS = AttachmentRegistry.create(
		BlockOpener.id("settings"),
		builder -> builder.persistent(ModSettings.CODEC).initializer(() -> ModSettings.DEFAULT)
	);

	public static void init() {
	}

	private ModAttachments() {
	}
}
