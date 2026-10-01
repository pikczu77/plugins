package io.github.pikczu77.blockopener.gametest;

import io.github.pikczu77.blockopener.ability.Allies;
import io.github.pikczu77.blockopener.ability.Explosions;
import io.github.pikczu77.blockopener.ability.IceWand;
import io.github.pikczu77.blockopener.ability.MobCage;
import io.github.pikczu77.blockopener.ability.StormHammer;
import io.github.pikczu77.blockopener.ability.TempBlocks;
import io.github.pikczu77.blockopener.ability.WeepingTotem;
import io.github.pikczu77.blockopener.entity.MossphereEntity;
import io.github.pikczu77.blockopener.opening.BlockOpening;
import io.github.pikczu77.blockopener.opening.OpeningLoot;
import io.github.pikczu77.blockopener.progress.Progress;
import io.github.pikczu77.blockopener.progress.SecretItem;
import io.github.pikczu77.blockopener.registry.ModEntities;
import io.github.pikczu77.blockopener.registry.ModFluids;
import io.github.pikczu77.blockopener.registry.ModItems;
import io.github.pikczu77.blockopener.settings.ModSettings;
import java.util.List;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BlockOpenerGameTests {
	private static final BlockPos POS = new BlockPos(2, 2, 2);

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static List<ItemStack> roll(GameTestHelper helper, BlockState state) {
		return OpeningLoot.roll(helper.getLevel(), helper.absolutePos(POS), state, null, 1);
	}

	@GameTest(maxTicks = 80)
	public void pumpkinPopsOutPumpkinBoots(GameTestHelper helper) {
		floor(helper);
		helper.setBlock(POS, Blocks.PUMPKIN);
		List<ItemStack> loot = BlockOpening.open(helper.getLevel(), helper.absolutePos(POS), null);
		helper.assertTrue(loot.stream().anyMatch(stack -> stack.is(ModItems.PUMPKIN_BOOTS)), "a pumpkin should hide the Pumpkin Boots");
		helper.assertBlockPresent(Blocks.AIR, POS);
		helper.succeedWhen(() -> helper.assertItemEntityCountIs(ModItems.PUMPKIN_BOOTS, POS, 3.0, 1));
	}

	@GameTest
	public void everySecretBlockHidesItsItem(GameTestHelper helper) {
		for (SecretItem secret : SecretItem.VIDEO) {
			for (var block : secret.sourceBlocks()) {
				List<ItemStack> loot = roll(helper, block.defaultBlockState());
				helper.assertTrue(loot.stream().anyMatch(stack -> stack.is(secret.item())),
					block.getName().getString() + " should hide " + secret.id() + " but gave " + loot);
			}
		}
		List<ItemStack> moss = roll(helper, Blocks.MOSS_BLOCK.defaultBlockState());
		helper.assertTrue(moss.getFirst().getCount() >= 3, "moss should give several Mosspheres");
		helper.succeed();
	}

	@GameTest
	public void videoJokesAreKept(GameTestHelper helper) {
		List<ItemStack> iron = roll(helper, Blocks.IRON_BLOCK.defaultBlockState());
		helper.assertTrue(iron.size() == 1 && iron.getFirst().is(Items.IRON_INGOT) && iron.getFirst().getCount() == 1, "iron block gives one iron: " + iron);
		List<ItemStack> gold = roll(helper, Blocks.GOLD_BLOCK.defaultBlockState());
		helper.assertTrue(gold.size() == 1 && gold.getFirst().is(Items.COAL), "gold block gives one coal: " + gold);
		helper.succeed();
	}

	@GameTest
	public void oresHideStructureLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (BlockState ore : List.of(Blocks.DIAMOND_ORE.defaultBlockState(), Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(),
			Blocks.COPPER_ORE.defaultBlockState(), Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState())) {
			helper.assertFalse(OpeningLoot.tableFor(level, ore).equals(OpeningLoot.DEFAULT), ore + " should have its own loot");
			helper.assertFalse(roll(helper, ore).isEmpty(), ore + " should not be empty");
		}
		helper.assertValueEqual(OpeningLoot.tableFor(level, Blocks.DIRT.defaultBlockState()), OpeningLoot.DEFAULT, "dirt uses the default loot");
		helper.assertFalse(roll(helper, Blocks.DIRT.defaultBlockState()).isEmpty(), "default loot is never empty");
		helper.succeed();
	}

	@GameTest
	public void technicalBlocksCannotBeOpened(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(POS);
		helper.assertFalse(BlockOpening.canOpen(level, pos, Blocks.BARRIER.defaultBlockState()), "barrier");
		helper.assertFalse(BlockOpening.canOpen(level, pos, Blocks.END_PORTAL_FRAME.defaultBlockState()), "end portal frame");
		helper.assertFalse(BlockOpening.canOpen(level, pos, Blocks.WATER.defaultBlockState()), "water");
		helper.assertTrue(BlockOpening.canOpen(level, pos, Blocks.BEDROCK.defaultBlockState()), "bedrock CAN be opened");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void waterloggedBlocksLeaveWaterAndChestsSpill(GameTestHelper helper) {
		floor(helper);
		BlockPos stairs = new BlockPos(1, 2, 1);
		helper.setBlock(stairs, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.WATERLOGGED, true));
		BlockOpening.open(helper.getLevel(), helper.absolutePos(stairs), null);
		helper.assertBlockPresent(Blocks.WATER, stairs);

		BlockPos chest = new BlockPos(3, 2, 3);
		helper.setBlock(chest, Blocks.CHEST);
		if (helper.getLevel().getBlockEntity(helper.absolutePos(chest)) instanceof ChestBlockEntity entity) {
			entity.setItem(0, new ItemStack(Items.DIAMOND, 5));
		}
		BlockOpening.open(helper.getLevel(), helper.absolutePos(chest), null);
		helper.succeedWhen(() -> helper.assertItemEntityCountIs(Items.DIAMOND, chest, 3.0, 5));
	}

	@GameTest(maxTicks = 40)
	public void anvilSlamOpensTheBlocksAround(GameTestHelper helper) {
		for (int x = 0; x < 5; x++) {
			for (int z = 0; z < 5; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.BEDROCK);
				helper.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
			}
		}
		Vec3 center = helper.absoluteVec(new Vec3(2.5, 1.5, 2.5));
		int opened = BlockOpening.openArea(helper.getLevel(), center, 2.4, null);
		helper.assertTrue(opened >= 9, "the slam should open the ground around it, opened " + opened);
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 1, 2));
		helper.assertBlockPresent(Blocks.BEDROCK, new BlockPos(2, 0, 2));
		helper.succeedWhen(() -> helper.assertTrue(!helper.getEntities(EntityType.ITEM).isEmpty(), "opened blocks should drop loot"));
	}

	@GameTest(maxTicks = 20)
	public void abilityExplosionsBlowUpItemsLikeTnt(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(POS));
		ItemEntity blownUp = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.GOLD_INGOT));
		level.addFreshEntity(blownUp);
		Explosions.explode(level, null, at, 3.0F, false, true);
		helper.assertTrue(blownUp.isRemoved(), "a TNT-like ability explosion should destroy dropped items");

		ItemEntity kept = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.IRON_INGOT));
		level.addFreshEntity(kept);
		Explosions.explode(level, null, at, 3.0F, false, false);
		helper.assertFalse(kept.isRemoved(), "the anvil slam keeps the loot it just opened");
		helper.succeed();
	}

	@GameTest
	public void extendedModeAddsTwelveItems(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ModSettings before = ModSettings.get(level.getServer());
		helper.assertTrue(roll(helper, Blocks.OBSIDIAN.defaultBlockState()).stream().noneMatch(stack -> stack.is(ModItems.OBSIDIAN_SHIELD)),
			"extended items must not drop in normal mode");
		ModSettings.set(level.getServer(), before.withExtendedItems(true));
		try {
			helper.assertValueEqual(Progress.active(level.getServer()).size(), 22, "items in extended mode");
			for (SecretItem secret : SecretItem.ALL) {
				for (var block : secret.sourceBlocks()) {
					List<ItemStack> loot = roll(helper, block.defaultBlockState());
					helper.assertTrue(loot.stream().anyMatch(stack -> stack.is(secret.item())),
						block.getName().getString() + " should hide " + secret.id() + " in extended mode but gave " + loot);
				}
			}
			List<ItemStack> candleCake = roll(helper, Blocks.RED_CANDLE_CAKE.defaultBlockState());
			helper.assertTrue(candleCake.stream().anyMatch(stack -> stack.is(ModItems.CAKE_OF_LIFE)), "candle cakes count as cake");
		} finally {
			ModSettings.set(level.getServer(), before);
		}
		helper.succeed();
	}

	@GameTest
	public void workstationsHideVillageLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (BlockState state : List.of(Blocks.BLAST_FURNACE.defaultBlockState(), Blocks.LECTERN.defaultBlockState(),
			Blocks.ENCHANTING_TABLE.defaultBlockState(), Blocks.OAK_LEAVES.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState())) {
			helper.assertFalse(OpeningLoot.tableFor(level, state).equals(OpeningLoot.DEFAULT), state + " should have its own loot");
		}
		List<ItemStack> table = roll(helper, Blocks.ENCHANTING_TABLE.defaultBlockState());
		helper.assertTrue(table.stream().anyMatch(stack -> stack.is(Items.ENCHANTED_BOOK)), "enchanting table gives a book: " + table);
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void mobCageCatchesAndReleasesAnAlly(GameTestHelper helper) {
		floor(helper);
		FakePlayer player = FakePlayer.get(helper.getLevel());
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, POS);
		zombie.setCustomName(Component.literal("Bob"));
		ItemStack cage = new ItemStack(ModItems.MOB_CAGE);
		helper.assertTrue(MobCage.capture(player, cage, zombie), "the cage should catch the zombie");
		helper.assertTrue(zombie.isRemoved(), "the caught zombie leaves the world");
		helper.assertValueEqual(MobCage.capturedType(cage), "minecraft:zombie", "caught type");
		helper.assertFalse(MobCage.capture(player, cage, helper.spawn(EntityType.PIG, POS)), "a full cage cannot catch another mob");

		Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(1, 2, 1)));
		helper.assertTrue(MobCage.release(player, cage, at), "the cage should release the zombie");
		helper.assertFalse(MobCage.isFull(cage), "the cage is empty again");
		Zombie released = helper.getLevel().getEntitiesOfClass(Zombie.class, new AABB(at, at).inflate(1.0)).stream().findFirst().orElse(null);
		helper.assertTrue(released != null && "Bob".equals(released.getCustomName().getString()), "the same zombie comes back");
		helper.assertTrue(Allies.isAllyOf(player, released), "released mobs are allies");
		helper.assertTrue(released.isPersistenceRequired(), "released mobs never despawn");
		helper.succeed();
	}

	@GameTest
	public void weepingTotemCheatsDeathOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FakePlayer player = FakePlayer.get(level);
		player.getInventory().clearContent();
		player.getInventory().setItem(20, new ItemStack(ModItems.WEEPING_TOTEM));
		player.setHealth(1.0F);
		helper.assertTrue(WeepingTotem.trySave(player, level.damageSources().fall()), "the totem should save the player from the inventory");
		helper.assertTrue(player.getHealth() >= 10.0F, "the totem heals");
		helper.assertTrue(player.getInventory().getItem(20).isEmpty(), "the totem is used up");
		helper.assertFalse(WeepingTotem.trySave(player, level.damageSources().fall()), "only once");
		player.getInventory().setItem(20, new ItemStack(ModItems.WEEPING_TOTEM));
		helper.assertFalse(WeepingTotem.trySave(player, level.damageSources().genericKill()), "/kill still works");
		player.getInventory().clearContent();
		player.removeAllEffects();
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void temporaryBlocksDisappearAndCannotBeOpened(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos dome = new BlockPos(1, 2, 1);
		BlockPos lava = new BlockPos(3, 2, 3);
		helper.setBlock(lava, Blocks.LAVA);
		helper.assertTrue(TempBlocks.place(level, helper.absolutePos(dome), Blocks.OBSIDIAN.defaultBlockState(), 10), "placed into air");
		helper.assertFalse(TempBlocks.place(level, helper.absolutePos(lava), Blocks.OBSIDIAN.defaultBlockState(), 10), "place() never covers fluids");
		helper.assertTrue(TempBlocks.placeOverSource(level, helper.absolutePos(lava), Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.LAVA, 10), "lava crust");
		helper.assertFalse(BlockOpening.canOpen(level, helper.absolutePos(dome), helper.getBlockState(dome)), "temporary blocks hide no loot");
		helper.assertBlockPresent(Blocks.OBSIDIAN, dome);
		helper.assertBlockPresent(Blocks.MAGMA_BLOCK, lava);
		helper.succeedWhen(() -> {
			helper.assertBlockPresent(Blocks.AIR, dome);
			helper.assertBlockPresent(Blocks.LAVA, lava);
		});
	}

	@GameTest(maxTicks = 20)
	public void stormHammerCallsLightning(GameTestHelper helper) {
		floor(helper);
		FakePlayer player = FakePlayer.get(helper.getLevel());
		StormHammer.strike(player, helper.absoluteVec(Vec3.atBottomCenterOf(POS)));
		LightningBolt bolt = helper.getLevel().getEntitiesOfClass(LightningBolt.class, new AABB(helper.absolutePos(POS)).inflate(2.0)).stream()
			.findFirst().orElse(null);
		helper.assertTrue(bolt != null && bolt.getCause() == player, "a lightning bolt owned by the player");
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void iceWandFreezesInIce(GameTestHelper helper) {
		floor(helper);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, POS);
		IceWand.freeze(null, zombie);
		helper.assertTrue(zombie.hasEffect(MobEffects.SLOWNESS), "frozen mobs cannot move");
		helper.assertTrue(zombie.isFullyFrozen(), "frozen mobs shiver");
		helper.assertBlockPresent(Blocks.ICE, POS.east());
		helper.assertBlockPresent(Blocks.ICE, POS.above(2));
		helper.succeed();
	}

	@GameTest(maxTicks = 60)
	public void mossphereKillsInstantly(GameTestHelper helper) {
		floor(helper);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, POS);
		zombie.setNoAi(true);
		MossphereEntity ball = new MossphereEntity(ModEntities.MOSSPHERE, helper.getLevel());
		Vec3 above = helper.absoluteVec(Vec3.atBottomCenterOf(POS).add(0.0, 3.0, 0.0));
		ball.setPos(above.x, above.y, above.z);
		ball.setDeltaMovement(0.0, -1.0, 0.0);
		helper.getLevel().addFreshEntity(ball);
		helper.succeedWhen(() -> helper.assertTrue(!zombie.isAlive(), "zombie should be dead"));
	}

	@GameTest(maxTicks = 80)
	public void voidWaterHurtsAndEatsItems(GameTestHelper helper) {
		floor(helper);
		helper.setBlock(POS, ModFluids.VOID_WATER.defaultFluidState().createLegacyBlock());
		Pig pig = helper.spawn(EntityType.PIG, POS);
		float health = pig.getHealth();
		Vec3 at = helper.absoluteVec(Vec3.atCenterOf(POS));
		ItemEntity item = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(Items.DIAMOND));
		helper.getLevel().addFreshEntity(item);
		helper.succeedWhen(() -> {
			helper.assertTrue(item.isRemoved(), "void water should swallow the item");
			helper.assertTrue(!pig.isAlive() || pig.getHealth() < health, "void water should hurt the pig");
		});
	}

	@GameTest
	public void voidWaterIgnoresNormalBuckets(GameTestHelper helper) {
		helper.setBlock(POS, ModFluids.VOID_WATER.defaultFluidState().createLegacyBlock());
		BlockState state = helper.getBlockState(POS);
		helper.assertTrue(state.is(ModFluids.VOID_WATER_BLOCK) && state.getValue(BlockStateProperties.LEVEL) == 0, "void water source placed");
		ItemStack picked = ((net.minecraft.world.level.block.BucketPickup) state.getBlock())
			.pickupBlock(null, helper.getLevel(), helper.absolutePos(POS), state);
		helper.assertTrue(picked.isEmpty(), "a normal bucket cannot pick up void water");
		helper.assertBlockPresent(ModFluids.VOID_WATER_BLOCK, POS);
		helper.succeed();
	}
}
