package io.github.pikczu77.blockopener.opening;

import io.github.pikczu77.blockopener.BlockOpener;
import io.github.pikczu77.blockopener.settings.ModSettings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What is inside a block is fully data driven:
 * {@code blockopener:opening/<namespace>/<block>} if it exists, otherwise {@code blockopener:opening/default}.
 * In extended mode {@code blockopener:opening_extended/<namespace>/<block>} wins over both.
 * Tables use the chest context (origin + opening player), so vanilla structure chest tables can be nested.
 */
public final class OpeningLoot {
	public static final ResourceKey<LootTable> DEFAULT = key("opening/default");

	public static ResourceKey<LootTable> tableFor(ServerLevel level, BlockState state) {
		Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		String path = blockId.getNamespace() + "/" + blockId.getPath();
		if (ModSettings.get(level.getServer()).extendedItems()) {
			ResourceKey<LootTable> extended = key("opening_extended/" + path);
			if (exists(level, extended)) {
				return extended;
			}
		}
		ResourceKey<LootTable> specific = key("opening/" + path);
		return exists(level, specific) ? specific : DEFAULT;
	}

	private static boolean exists(ServerLevel level, ResourceKey<LootTable> key) {
		return level.getServer().reloadableRegistries().getLootTable(key) != LootTable.EMPTY;
	}

	public static List<ItemStack> roll(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player player, int rolls) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(tableFor(level, state));
		LootParams.Builder params = new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
			.withOptionalParameter(LootContextParams.THIS_ENTITY, player);
		if (player != null) {
			params.withLuck(player.getLuck());
		}
		LootParams built = params.create(LootContextParamSets.CHEST);
		List<ItemStack> result = new ArrayList<>();
		for (int i = 0; i < Math.max(1, rolls); i++) {
			result.addAll(table.getRandomItems(built));
		}
		result.removeIf(ItemStack::isEmpty);
		return result;
	}

	private static ResourceKey<LootTable> key(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, BlockOpener.id(path));
	}

	private OpeningLoot() {
	}
}
