package io.github.pikczu77.upgrades.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
	@Accessor("baseDamage")
	double upgrades$getBaseDamage();

	@Invoker("setPierceLevel")
	void upgrades$setPierceLevel(byte level);
}
