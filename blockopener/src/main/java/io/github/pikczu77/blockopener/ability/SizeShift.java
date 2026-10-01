package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.BlockOpener;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Size Mushroom: giant (x3: longer reach, harder hits, steps over walls) or tiny (x0.3: fits through
 * 1x1 holes, mobs lose sight of you). Uses attribute modifiers, so the size survives relogging.
 */
public final class SizeShift {
	public enum Size {
		NORMAL, GIANT, TINY
	}

	private static final Identifier ID = BlockOpener.id("size_mushroom");
	private static final List<Holder<Attribute>> ATTRIBUTES = List.of(
		Attributes.SCALE, Attributes.STEP_HEIGHT, Attributes.JUMP_STRENGTH, Attributes.ATTACK_DAMAGE,
		Attributes.ENTITY_INTERACTION_RANGE, Attributes.BLOCK_INTERACTION_RANGE, Attributes.SAFE_FALL_DISTANCE, Attributes.MOVEMENT_SPEED
	);

	public static Size current(Player player) {
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		if (scale == null || scale.getModifier(ID) == null) {
			return Size.NORMAL;
		}
		return scale.getModifier(ID).amount() > 0 ? Size.GIANT : Size.TINY;
	}

	/** @return false when there is no room to grow */
	public static boolean set(ServerPlayer player, Size size) {
		if (size == Size.GIANT) {
			AABB giant = player.getBoundingBox().inflate(0.6, 0.0, 0.6).expandTowards(0.0, 3.8, 0.0);
			if (!player.level().noCollision(player, giant)) {
				return false;
			}
		}
		for (Holder<Attribute> attribute : ATTRIBUTES) {
			AttributeInstance instance = player.getAttribute(attribute);
			if (instance != null) {
				instance.removeModifier(ID);
			}
		}
		switch (size) {
			case GIANT -> {
				add(player, Attributes.SCALE, 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
				add(player, Attributes.STEP_HEIGHT, 1.0, AttributeModifier.Operation.ADD_VALUE);
				add(player, Attributes.JUMP_STRENGTH, 0.25, AttributeModifier.Operation.ADD_VALUE);
				add(player, Attributes.ATTACK_DAMAGE, 5.0, AttributeModifier.Operation.ADD_VALUE);
				add(player, Attributes.ENTITY_INTERACTION_RANGE, 3.0, AttributeModifier.Operation.ADD_VALUE);
				add(player, Attributes.BLOCK_INTERACTION_RANGE, 3.0, AttributeModifier.Operation.ADD_VALUE);
				add(player, Attributes.SAFE_FALL_DISTANCE, 10.0, AttributeModifier.Operation.ADD_VALUE);
				add(player, Attributes.MOVEMENT_SPEED, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
			}
			case TINY -> {
				add(player, Attributes.SCALE, -0.7, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
				add(player, Attributes.MOVEMENT_SPEED, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
			}
			case NORMAL -> {
			}
		}
		ServerLevel level = player.level();
		level.sendParticles(ParticleTypes.CRIMSON_SPORE, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.6, 1.0, 0.6, 0.0);
		level.playSound(null, player.blockPosition(), size == Size.GIANT ? SoundEvents.RAVAGER_ROAR : SoundEvents.PLAYER_LEVELUP,
			SoundSource.PLAYERS, 0.7F, size == Size.TINY ? 2.0F : size == Size.GIANT ? 0.8F : 1.2F);
		return true;
	}

	private static void add(ServerPlayer player, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null) {
			instance.addOrReplacePermanentModifier(new AttributeModifier(ID, amount, operation));
		}
	}

	static void tick(ServerPlayer player) {
		if (current(player) == Size.TINY && player.tickCount % 10 == 0) {
			// Too small to be noticed from more than a few blocks away.
			Stealth.loseTrack(player, 4.0, mob -> true);
		}
	}

	private SizeShift() {
	}
}
