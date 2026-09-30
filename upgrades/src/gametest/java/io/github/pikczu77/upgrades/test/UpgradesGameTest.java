package io.github.pikczu77.upgrades.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.pikczu77.upgrades.ability.BonusPassives;
import io.github.pikczu77.upgrades.entity.CombatClone;
import io.github.pikczu77.upgrades.upgrade.Bonus;
import io.github.pikczu77.upgrades.upgrade.Upgrade;
import io.github.pikczu77.upgrades.upgrade.UpgradeManager;

public class UpgradesGameTest {
	/** Every upgrade must point at an advancement that exists, otherwise it could never be unlocked. */
	@GameTest
	public void everyUpgradeHasAnAdvancement(GameTestHelper helper) {
		for (Upgrade upgrade : Upgrade.VALUES) {
			helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(upgrade.advancement) != null,
					"Missing advancement " + upgrade.advancement + " for " + upgrade.id());
		}

		helper.succeed();
	}

	/** Every advancement that shows up in chat unlocks exactly one upgrade ("upgrade yourself infinitely"). */
	@GameTest
	public void everyAdvancementUnlocksSomething(GameTestHelper helper) {
		Set<Identifier> seen = new HashSet<>();

		for (Bonus bonus : Bonus.VALUES) {
			helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(bonus.advancement()) != null,
					"Missing advancement " + bonus.advancement() + " for bonus " + bonus.id());
			helper.assertTrue(seen.add(bonus.advancement()), "Advancement used twice: " + bonus.advancement());
		}

		for (Upgrade upgrade : Upgrade.VALUES) {
			helper.assertTrue(seen.add(upgrade.advancement()), "Advancement used twice: " + upgrade.advancement());
		}

		for (AdvancementHolder holder : helper.getLevel().getServer().getAdvancements().getAllAdvancements()) {
			boolean announced = holder.value().display().map(DisplayInfo::shouldAnnounceChat).orElse(false);

			if (announced) {
				helper.assertTrue(seen.contains(holder.id()), "No upgrade for the advancement " + holder.id());
			}
		}

		helper.succeed();
	}

	/** Stat bonuses become attribute modifiers, and go away again. */
	@GameTest
	public void bonusStats(GameTestHelper helper) {
		FakePlayer player = FakePlayer.get(helper.getLevel());
		double before = player.getMaxHealth();
		long[] bits = Bonus.empty();
		Bonus.set(bits, Bonus.HEART_TRANSPLANTER);
		BonusPassives.apply(player, bits);
		helper.assertTrue(Math.abs(player.getMaxHealth() - (before + 4.0)) < 0.01, "Drugie Serce should add 2 hearts, max health " + player.getMaxHealth());
		BonusPassives.apply(player, Bonus.empty());
		helper.assertTrue(Math.abs(player.getMaxHealth() - before) < 0.01, "The bonus did not go away, max health " + player.getMaxHealth());
		helper.succeed();
	}

	/** Fists mine like the best pickaxe on the shoulder. */
	@GameTest
	public void fistTiers(GameTestHelper helper) {
		helper.assertTrue(Upgrade.fistTier(0L) == 0, "No upgrades, no pickaxe fist");
		helper.assertTrue(Upgrade.fistTier(Upgrade.FIST_PICKAXE.bit()) == 1, "Stone Age gives a wooden pickaxe fist");
		helper.assertTrue(Upgrade.fistTier(Upgrade.FIST_PICKAXE.bit() | Upgrade.VEIN_MINER_3.bit()) == 4, "Diamonds! gives a diamond pickaxe fist");
		helper.assertTrue(Upgrade.veinLevel(Upgrade.VEIN_MINER.bit() | Upgrade.VEIN_MINER_MAX.bit()) == 4, "Vein miner max is level 4");
		helper.assertTrue(Upgrade.cloneLevel(Upgrade.CLONE_1.bit() | Upgrade.CLONE_3.bit()) == 3, "The best clone level counts");
		helper.succeed();
	}

	/** Breaking one block of a vein breaks the connected blocks of the same kind, and nothing else. */
	@GameTest(maxTicks = 40)
	public void veinMinerBreaksTheVein(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FakePlayer player = FakePlayer.get(level);
		UpgradeManager.setMaskForTest(player, Upgrade.FIST_PICKAXE.bit() | Upgrade.VEIN_MINER.bit());

		for (int x = 0; x < 4; x++) {
			helper.setBlock(new BlockPos(x, 1, 1), Blocks.COAL_ORE);
		}

		helper.setBlock(new BlockPos(0, 1, 2), Blocks.STONE);
		player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(0, 1, 1)));

		for (int x = 0; x < 4; x++) {
			helper.assertBlockNotPresent(Blocks.COAL_ORE, x, 1, 1);
		}

		helper.assertBlockPresent(Blocks.STONE, 0, 1, 2);
		UpgradeManager.setMaskForTest(player, 0L);
		helper.succeed();
	}

	/** A monster hurting a player with the clone upgrade makes a clone appear. */
	@GameTest(maxTicks = 60)
	public void monsterHitSpawnsAClone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FakePlayer player = FakePlayer.get(level);
		player.snapTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1.0, 2.5)));
		UpgradeManager.setMaskForTest(player, Upgrade.CLONE_2.bit());
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
		io.github.pikczu77.upgrades.ability.Clones.afterDamage(player, level.damageSources().mobAttack(zombie), 2.0F, 2.0F, false);

		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8.0);
		helper.assertTrue(!level.getEntitiesOfClass(CombatClone.class, area).isEmpty(), "No clone appeared");
		UpgradeManager.setMaskForTest(player, 0L);
		helper.succeed();
	}
}
