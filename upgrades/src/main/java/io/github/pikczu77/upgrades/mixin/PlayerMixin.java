package io.github.pikczu77.upgrades.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.attribute.BedRule;

import io.github.pikczu77.upgrades.ability.FistMining;
import io.github.pikczu77.upgrades.ability.Nap;

/**
 * Fists that mine like a pickaxe (both sides, so the client predicts the mining speed), and naps during the day.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@WrapOperation(method = "getDestroySpeed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F"))
	private float upgrades$fistSpeed(ItemStack stack, BlockState state, Operation<Float> original) {
		return FistMining.destroySpeed((Player) (Object) this, stack, state, original.call(stack, state));
	}

	@WrapOperation(method = "hasCorrectToolForDrops", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;isCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean upgrades$fistDrops(ItemStack stack, BlockState state, Operation<Boolean> original) {
		return original.call(stack, state) || FistMining.correctTool((Player) (Object) this, state);
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/attribute/BedRule;canSleep(Lnet/minecraft/world/level/Level;)Z"))
	private boolean upgrades$napAnyTime(BedRule rule, Level level, Operation<Boolean> original) {
		return original.call(rule, level) || Nap.isNapping((Player) (Object) this);
	}
}
