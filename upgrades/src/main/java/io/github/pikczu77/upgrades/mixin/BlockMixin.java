package io.github.pikczu77.upgrades.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import io.github.pikczu77.upgrades.ability.Enchanted;

@Mixin(Block.class)
public abstract class BlockMixin {
	@Unique
	private static boolean upgrades$copying;

	@Shadow
	private static void popResource(Level level, Supplier<ItemEntity> supplier, ItemStack stack) {
		throw new AssertionError();
	}

	/** Enchanted body: block drops come out several times. */
	@Inject(method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V",
			at = @At("HEAD"))
	private static void upgrades$moreDrops(Level level, Supplier<ItemEntity> supplier, ItemStack stack, CallbackInfo ci) {
		if (upgrades$copying || stack.isEmpty()) {
			return;
		}

		int copies = Enchanted.blockCopies(level.getRandom());

		if (copies <= 1) {
			return;
		}

		upgrades$copying = true;

		try {
			for (int i = 1; i < copies; i++) {
				popResource(level, supplier, stack.copy());
			}
		} finally {
			upgrades$copying = false;
		}
	}
}
