package io.github.pikczu77.blockopener.ability;

import io.github.pikczu77.blockopener.registry.ModAttachments;
import io.github.pikczu77.blockopener.registry.ModItems;
import io.github.pikczu77.blockopener.settings.ModSettings;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

/** Wires every custom item ability into the game events and ticks them for each player. */
public final class ModAbilities {
	private static final Map<UUID, PlayerState> STATES = new HashMap<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ModAbilities::tick);
		UseEntityCallback.EVENT.register(ModAbilities::onUseEntity);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(ModAbilities::allowDamage);
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
			!(entity instanceof ServerPlayer player) || !WeepingTotem.trySave(player, source));
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !TempBlocks.remove(level, pos));
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (entity instanceof ServerPlayer player && source.getEntity() instanceof LivingEntity attacker && attacker != player) {
				BeeSwarm.defend(player, attacker);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			PlayerState state = STATES.remove(handler.getPlayer().getUUID());
			if (state != null) {
				PumpkinBoots.leaveForm(handler.getPlayer(), state);
			}
			BeeSwarm.dismiss(handler.getPlayer().getUUID());
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				PlayerState state = STATES.get(player.getUUID());
				if (state != null) {
					PumpkinBoots.leaveForm(player, state);
				}
			}
			STATES.clear();
			TempBlocks.clear();
			Allies.clear();
			Bombs.clear();
			MegaKick.clear();
			DripstoneRain.clear();
			BeeSwarm.clear();
		});
	}

	static PlayerState state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), uuid -> new PlayerState());
	}

	public static boolean wearing(LivingEntity entity, EquipmentSlot slot, Item item) {
		return entity.getItemBySlot(slot).is(item);
	}

	public static boolean holding(LivingEntity entity, Item item) {
		return entity.getMainHandItem().is(item) || entity.getOffhandItem().is(item);
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PlayerState state = state(player);
			if (!player.isAlive() || player.isSpectator()) {
				PumpkinBoots.leaveForm(player, state);
				state.prevShift = false;
				continue;
			}
			boolean shift = player.getLastClientInput().shift();
			PumpkinBoots.tick(player, state, shift);
			AnvilChestplate.tick(player, state, shift);
			DiamondLeggings.tick(player, state);
			SculkHelmet.tick(player);
			CopperMagnet.tick(player, shift);
			SizeShift.tick(player);
			WeepingTotem.tick(player, state);
			GlassSpyglass.tick(player);
			EnderGloves.tick(player);
			SlimeGloves.tick(player, state, shift);
			IceWand.tick(player);
			MagmaFist.tick(player);
			tickLaunch(player, state);
			state.prevShift = shift;
		}
		syncExtendedMode(server);
		TempBlocks.tick();
		Allies.tick(server);
		Bombs.tick();
		MegaKick.tick();
		DripstoneRain.tick();
		BeeSwarm.tick(server);
	}

	/** Lets the client know how many items the tracker should show. */
	private static void syncExtendedMode(MinecraftServer server) {
		boolean extended = ModSettings.get(server).extendedItems();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.getAttachedOrElse(ModAttachments.EXTENDED_MODE, false) != extended) {
				player.setAttached(ModAttachments.EXTENDED_MODE, extended);
			}
		}
	}

	/** Fall protection after a Turbo Launch lasts until the player lands. */
	private static void tickLaunch(ServerPlayer player, PlayerState state) {
		if (state.launched) {
			state.launchedTicks++;
			if (state.launchedTicks > 4 && (player.onGround() || player.isInWater() || player.onClimbable())) {
				state.launched = false;
				state.noFallUntil = player.level().getGameTime() + 2;
			}
		}
	}

	public static void markLaunched(ServerPlayer player) {
		PlayerState state = state(player);
		state.launched = true;
		state.launchedTicks = 0;
	}

	private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND || player.isSpectator()) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getMainHandItem();
		Entity target = Aim.root(entity);
		if (stack.is(ModItems.PISTON_LAUNCHER) && target instanceof LivingEntity living) {
			if (player instanceof ServerPlayer serverPlayer) {
				MegaKick.kick(serverPlayer, living);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(ModItems.BEE_DRILL) && target instanceof LivingEntity living && !BeeSwarm.isOwnBee(player, living)) {
			if (player instanceof ServerPlayer serverPlayer) {
				BeeSwarm.attack(serverPlayer, living);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(ModItems.MOB_CAGE) && !MobCage.isFull(stack) && MobCage.canCatch(target)) {
			if (player instanceof ServerPlayer serverPlayer && target instanceof Mob mob) {
				MobCage.capture(serverPlayer, stack, mob);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(ModItems.ENDER_GLOVES) && target instanceof LivingEntity living && !player.getCooldowns().isOnCooldown(stack)) {
			if (player instanceof ServerPlayer serverPlayer) {
				EnderGloves.swap(serverPlayer, living);
				player.getCooldowns().addCooldown(stack, 30);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (Explosions.isOwnAbilityExplosion(entity, source)) {
			return false;
		}
		if (entity instanceof Bee bee && source.getEntity() instanceof Player player && BeeSwarm.isOwnBee(player, bee)) {
			return false;
		}
		if (source.getEntity() instanceof Player player && Allies.isAllyOf(player, entity)) {
			// No friendly fire from a jump attack's lightning or a fire nova; a direct hit still lands.
			return source.getDirectEntity() == player && !source.is(DamageTypeTags.IS_LIGHTNING);
		}
		if (entity instanceof ServerPlayer player) {
			if (DripstoneRain.isOwnDripstone(player, source) || StormHammer.isOwnStorm(player, source)) {
				return false;
			}
			if (source.is(DamageTypeTags.IS_FALL)) {
				PlayerState state = state(player);
				if (state.launched || state.slamming || player.level().getGameTime() <= state.noFallUntil) {
					return false;
				}
				if (DiamondLeggings.isFlightOn(player) || SlimeGloves.holding(player)) {
					return false;
				}
			}
		}
		return true;
	}

	private ModAbilities() {
	}
}
