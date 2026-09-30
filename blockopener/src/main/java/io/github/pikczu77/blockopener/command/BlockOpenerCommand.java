package io.github.pikczu77.blockopener.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.pikczu77.blockopener.challenge.ChallengeTimer;
import io.github.pikczu77.blockopener.opening.BlockOpening;
import io.github.pikczu77.blockopener.progress.Progress;
import io.github.pikczu77.blockopener.progress.SecretItem;
import io.github.pikczu77.blockopener.registry.ModAttachments;
import io.github.pikczu77.blockopener.registry.ModItems;
import io.github.pikczu77.blockopener.settings.ModSettings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jspecify.annotations.Nullable;

/**
 * {@code /blockopener} (alias {@code /bo}): everything needed to set up, stage and re-take recordings.
 */
public final class BlockOpenerCommand {
	private static final DynamicCommandExceptionType UNKNOWN_ITEM = new DynamicCommandExceptionType(
		name -> Component.translatable("blockopener.command.unknown_item", name)
	);
	private static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS = (context, builder) ->
		SharedSuggestionProvider.suggest(Stream.concat(Stream.of("opener", "all"), Stream.of(SecretItem.values()).map(SecretItem::id)), builder);
	private static final SuggestionProvider<CommandSourceStack> SECRET_SUGGESTIONS = (context, builder) ->
		SharedSuggestionProvider.suggest(Stream.of(SecretItem.values()).map(SecretItem::id), builder);
	private static final int LOCATE_RADIUS_CHUNKS = 8;

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("blockopener")
			.executes(BlockOpenerCommand::help)
			.then(Commands.literal("help").executes(BlockOpenerCommand::help))
			.then(Commands.literal("progress")
				.executes(ctx -> progress(ctx.getSource(), ctx.getSource().getPlayerOrException()))
				.then(Commands.argument("player", EntityArgument.player()).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(ctx -> progress(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
			.then(Commands.literal("hud")
				.executes(ctx -> hud(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), null))
				.then(Commands.argument("visible", BoolArgumentType.bool())
					.executes(ctx -> hud(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), BoolArgumentType.getBool(ctx, "visible")))
					.then(Commands.argument("targets", EntityArgument.players()).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ctx -> hud(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), BoolArgumentType.getBool(ctx, "visible"))))))
			.then(gamemaster("give")
				.executes(ctx -> give(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException()), "opener"))
				.then(Commands.argument("targets", EntityArgument.players())
					.executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), "opener"))
					.then(Commands.argument("item", StringArgumentType.word()).suggests(ITEM_SUGGESTIONS)
						.executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "item"))))))
			.then(gamemaster("reset")
				.executes(ctx -> reset(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
				.then(Commands.argument("targets", EntityArgument.players())
					.executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
			.then(gamemaster("complete")
				.executes(ctx -> complete(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
				.then(Commands.argument("targets", EntityArgument.players())
					.executes(ctx -> complete(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
			.then(gamemaster("reveal")
				.then(Commands.argument("player", EntityArgument.player())
					.then(Commands.argument("item", StringArgumentType.word()).suggests(SECRET_SUGGESTIONS)
						.executes(ctx -> reveal(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), secret(StringArgumentType.getString(ctx, "item")))))))
			.then(gamemaster("locate")
				.then(Commands.argument("item", StringArgumentType.word()).suggests(SECRET_SUGGESTIONS)
					.executes(ctx -> locate(ctx.getSource(), secret(StringArgumentType.getString(ctx, "item"))))))
			.then(gamemaster("testrow").executes(ctx -> testRow(ctx.getSource())))
			.then(gamemaster("showcase")
				.executes(ctx -> showcase(ctx.getSource(), 60))
				.then(Commands.argument("seconds", IntegerArgumentType.integer(5, 3600))
					.executes(ctx -> showcase(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "seconds"))))
				.then(Commands.literal("clear").executes(ctx -> clearMarkers(ctx.getSource()))))
			.then(gamemaster("open")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(ctx -> openAt(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))
			.then(gamemaster("timer")
				.then(Commands.literal("start")
					.then(Commands.argument("minutes", IntegerArgumentType.integer(0, 600))
						.executes(ctx -> timerStart(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "minutes"), 0))
						.then(Commands.argument("seconds", IntegerArgumentType.integer(0, 59))
							.executes(ctx -> timerStart(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "minutes"), IntegerArgumentType.getInteger(ctx, "seconds"))))))
				.then(Commands.literal("pause").executes(ctx -> timerPause(ctx.getSource())))
				.then(Commands.literal("stop").executes(ctx -> timerStop(ctx.getSource())))
				.then(Commands.literal("add")
					.then(Commands.argument("seconds", IntegerArgumentType.integer(-3600, 3600))
						.executes(ctx -> timerAdd(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "seconds"))))))
			.then(gamemaster("settings")
				.executes(ctx -> settings(ctx.getSource()))
				.then(Commands.literal("explosionsBreakBlocks").then(Commands.argument("value", BoolArgumentType.bool())
					.executes(ctx -> updateSettings(ctx.getSource(), s -> s.withExplosionsBreakBlocks(BoolArgumentType.getBool(ctx, "value"))))))
				.then(Commands.literal("announceFinds").then(Commands.argument("value", BoolArgumentType.bool())
					.executes(ctx -> updateSettings(ctx.getSource(), s -> s.withAnnounceFinds(BoolArgumentType.getBool(ctx, "value"))))))
				.then(Commands.literal("lootRolls").then(Commands.argument("value", IntegerArgumentType.integer(1, 16))
					.executes(ctx -> updateSettings(ctx.getSource(), s -> s.withLootRolls(IntegerArgumentType.getInteger(ctx, "value"))))))
				.then(Commands.literal("openCooldown").then(Commands.argument("ticks", IntegerArgumentType.integer(0, 200))
					.executes(ctx -> updateSettings(ctx.getSource(), s -> s.withOpenCooldown(IntegerArgumentType.getInteger(ctx, "ticks")))))));

		dispatcher.register(root);
		dispatcher.register(Commands.literal("bo").executes(BlockOpenerCommand::help).redirect(dispatcher.getRoot().getChild("blockopener")));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> gamemaster(String name) {
		return Commands.literal(name).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
	}

	private static SecretItem secret(String id) throws CommandSyntaxException {
		return SecretItem.byId(id).orElseThrow(() -> UNKNOWN_ITEM.create(id));
	}

	private static int help(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		source.sendSuccess(() -> Component.translatable("blockopener.command.help.header").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
		for (String line : new String[] {"give", "reset", "complete", "reveal", "timer", "locate", "testrow", "showcase", "hud", "progress", "settings", "open"}) {
			source.sendSuccess(() -> Component.literal("/bo " + line).withStyle(ChatFormatting.YELLOW)
				.append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
				.append(Component.translatable("blockopener.command.help." + line).withStyle(ChatFormatting.GRAY)), false);
		}
		return 1;
	}

	private static int give(CommandSourceStack source, Collection<ServerPlayer> targets, String what) throws CommandSyntaxException {
		List<ItemStack> stacks = new ArrayList<>();
		switch (what) {
			case "opener" -> stacks.add(new ItemStack(ModItems.BLOCK_OPENER));
			case "all" -> {
				stacks.add(new ItemStack(ModItems.BLOCK_OPENER));
				for (SecretItem secret : SecretItem.values()) {
					stacks.add(stackOf(secret));
				}
			}
			default -> stacks.add(stackOf(secret(what)));
		}
		for (ServerPlayer player : targets) {
			for (ItemStack stack : stacks) {
				ItemStack copy = stack.copy();
				if (!player.getInventory().add(copy)) {
					player.drop(copy, false);
				}
			}
		}
		source.sendSuccess(() -> Component.translatable("blockopener.command.give", what, targets.size()), true);
		return targets.size();
	}

	private static ItemStack stackOf(SecretItem secret) {
		ItemStack stack = new ItemStack(secret.item());
		if (secret == SecretItem.MOSSPHERE) {
			stack.setCount(16);
		}
		return stack;
	}

	private static int progress(CommandSourceStack source, ServerPlayer player) {
		source.sendSuccess(() -> Component.translatable("blockopener.command.progress", player.getDisplayName(), Progress.count(player), SecretItem.COUNT)
			.withStyle(ChatFormatting.GOLD), false);
		for (SecretItem secret : SecretItem.values()) {
			boolean found = Progress.hasFound(player, secret);
			MutableComponent line = Component.literal(found ? " ✔ " : " ✘ ").withStyle(found ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
			line.append(found ? Progress.itemName(secret) : Component.literal("???").withStyle(ChatFormatting.DARK_GRAY));
			if (found || source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
				line.append(Component.literal("  ← ").withStyle(ChatFormatting.DARK_GRAY))
					.append(secret.mainSourceBlock().getName().withStyle(ChatFormatting.GRAY));
			}
			source.sendSuccess(() -> line, false);
		}
		return Progress.count(player);
	}

	private static int hud(CommandSourceStack source, Collection<ServerPlayer> targets, @Nullable Boolean visible) {
		for (ServerPlayer player : targets) {
			boolean now = visible != null ? visible : !player.getAttachedOrElse(ModAttachments.TRACKER_HUD, true);
			player.setAttached(ModAttachments.TRACKER_HUD, now);
		}
		source.sendSuccess(() -> Component.translatable("blockopener.command.hud", targets.size()), false);
		return targets.size();
	}

	private static int reset(CommandSourceStack source, Collection<ServerPlayer> targets) {
		for (ServerPlayer player : targets) {
			Progress.reset(player);
			player.removeAttached(ModAttachments.DIAMOND_FLIGHT);
		}
		source.sendSuccess(() -> Component.translatable("blockopener.command.reset", targets.size()), true);
		return targets.size();
	}

	private static int complete(CommandSourceStack source, Collection<ServerPlayer> targets) {
		for (ServerPlayer player : targets) {
			Progress.setAll(player);
		}
		source.sendSuccess(() -> Component.translatable("blockopener.command.complete", targets.size()), true);
		return targets.size();
	}

	private static int reveal(CommandSourceStack source, ServerPlayer player, SecretItem secret) {
		Progress.markFound(player, secret);
		Progress.reveal(player, secret, true);
		ItemStack stack = stackOf(secret);
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
		return 1;
	}

	private static int locate(CommandSourceStack source, SecretItem secret) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = player.level();
		Set<Block> blocks = new HashSet<>(secret.sourceBlocks());
		BlockPos origin = player.blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		ChunkPos center = new ChunkPos(origin);
		for (int dx = -LOCATE_RADIUS_CHUNKS; dx <= LOCATE_RADIUS_CHUNKS; dx++) {
			for (int dz = -LOCATE_RADIUS_CHUNKS; dz <= LOCATE_RADIUS_CHUNKS; dz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(center.x + dx, center.z + dz);
				if (chunk == null) {
					continue;
				}
				LevelChunkSection[] sections = chunk.getSections();
				for (int index = 0; index < sections.length; index++) {
					LevelChunkSection section = sections[index];
					if (section.hasOnlyAir() || !section.maybeHas(state -> blocks.contains(state.getBlock()))) {
						continue;
					}
					int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
					for (int y = 0; y < 16; y++) {
						for (int z = 0; z < 16; z++) {
							for (int x = 0; x < 16; x++) {
								if (!blocks.contains(section.getBlockState(x, y, z).getBlock())) {
									continue;
								}
								BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
								double distance = pos.distSqr(origin);
								if (distance < bestDistance) {
									bestDistance = distance;
									best = pos;
								}
							}
						}
					}
				}
			}
		}
		if (best == null) {
			source.sendFailure(Component.translatable("blockopener.command.locate.none", secret.mainSourceBlock().getName(), LOCATE_RADIUS_CHUNKS * 16));
			return 0;
		}
		BlockPos found = best;
		BlockState state = level.getBlockState(found);
		Markers.highlight(level, found, state, secret.color(), 30);
		String tp = "/tp @s " + found.getX() + " " + (found.getY() + 1) + " " + found.getZ();
		Component coords = Component.literal("[" + found.getX() + ", " + found.getY() + ", " + found.getZ() + "]")
			.withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
				.withClickEvent(new ClickEvent.SuggestCommand(tp))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(tp))));
		int distance = (int) Math.sqrt(bestDistance);
		source.sendSuccess(() -> Component.translatable("blockopener.command.locate.found", state.getBlock().getName(), coords, distance), false);
		return distance;
	}

	private static int testRow(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = player.level();
		Direction facing = player.getDirection();
		Direction side = facing.getClockWise();
		BlockPos start = player.blockPosition().relative(facing, 3);
		SecretItem[] secrets = SecretItem.values();
		for (int i = 0; i < secrets.length; i++) {
			BlockPos pos = start.relative(side, (i - secrets.length / 2) * 2);
			BlockState state = secrets[i].mainSourceBlock().defaultBlockState();
			if (state.hasProperty(DirectionalBlock.FACING)) {
				state = state.setValue(DirectionalBlock.FACING, Direction.UP);
			}
			level.setBlockAndUpdate(pos, state);
			if (level.getBlockState(pos.below()).isAir()) {
				level.setBlockAndUpdate(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState());
			}
		}
		source.sendSuccess(() -> Component.translatable("blockopener.command.testrow"), true);
		return secrets.length;
	}

	private static int showcase(CommandSourceStack source, int seconds) throws CommandSyntaxException {
		Markers.showcase(source.getPlayerOrException(), seconds);
		source.sendSuccess(() -> Component.translatable("blockopener.command.showcase", seconds), false);
		return 1;
	}

	private static int clearMarkers(CommandSourceStack source) {
		int removed = Markers.clear();
		source.sendSuccess(() -> Component.translatable("blockopener.command.showcase.clear", removed), false);
		return removed;
	}

	private static int openAt(CommandSourceStack source, BlockPos pos) {
		ServerLevel level = source.getLevel();
		BlockState state = level.getBlockState(pos);
		if (!BlockOpening.canOpen(level, pos, state)) {
			source.sendFailure(Component.translatable("blockopener.command.open.cannot", state.getBlock().getName()));
			return 0;
		}
		List<ItemStack> loot = BlockOpening.open(level, pos, source.getPlayer());
		source.sendSuccess(() -> Component.translatable("blockopener.command.open", state.getBlock().getName(), loot.size()), false);
		return loot.size();
	}

	private static int timerStart(CommandSourceStack source, int minutes, int seconds) {
		int total = minutes * 60 + seconds;
		if (total <= 0) {
			source.sendFailure(Component.translatable("blockopener.command.timer.zero"));
			return 0;
		}
		ChallengeTimer.start(source.getServer(), total);
		source.sendSuccess(() -> Component.translatable("blockopener.command.timer.start", minutes, seconds), true);
		return total;
	}

	private static int timerPause(CommandSourceStack source) {
		if (!ChallengeTimer.isRunning()) {
			source.sendFailure(Component.translatable("blockopener.command.timer.none"));
			return 0;
		}
		boolean paused = ChallengeTimer.togglePause();
		source.sendSuccess(() -> Component.translatable(paused ? "blockopener.command.timer.paused" : "blockopener.command.timer.resumed"), true);
		return 1;
	}

	private static int timerStop(CommandSourceStack source) {
		ChallengeTimer.stop();
		source.sendSuccess(() -> Component.translatable("blockopener.command.timer.stop"), true);
		return 1;
	}

	private static int timerAdd(CommandSourceStack source, int seconds) {
		if (!ChallengeTimer.isRunning()) {
			source.sendFailure(Component.translatable("blockopener.command.timer.none"));
			return 0;
		}
		ChallengeTimer.addSeconds(seconds);
		source.sendSuccess(() -> Component.translatable("blockopener.command.timer.add", seconds, ChallengeTimer.remainingSeconds()), true);
		return ChallengeTimer.remainingSeconds();
	}

	private static int settings(CommandSourceStack source) {
		ModSettings settings = ModSettings.get(source.getServer());
		source.sendSuccess(() -> Component.translatable("blockopener.command.settings.header").withStyle(ChatFormatting.GOLD), false);
		source.sendSuccess(() -> setting("explosionsBreakBlocks", settings.explosionsBreakBlocks()), false);
		source.sendSuccess(() -> setting("announceFinds", settings.announceFinds()), false);
		source.sendSuccess(() -> setting("lootRolls", settings.lootRolls()), false);
		source.sendSuccess(() -> setting("openCooldown", settings.openCooldown()), false);
		return 1;
	}

	private static Component setting(String name, Object value) {
		return Component.literal(" " + name + ": ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("  "))
			.append(Component.translatable("blockopener.command.settings." + name).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}

	private static int updateSettings(CommandSourceStack source, java.util.function.UnaryOperator<ModSettings> change) {
		ModSettings.set(source.getServer(), change.apply(ModSettings.get(source.getServer())));
		return settings(source);
	}

	private BlockOpenerCommand() {
	}
}
