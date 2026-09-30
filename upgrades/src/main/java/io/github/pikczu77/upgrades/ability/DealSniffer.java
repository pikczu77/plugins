package io.github.pikczu77.upgrades.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import io.github.pikczu77.upgrades.entity.CombatClone;
import io.github.pikczu77.upgrades.util.Screens;
import io.github.pikczu77.upgrades.util.TempDisplays;

/**
 * The villager nose: sneak + right-click sniffs out a nearby mob or block that wants to trade. It glows for a short
 * time; right-click it to see its (random, often terrible) offers. Costs are taken from what you carry.
 */
public final class DealSniffer {
	private static final int OFFER_TICKS = 30 * 20;
	private static final double MOB_RANGE = 16.0;
	private static final int BLOCK_RANGE = 6;

	private static final class Deal {
		final ResourceKey<Level> dimension;
		final @Nullable UUID entity;
		final @Nullable BlockPos block;
		final Component name;
		final MerchantOffers offers;
		final long expires;
		@Nullable UUID outline;

		Deal(ResourceKey<Level> dimension, @Nullable UUID entity, @Nullable BlockPos block, Component name, MerchantOffers offers, long expires) {
			this.dimension = dimension;
			this.entity = entity;
			this.block = block;
			this.name = name;
			this.offers = offers;
			this.expires = expires;
		}
	}

	private record Reward(Item item, int min, int max, int weight) {
	}

	/** What sniffed traders give, mostly junk. */
	private static final List<Reward> REWARDS = List.of(
			new Reward(Items.DIRT, 1, 16, 8), new Reward(Items.ROTTEN_FLESH, 1, 6, 6), new Reward(Items.STICK, 2, 8, 6),
			new Reward(Items.FLINT, 1, 4, 6), new Reward(Items.STRING, 1, 3, 6), new Reward(Items.ARROW, 2, 8, 6),
			new Reward(Items.CHICKEN, 1, 3, 5), new Reward(Items.BONE, 1, 4, 5), new Reward(Items.FEATHER, 1, 4, 4),
			new Reward(Items.GRAVEL, 2, 12, 4), new Reward(Items.WHEAT_SEEDS, 2, 8, 4),
			new Reward(Items.IRON_INGOT, 1, 3, 4), new Reward(Items.GOLD_INGOT, 1, 3, 3), new Reward(Items.BREAD, 2, 5, 4),
			new Reward(Items.COAL, 2, 6, 4), new Reward(Items.LEATHER, 1, 3, 3), new Reward(Items.SUGAR_CANE, 1, 4, 3),
			new Reward(Items.BOOK, 1, 1, 2), new Reward(Items.LAPIS_LAZULI, 2, 6, 3), new Reward(Items.REDSTONE, 2, 8, 3),
			new Reward(Items.EXPERIENCE_BOTTLE, 1, 4, 2),
			new Reward(Items.GOLDEN_APPLE, 1, 2, 2), new Reward(Items.DIAMOND, 1, 1, 1), new Reward(Items.EMERALD, 1, 3, 2),
			new Reward(Items.ENDER_PEARL, 1, 2, 1), new Reward(Items.OBSIDIAN, 1, 3, 1), new Reward(Items.NAME_TAG, 1, 1, 1));

	private static final Map<UUID, Deal> DEALS = new HashMap<>();

	private DealSniffer() {
	}

	public static void sniff(ServerPlayer player) {
		ServerLevel level = player.level();
		RandomSource random = player.getRandom();
		clear(player);

		List<LivingEntity> mobs = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(MOB_RANGE),
				entity -> entity != player && entity.isAlive() && !(entity instanceof Player) && !(entity instanceof CombatClone)
						&& entity instanceof Mob);
		Deal deal = null;

