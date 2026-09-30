package io.github.pikczu77.blockopener.opening;

import com.mojang.math.Transformation;
import io.github.pikczu77.blockopener.progress.Progress;
import io.github.pikczu77.blockopener.progress.SecretItem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Server-side opening animation built from display entities, so it also looks right for players
 * without the mod's resources and is visible in every recording angle:
 * <ol>
 *   <li>a piston head pops the lid off the block,</li>
 *   <li>the loot bursts out of the top (secret items first float up, spinning and glowing),</li>
 *   <li>the opened block shrinks away.</li>
 * </ol>
 */
public final class OpeningAnimations {
	private static final List<Animation> ACTIVE = new ArrayList<>();

	public static void start(ServerLevel level, BlockPos pos, BlockState state, List<ItemStack> loot, @Nullable ServerPlayer player) {
		ACTIVE.add(new Animation(level, pos.immutable(), state, loot, player == null ? null : player.getUUID()));
	}

	public static boolean isAnimating(Level level, BlockPos pos) {
		if (level.isClientSide()) {
			return false;
		}
		for (Animation animation : ACTIVE) {
			if (animation.level == level && animation.pos.equals(pos)) {
				return true;
			}
		}
		return false;
	}

	static void tick(MinecraftServer server) {
		Iterator<Animation> iterator = ACTIVE.iterator();
		while (iterator.hasNext()) {
			Animation animation = iterator.next();
			if (animation.tick()) {
				animation.remove();
				iterator.remove();
			}
		}
	}

	/** Finishes every running animation immediately (loot is still dropped). */
	static void finishAll() {
		for (Animation animation : ACTIVE) {
			animation.popLoot();
			animation.remove();
		}
		ACTIVE.clear();
	}

	private static final class Animation {
		private final ServerLevel level;
		private final BlockPos pos;
		private final BlockState state;
		private final List<ItemStack> loot;
		private final @Nullable UUID playerId;
		private final @Nullable SecretItem secret;
		private final int popTick;
		private final int endTick;
		private final Display.BlockDisplay body;
		private final Display.BlockDisplay lid;
		private Display.@Nullable ItemDisplay reveal;
		private boolean popped;
		private int age;

		Animation(ServerLevel level, BlockPos pos, BlockState state, List<ItemStack> loot, @Nullable UUID playerId) {
			this.level = level;
			this.pos = pos;
			this.state = state;
			this.loot = loot;
			this.playerId = playerId;
			this.secret = loot.stream().map(SecretItem::of).flatMap(Optional::stream).findFirst().orElse(null);
			this.popTick = this.secret != null ? 26 : 5;
			this.endTick = this.popTick + 9;

			this.body = spawnBlock(state, identity());
			BlockState head = Blocks.PISTON_HEAD.defaultBlockState()
				.setValue(PistonHeadBlock.FACING, Direction.UP)
				.setValue(PistonHeadBlock.TYPE, state.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT)
				.setValue(PistonHeadBlock.SHORT, false);
			// The lid sits 0.01 over the block so the faces do not flicker.
			this.lid = spawnBlock(head, new Transformation(new Vector3f(-0.01F, 0.01F, -0.01F), null, new Vector3f(1.02F, 1.0F, 1.02F), null));
		}

		private Display.BlockDisplay spawnBlock(BlockState displayed, Transformation transformation) {
			Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, this.level);
			display.setPos(this.pos.getX(), this.pos.getY(), this.pos.getZ());
			display.setBlockState(displayed);
			display.setTransformation(transformation);
			return TransientEntities.spawn(this.level, display);
		}

		/** @return true once the animation is over */
		boolean tick() {
			this.age++;
			if (this.age == 2) {
				// Piston pushes the lid up and tilts it back.
				float lift = this.secret != null ? 0.85F : 0.55F;
				animate(this.lid, hinged(lift, this.secret != null ? -35.0F : -18.0F), 4);
				spawnRing();
				if (this.secret != null) {
					startReveal();
				}
			}
			if (this.reveal != null) {
				tickReveal();
			}
			if (this.age == this.popTick) {
				popLoot();
			}
			if (this.age == this.popTick + 2) {
				// The opened block shrinks into its centre.
				animate(this.body, shrunk(), 6);
				animate(this.lid, new Transformation(new Vector3f(0.5F, 1.2F, 0.5F), null, new Vector3f(0.0F), null), 6);
			}
			return this.age >= this.endTick;
		}

