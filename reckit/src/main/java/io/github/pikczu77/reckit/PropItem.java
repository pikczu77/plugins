package io.github.pikczu77.reckit;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A prop with no behavior of its own. Only the name color can differ from a plain {@link Item}. */
public class PropItem extends Item {
	private final @Nullable Integer color;

	public PropItem(Properties properties, @Nullable Integer color) {
		super(properties);
		this.color = color;
	}

	@Override
	public Component getName(ItemStack stack) {
		Component name = super.getName(stack);
		return color == null ? name : name.copy().withColor(color);
	}
}