		if (!mobs.isEmpty() && random.nextFloat() < 0.6F) {
			LivingEntity mob = mobs.get(random.nextInt(mobs.size()));
			deal = new Deal(level.dimension(), mob.getUUID(), null, mob.getDisplayName(), offers(player, mobLoot(level, mob)),
					level.getGameTime() + OFFER_TICKS);
			mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, OFFER_TICKS, 0, false, false));
			trail(level, player, mob.getBoundingBox().getCenter());
		} else {
			BlockPos pos = randomBlock(level, player, random);

			if (pos != null) {
				BlockState state = level.getBlockState(pos);
				ItemStack own = new ItemStack(state.getBlock().asItem(), 1 + random.nextInt(8));
				deal = new Deal(level.dimension(), null, pos, state.getBlock().getName(), offers(player, own.isEmpty() ? List.of() : List.of(own)),
						level.getGameTime() + OFFER_TICKS);
				Display.BlockDisplay outline = TempDisplays.box(level, state, pos.getX() - 0.01, pos.getY() - 0.01, pos.getZ() - 0.01,
						pos.getX() + 1.01, pos.getY() + 1.01, pos.getZ() + 1.01, true);
				outline.setGlowingTag(true);
				outline.setGlowColorOverride(0x55FF55);
				deal.outline = outline.getUUID();
				trail(level, player, Vec3.atCenterOf(pos));
			}
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNIFFER_SNIFFING, SoundSource.PLAYERS, 1.0F, 1.4F);

		if (deal == null) {
			Screens.actionBar(player, Component.literal("Nic tu nie pachnie handlem...").withStyle(ChatFormatting.GRAY));
			return;
		}

		DEALS.put(player.getUUID(), deal);
		Screens.actionBar(player, Component.literal("Wywęszono handel: ").withStyle(ChatFormatting.GREEN)
				.append(deal.name.copy().withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(" (PPM, żeby handlować)").withStyle(ChatFormatting.GRAY)));
	}

	private static @Nullable BlockPos randomBlock(ServerLevel level, ServerPlayer player, RandomSource random) {
		BlockPos origin = player.blockPosition();

		for (int attempt = 0; attempt < 40; attempt++) {
			BlockPos pos = origin.offset(random.nextInt(BLOCK_RANGE * 2 + 1) - BLOCK_RANGE, random.nextInt(5) - 2,
					random.nextInt(BLOCK_RANGE * 2 + 1) - BLOCK_RANGE);
			BlockState state = level.getBlockState(pos);

			// A visible, ordinary block.
			if (!state.isAir() && state.isSolidRender() && level.getBlockState(pos.above()).isAir() && state.getDestroySpeed(level, pos) >= 0.0F) {
				return pos;
			}
		}

		return null;
	}

	private static List<ItemStack> mobLoot(ServerLevel level, LivingEntity mob) {
		Optional<ResourceKey<LootTable>> key = mob.getLootTable();

		if (key.isEmpty()) {
			return List.of();
		}

		LootTable table = level.getServer().reloadableRegistries().getLootTable(key.get());
		LootParams params = new LootParams.Builder(level)
				.withParameter(LootContextParams.THIS_ENTITY, mob)
				.withParameter(LootContextParams.ORIGIN, mob.position())
				.withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
				.create(LootContextParamSets.ENTITY);
		List<ItemStack> loot = new ArrayList<>();

		for (ItemStack stack : table.getRandomItems(params)) {
			if (!stack.isEmpty()) {
				loot.add(stack);
			}
		}

		return loot;
	}

	/** 3-5 offers: costs picked from the player's inventory, rewards from the trader's own stuff or the junk pile. */
	private static MerchantOffers offers(ServerPlayer player, List<ItemStack> themed) {
		RandomSource random = player.getRandom();
		MerchantOffers offers = new MerchantOffers();
		List<ItemStack> carried = new ArrayList<>();

		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (!stack.isEmpty()) {
				carried.add(stack);
			}
		}

		int count = 3 + random.nextInt(3);

		for (int i = 0; i < count; i++) {
			ItemStack reward = i < themed.size() && random.nextBoolean() ? themed.get(i).copy() : randomReward(random);
			ItemCost cost;

			if (carried.isEmpty()) {
				cost = new ItemCost(random.nextBoolean() ? Items.DIRT : Items.COBBLESTONE, 1 + random.nextInt(8));
			} else {
				ItemStack wanted = carried.get(random.nextInt(carried.size()));
				int amount = 1 + random.nextInt(Math.min(wanted.getMaxStackSize(), Math.max(1, wanted.getCount() * 3 / 2)));
				cost = new ItemCost(wanted.getItem(), Math.min(amount, wanted.getMaxStackSize()));
			}

			offers.add(new MerchantOffer(cost, Optional.empty(), reward, 1 + random.nextInt(3), 0, 0.0F));
		}

		return offers;
	}

	private static ItemStack randomReward(RandomSource random) {
		int total = REWARDS.stream().mapToInt(Reward::weight).sum();
		int roll = random.nextInt(total);

		for (Reward reward : REWARDS) {
			roll -= reward.weight();

			if (roll < 0) {
				return new ItemStack(reward.item(), reward.min() + random.nextInt(reward.max() - reward.min() + 1));
			}
		}

		return new ItemStack(Items.DIRT);
	}

	private static void trail(ServerLevel level, ServerPlayer player, Vec3 target) {
		Vec3 start = player.getEyePosition();
		Vec3 step = target.subtract(start);
		int points = (int) Math.max(4, step.length() * 2);

		for (int i = 0; i <= points; i++) {
			Vec3 point = start.add(step.scale(i / (double) points));
			level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, false, false, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
		}
	}

	/** Opens the trades when the player clicks their sniffed mob (from the empty-hand packet). */
	public static boolean tryOpen(ServerPlayer player, @Nullable Entity target) {
		Deal deal = DEALS.get(player.getUUID());

		if (deal == null) {
			return false;
		}

		if (target != null && target.getUUID().equals(deal.entity)) {
			return open(player, deal);
		}

		if (deal.block != null) {
			HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);

			if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK && blockHit.getBlockPos().equals(deal.block)) {
				return open(player, deal);
			}
		}

		return false;
	}

	public static InteractionResult useEntity(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hit) {
		if (!(player instanceof ServerPlayer serverPlayer) || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		Deal deal = DEALS.get(player.getUUID());
		return deal != null && entity.getUUID().equals(deal.entity) && open(serverPlayer, deal) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	public static InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer serverPlayer) || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		Deal deal = DEALS.get(player.getUUID());
		return deal != null && hit.getBlockPos().equals(deal.block) && open(serverPlayer, deal) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	private static boolean open(ServerPlayer player, Deal deal) {
		if (player.level().dimension() != deal.dimension) {
			return false;
		}

		SniffedMerchant merchant = new SniffedMerchant(deal.offers, trader -> DEALS.get(trader.getUUID()) == deal && near(trader, deal));
		merchant.setTradingPlayer(player);
		merchant.openTradingScreen(player, deal.name, 1);
		return true;
	}

	private static boolean near(Player player, Deal deal) {
		if (deal.block != null) {
			return player.distanceToSqr(Vec3.atCenterOf(deal.block)) < 64.0;
		}

		Entity entity = player.level() instanceof ServerLevel level && deal.entity != null ? level.getEntity(deal.entity) : null;
		return entity != null && entity.isAlive() && player.distanceToSqr(entity) < 64.0;
	}

	public static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Deal>> iterator = DEALS.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<UUID, Deal> entry = iterator.next();
			ServerLevel level = server.getLevel(entry.getValue().dimension);

			if (level == null || level.getGameTime() >= entry.getValue().expires) {
				iterator.remove();
				removeOutline(server, entry.getValue());
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

				if (player != null) {
					if (player.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu) {
						player.closeContainer();
					}

					Screens.actionBar(player, Component.literal("Oferta wygasła!").withStyle(ChatFormatting.GRAY));
				}
			}
		}
	}

	private static void removeOutline(MinecraftServer server, Deal deal) {
		ServerLevel level = server.getLevel(deal.dimension);

		if (level != null && deal.outline != null) {
			TempDisplays.remove(level, deal.outline);
		}
	}

	private static void clear(ServerPlayer player) {
		Deal old = DEALS.remove(player.getUUID());

		if (old != null) {
			removeOutline(player.level().getServer(), old);
		}
	}

	public static void forget(ServerPlayer player) {
		clear(player);
	}

	public static void reset() {
		DEALS.clear();
	}
}
