package io.github.pikczu77.blockopener.item;

import net.minecraft.world.item.Item;

/**
 * Right-click any block to open it. The opening itself runs from a block-use callback in
 * {@link io.github.pikczu77.blockopener.opening.BlockOpening} so it takes priority over the block's own action.
 */
public class BlockOpenerItem extends Item {
	public BlockOpenerItem(Properties properties) {
		super(properties);
	}
}
