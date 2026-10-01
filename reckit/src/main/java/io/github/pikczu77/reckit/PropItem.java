package io.github.pikczu77.reckit;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A prop with no behavior of its own. Only the name style can differ from a plain {@link Item}. */
public class PropItem extends Item {
	private final @Nullable Integer color;
	private final boolean bold;

	public PropItem(Properties properties, @Nullable Integer color, boolean bold) {
		super(properties);
		this.color = color;
		this.bold = bold;
	}

	@Override
	public Component getName(ItemStack stack) {
		Component name = super.getName(stack);

		if (color == null && !bold) {
			return name;
		}

		MutableComponent styled = name.copy().withStyle(style -> style.withBold(bold));
		return color == null ? styled : styled.withColor(color);
	}
}
