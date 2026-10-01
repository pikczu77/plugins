package io.github.pikczu77.blockopener.command;

import com.mojang.math.Transformation;
import io.github.pikczu77.blockopener.opening.TransientEntities;
import io.github.pikczu77.blockopener.progress.Progress;
import io.github.pikczu77.blockopener.progress.SecretItem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Temporary display entities for recording: highlighted blocks ({@code locate}) and the item showcase. */
public final class Markers {
	private static final List<Marker> ACTIVE = new ArrayList<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> clear());
	}

	/** A glowing copy of the block, visible through walls, for {@code seconds}. */
	static void highlight(ServerLevel level, BlockPos pos, BlockState state, int color, int seconds) {
		highlight(level, pos, state, color, seconds * 20, false);
	}

	/** Same as above with the duration in ticks (used by the Glass Spyglass, refreshed every second). */
	public static void highlight(ServerLevel level, BlockPos pos, BlockState state, int color, int ticks, boolean inTicks) {
		Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
		display.setPos(pos.getX(), pos.getY(), pos.getZ());
		display.setBlockState(state);
		display.setTransformation(new Transformation(new Vector3f(-0.005F), null, new Vector3f(1.01F), null));
		display.setGlowingTag(true);
		display.setGlowColorOverride(color);
		display.setViewRange(8.0F);
		TransientEntities.spawn(level, display);
		ACTIVE.add(new Marker(display, level.getGameTime() + ticks, false));
	}

	/**
	 * The secret items floating and spinning in an arc in front of the player, each with its name,
	 * like the intro shot of the video. In extended mode the 12 extra items get a second, higher arc.
	 */
	static void showcase(ServerPlayer player, int seconds) {
		ServerLevel level = player.level();
		long expires = level.getGameTime() + seconds * 20L;
		boolean extended = Progress.active(level.getServer()).size() > SecretItem.COUNT;
		showcaseArc(level, player, SecretItem.VIDEO, 7.0, 1.2, 8.5, expires);
		if (extended) {
			List<SecretItem> extra = SecretItem.ALL.stream().filter(SecretItem::extended).toList();
			showcaseArc(level, player, extra, 9.5, 2.9, 6.0, expires);
		}
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 80, 3.0, 1.0, 3.0, 0.3);
	}

	private static void showcaseArc(ServerLevel level, ServerPlayer player, List<SecretItem> secrets, double radius, double height,
		double spacing, long expires) {
		float yaw = player.getYRot();
		for (int i = 0; i < secrets.size(); i++) {
			SecretItem secret = secrets.get(i);
			double angle = Math.toRadians(yaw + (i - (secrets.size() - 1) / 2.0) * spacing);
			Vec3 at = player.position().add(-Math.sin(angle) * radius, height, Math.cos(angle) * radius);

			Display.ItemDisplay item = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
			item.setPos(at.x, at.y, at.z);
			item.setYRot(yaw);
			item.setItemStack(new ItemStack(secret.item()));
			item.setItemTransform(ItemDisplayContext.FIXED);
			item.setTransformation(new Transformation(null, null, new Vector3f(0.9F), null));
			item.setBrightnessOverride(new Brightness(15, 15));
			item.setGlowingTag(true);
			item.setGlowColorOverride(secret.color());
			TransientEntities.spawn(level, item);
			ACTIVE.add(new Marker(item, expires, true));

			Display.TextDisplay label = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
			label.setPos(at.x, at.y + (i % 2 == 0 ? 0.7 : 1.05), at.z);
			label.setText(Progress.itemName(secret));
			label.setBillboardConstraints(Display.BillboardConstraints.CENTER);
			label.setTransformation(new Transformation(null, null, new Vector3f(0.5F), null));
			label.setBackgroundColor(0x60000000);
			TransientEntities.spawn(level, label);
			ACTIVE.add(new Marker(label, expires, false));
		}
	}

	static int clear() {
		int count = ACTIVE.size();
		ACTIVE.forEach(marker -> TransientEntities.remove(marker.display));
		ACTIVE.clear();
		return count;
	}

	private static void tick() {
		Iterator<Marker> iterator = ACTIVE.iterator();
		while (iterator.hasNext()) {
			Marker marker = iterator.next();
			Display display = marker.display;
			if (display.isRemoved() || display.level().getGameTime() > marker.expires) {
				TransientEntities.remove(display);
				iterator.remove();
			} else if (marker.spin && display.tickCount % 10 == 2) {
				// Sway left and right (always readable on camera) while bobbing up and down.
				float angle = (float) Math.sin(display.tickCount / 16.0) * 0.6F;
				float bob = (float) Math.sin(display.tickCount / 10.0) * 0.08F;
				display.setTransformation(new Transformation(new Vector3f(0.0F, bob, 0.0F), new Quaternionf().rotateY(angle), new Vector3f(0.9F), null));
				display.setTransformationInterpolationDuration(10);
				display.setTransformationInterpolationDelay(0);
			}
		}
	}

	private record Marker(Display display, long expires, boolean spin) {
	}

	private Markers() {
	}
}
