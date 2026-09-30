package io.github.pikczu77.hpsize.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;

/**
 * The Ender Dragon's hitbox is made of parts with fixed sizes, which ignore the scale attribute.
 * This makes every part as big as the dragon's scale.
 */
@Mixin(EnderDragonPart.class)
public abstract class EnderDragonPartMixin {
	@Shadow
	@Final
	public EnderDragon parentMob;

	@ModifyReturnValue(method = "getDimensions", at = @At("RETURN"))
	private EntityDimensions hpsize$scaleWithDragon(EntityDimensions dimensions) {
		// The constructor asks for the dimensions before parentMob is set.
		return parentMob == null ? dimensions : dimensions.scale(parentMob.getScale());
	}
}