		private void startReveal() {
			SecretItem secret = this.secret;
			Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, this.level);
			display.setPos(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5);
			display.setItemStack(new ItemStack(secret.item()));
			display.setItemTransform(ItemDisplayContext.FIXED);
			display.setTransformation(new Transformation(null, null, new Vector3f(0.3F), null));
			display.setGlowingTag(true);
			display.setGlowColorOverride(secret.color());
			display.setBrightnessOverride(new Brightness(15, 15));
			this.reveal = TransientEntities.spawn(this.level, display);
			this.level.playSound(null, this.pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 1.2F);
		}

		private void tickReveal() {
			Display.ItemDisplay display = this.reveal;
			int t = this.age - 2;
			if (t >= 1 && t % 6 == 1 && this.age < this.popTick) {
				// Rise out of the block while spinning, one third of a turn every 6 ticks.
				float progress = Math.min(1.0F, t / 18.0F);
				float y = Mth.lerp(progress, 0.0F, 1.1F);
				float scale = Mth.lerp(progress, 0.3F, 0.9F);
				Quaternionf spin = new Quaternionf().rotateY((float) Math.toRadians(120.0 * ((t / 6) + 1)));
				animate(display, new Transformation(new Vector3f(0.0F, y, 0.0F), spin, new Vector3f(scale), null), 6);
			}
			if (this.age < this.popTick && this.age % 2 == 0) {
				Vec3 at = display.position().add(0.0, 0.2 + Math.min(1.1, (this.age - 2) / 18.0 * 1.1), 0.0);
				this.level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 2, 0.25, 0.25, 0.25, 0.02);
			}
			if (this.age == this.popTick) {
				TransientEntities.remove(display);
				this.reveal = null;
			}
		}

		private void spawnRing() {
			// Coloured swirl around the block, tinted like the block itself.
			int color = this.state.getMapColor(this.level, this.pos).col;
			DustParticleOptions dust = new DustParticleOptions(color == 0 ? 0xFFFFFF : color, 1.3F);
			Vec3 center = Vec3.atCenterOf(this.pos);
			for (int i = 0; i < 24; i++) {
				double angle = Math.PI * 2 * i / 24;
				this.level.sendParticles(dust, center.x + Math.cos(angle) * 0.9, center.y + 0.3, center.z + Math.sin(angle) * 0.9, 1, 0, 0, 0, 0);
			}
		}

		void popLoot() {
			if (this.popped) {
				return;
			}
			this.popped = true;
			Vec3 top = new Vec3(this.pos.getX() + 0.5, this.pos.getY() + 0.8, this.pos.getZ() + 0.5);
			for (ItemStack stack : this.loot) {
				ItemEntity item = new ItemEntity(this.level, top.x, top.y, top.z, stack.copy(),
					this.level.random.triangle(0.0, 0.12), 0.3 + this.level.random.nextDouble() * 0.12, this.level.random.triangle(0.0, 0.12));
				item.setPickUpDelay(8);
				this.level.addFreshEntity(item);
			}
			this.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, this.state), top.x, top.y, top.z, 30, 0.3, 0.3, 0.3, 0.1);
			this.level.sendParticles(ParticleTypes.POOF, top.x, top.y + 0.2, top.z, 6, 0.2, 0.1, 0.2, 0.02);
			this.level.playSound(null, this.pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 0.6F);

			if (this.secret != null && this.playerId != null) {
				ServerPlayer player = this.level.getServer().getPlayerList().getPlayer(this.playerId);
				if (player != null) {
					Progress.onSecretFound(player, this.secret, top);
				}
			}
		}

		void remove() {
			for (Display display : new Display[] {this.body, this.lid, this.reveal}) {
				if (display != null) {
					TransientEntities.remove(display);
				}
			}
		}

		/** Lid lifted by {@code lift} and rotated around its back edge by {@code angle} degrees. */
		private static Transformation hinged(float lift, float angle) {
			Quaternionf rotation = new Quaternionf().rotateX((float) Math.toRadians(angle));
			Vector3f hinge = new Vector3f(0.5F, 1.0F, 1.0F);
			Vector3f scale = new Vector3f(1.02F, 1.0F, 1.02F);
			Vector3f rotatedHinge = rotation.transform(new Vector3f(hinge));
			Vector3f translation = new Vector3f(hinge).sub(rotatedHinge).add(-0.01F, lift, -0.01F);
			return new Transformation(translation, rotation, scale, null);
		}

		private static Transformation shrunk() {
			return new Transformation(new Vector3f(0.5F, 0.5F, 0.5F), null, new Vector3f(0.0F), null);
		}

		private static Transformation identity() {
			return new Transformation(null, null, null, null);
		}

		private static void animate(Display display, Transformation transformation, int duration) {
			display.setTransformation(transformation);
			display.setTransformationInterpolationDuration(duration);
			display.setTransformationInterpolationDelay(0);
		}
	}

	private OpeningAnimations() {
	}
}
