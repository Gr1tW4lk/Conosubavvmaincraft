package dev.konocraft.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.konocraft.skill.PlayerClass;
import dev.konocraft.story.DialogueManager;
import dev.konocraft.story.Locations;
import dev.konocraft.story.Npcs;
import dev.konocraft.story.Party;
import dev.konocraft.story.Progression;
import dev.konocraft.story.Story;
import dev.konocraft.story.StoryState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /konocraft - helpers for players and testers:
 * start, where, tp &lt;location&gt;, stage &lt;n&gt;, money &lt;n&gt;, exp &lt;n&gt;, class &lt;class&gt;, dialogue &lt;id&gt;, party &lt;who&gt;.
 */
public final class KonoCommand {
	private KonoCommand() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			CommandManager.literal("konocraft")
				.then(CommandManager.literal("start").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					Story.startPrologue(p);
					return 1;
				}))
				.then(CommandManager.literal("axel").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					if (!Progression.data(p).prologueDone) {
						ctx.getSource().sendError(Text.literal("Сначала пройдите пролог у богини Эрис."));
						return 0;
					}
					Story.teleport(p, Locations.AXEL, true);
					return 1;
				}))
				.then(CommandManager.literal("where").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					StoryState s = StoryState.get(p.getServer());
					Story.Objective o = Story.objective(s);
					ctx.getSource().sendFeedback(() -> Text.literal("Стадия " + s.stage + ": " + o.text()), false);
					return 1;
				}))
				.then(CommandManager.literal("unstuck").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					DialogueManager.close(p);
					Npcs.refresh(p.getServer());
					ctx.getSource().sendFeedback(() -> Text.literal("Диалог закрыт, персонажи обновлены."), false);
					return 1;
				}))
				.then(CommandManager.literal("tp").requires(src -> src.hasPermissionLevel(2))
					.then(CommandManager.argument("location", StringArgumentType.word())
						.suggests((ctx, b) -> CommandSource.suggestMatching(Locations.all().keySet(), b))
						.executes(ctx -> {
							ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
							Locations.Loc loc = Locations.get(StringArgumentType.getString(ctx, "location"));
							if (loc == null) return 0;
							Story.teleport(p, loc, true);
							return 1;
						})))
				.then(CommandManager.literal("stage").requires(src -> src.hasPermissionLevel(2))
					.then(CommandManager.argument("stage", IntegerArgumentType.integer(0, 100)).executes(ctx -> {
						Story.setStage(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "stage"));
						return 1;
					})))
				.then(CommandManager.literal("money").requires(src -> src.hasPermissionLevel(2))
					.then(CommandManager.argument("amount", IntegerArgumentType.integer()).executes(ctx -> {
						Progression.addMoney(ctx.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(ctx, "amount"));
						return 1;
					})))
				.then(CommandManager.literal("exp").requires(src -> src.hasPermissionLevel(2))
					.then(CommandManager.argument("amount", IntegerArgumentType.integer(0)).executes(ctx -> {
						Progression.addExp(ctx.getSource().getPlayerOrThrow(), IntegerArgumentType.getInteger(ctx, "amount"));
						return 1;
					})))
				.then(CommandManager.literal("class").requires(src -> src.hasPermissionLevel(2))
					.then(CommandManager.argument("class", StringArgumentType.word())
						.suggests((ctx, b) -> CommandSource.suggestMatching(java.util.Arrays.stream(PlayerClass.values()).map(c -> c.name().toLowerCase()), b))
						.executes(ctx -> {
							Progression.setClass(ctx.getSource().getPlayerOrThrow(), PlayerClass.byName(StringArgumentType.getString(ctx, "class")));
							return 1;
						})))
				.then(CommandManager.literal("dialogue").requires(src -> src.hasPermissionLevel(2))
					.then(CommandManager.argument("id", StringArgumentType.word()).executes(ctx -> {
						DialogueManager.start(ctx.getSource().getPlayerOrThrow(), StringArgumentType.getString(ctx, "id"));
						return 1;
					})))
				.then(CommandManager.literal("fx").requires(src -> src.hasPermissionLevel(2))
					.then(CommandManager.argument("type", StringArgumentType.word()).executes(ctx -> {
						ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
						dev.konocraft.fx.Fx.play(p, StringArgumentType.getString(ctx, "type"),
							dev.konocraft.story.Actions.resolve(p, "ahead 24", null), null);
						return 1;
					})))
				.then(CommandManager.literal("shop").requires(src -> src.hasPermissionLevel(2))
					.then(CommandManager.argument("id", StringArgumentType.word()).executes(ctx -> {
						dev.konocraft.story.Shops.open(ctx.getSource().getPlayerOrThrow(), StringArgumentType.getString(ctx, "id"));
						return 1;
					})))
				.then(CommandManager.literal("party").requires(src -> src.hasPermissionLevel(2))
					.then(CommandManager.argument("who", StringArgumentType.word()).executes(ctx -> {
						Party.join(ctx.getSource().getPlayerOrThrow(), StringArgumentType.getString(ctx, "who"));
						return 1;
					})))
		));
	}
}
