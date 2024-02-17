package outercloud.simple_npcs;

import com.google.common.collect.Sets;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.core.jmx.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class SimpleNpcs implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("simple_npcs");

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));

		ServerTickEvents.END_SERVER_TICK.register(this::tick);
	}

	private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				CommandManager.literal("respawn")
						.then(CommandManager.literal("create")
								.then(CommandManager.argument("tag", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(getTags(context), builder))
										.then(CommandManager.argument("delay", FloatArgumentType.floatArg(0))
												.then(CommandManager.argument("radius", FloatArgumentType.floatArg(0))
														.then(CommandManager.literal("random")
																.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
																		.executes(context -> {
																			String tag = StringArgumentType.getString(context, "tag");
																			float delay = FloatArgumentType.getFloat(context, "delay");
																			int amount = IntegerArgumentType.getInteger(context, "amount");
																			float radius = FloatArgumentType.getFloat(context, "radius");

																			if(getPersistentState(context).respawnGroups.containsKey(tag)) {
																				context.getSource().sendError(Text.of("A respawn group with that tag already exists!"));

																				return -1;
																			}

																			getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(tag, delay, true, amount, radius, context.getSource().getServer()));

																			context.getSource().sendFeedback(() -> Text.of("Created respawn group!"), false);

																			return Command.SINGLE_SUCCESS;
																		})
																)
																.executes(context -> {
																	String tag = StringArgumentType.getString(context, "tag");
																	float delay = FloatArgumentType.getFloat(context, "delay");
																	float radius = FloatArgumentType.getFloat(context, "radius");

																	if(getPersistentState(context).respawnGroups.containsKey(tag)) {
																		context.getSource().sendError(Text.of("A respawn group with that tag already exists!"));

																		return -1;
																	}

																	getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(tag, delay, true, 0, radius, context.getSource().getServer()));

																	context.getSource().sendFeedback(() -> Text.of("Created respawn group!"), false);

																	return Command.SINGLE_SUCCESS;
																})
														)
														.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
																.executes(context -> {
																	String tag = StringArgumentType.getString(context, "tag");
																	float delay = FloatArgumentType.getFloat(context, "delay");
																	float radius = FloatArgumentType.getFloat(context, "radius");
																	int amount = IntegerArgumentType.getInteger(context, "amount");

																	if(getPersistentState(context).respawnGroups.containsKey(tag)) {
																		context.getSource().sendError(Text.of("A respawn group with that tag already exists!"));

																		return -1;
																	}

																	getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(tag, delay, false, amount, radius, context.getSource().getServer()));

																	context.getSource().sendFeedback(() -> Text.of("Created respawn group!"), false);

																	return Command.SINGLE_SUCCESS;
																})
														)
														.executes(context -> {
															String tag = StringArgumentType.getString(context, "tag");
															float delay = FloatArgumentType.getFloat(context, "delay");
															float radius = FloatArgumentType.getFloat(context, "radius");

															if(getPersistentState(context).respawnGroups.containsKey(tag)) {
																context.getSource().sendError(Text.of("A respawn group with that tag already exists!"));

																return -1;
															}

															getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(tag, delay, false, 0, radius, context.getSource().getServer()));

															context.getSource().sendFeedback(() -> Text.of("Created respawn group!"), false);

															return Command.SINGLE_SUCCESS;
														})
												)
												.executes(context -> {
													String tag = StringArgumentType.getString(context, "tag");
													float delay = FloatArgumentType.getFloat(context, "delay");

													if(getPersistentState(context).respawnGroups.containsKey(tag)) {
														context.getSource().sendError(Text.of("A respawn group with that tag already exists!"));

														return -1;
													}

													getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(tag, delay, false, 0, 0, context.getSource().getServer()));

													context.getSource().sendFeedback(() -> Text.of("Created respawn group!"), false);

													return Command.SINGLE_SUCCESS;
												})
										)
										.executes(context -> {
											String tag = StringArgumentType.getString(context, "tag");

											if(getPersistentState(context).respawnGroups.containsKey(tag)) {
												context.getSource().sendError(Text.of("A respawn group with that tag already exists!"));

												return -1;
											}

											getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(tag, 0, false, 0, 0, context.getSource().getServer()));

											context.getSource().sendFeedback(() -> Text.of("Created respawn group!"), false);

											return Command.SINGLE_SUCCESS;
										})))
						.then(CommandManager.literal("edit")
								.then(CommandManager.argument("tag", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(getPersistentState(context).respawnGroups.keySet(), builder))
										.then(CommandManager.argument("delay", FloatArgumentType.floatArg(0))
												.then(CommandManager.argument("radius", FloatArgumentType.floatArg(0))
														.then(CommandManager.literal("random")
																.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
																		.executes(context -> {
																			String tag = StringArgumentType.getString(context, "tag");
																			float delay = FloatArgumentType.getFloat(context, "delay");
																			int amount = IntegerArgumentType.getInteger(context, "amount");
																			float radius = FloatArgumentType.getFloat(context, "radius");

																			if(!getPersistentState(context).respawnGroups.containsKey(tag)) {
																				context.getSource().sendError(Text.of("No respawn group with that tag exists!"));

																				return -1;
																			}

																			getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(tag, delay, true, amount, radius, context.getSource().getServer(), getPersistentState(context).respawnGroups.get(tag)));

																			context.getSource().sendFeedback(() -> Text.of("Edited respawn group!"), false);

																			return Command.SINGLE_SUCCESS;
																		})
																)
																.executes(context -> {
																	String tag = StringArgumentType.getString(context, "tag");
																	float delay = FloatArgumentType.getFloat(context, "delay");
																	float radius = FloatArgumentType.getFloat(context, "radius");

																	if(!getPersistentState(context).respawnGroups.containsKey(tag)) {
																		context.getSource().sendError(Text.of("No respawn group with that tag exists!"));

																		return -1;
																	}
																	getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(tag, delay, true, 0, radius, context.getSource().getServer(), getPersistentState(context).respawnGroups.get(tag)));

																	context.getSource().sendFeedback(() -> Text.of("Edited respawn group!"), false);

																	return Command.SINGLE_SUCCESS;
																})
														)
														.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
																.executes(context -> {
																	String tag = StringArgumentType.getString(context, "tag");
																	float delay = FloatArgumentType.getFloat(context, "delay");
																	int amount = IntegerArgumentType.getInteger(context, "amount");
																	float radius = FloatArgumentType.getFloat(context, "radius");

																	if(!getPersistentState(context).respawnGroups.containsKey(tag)) {
																		context.getSource().sendError(Text.of("No respawn group with that tag exists!"));

																		return -1;
																	}

																	getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(tag, delay, false, amount, radius, context.getSource().getServer(), getPersistentState(context).respawnGroups.get(tag)));

																	context.getSource().sendFeedback(() -> Text.of("Edited respawn group!"), false);

																	return Command.SINGLE_SUCCESS;
																})
														)
														.executes(context -> {
															String tag = StringArgumentType.getString(context, "tag");
															float delay = FloatArgumentType.getFloat(context, "delay");
															float radius = FloatArgumentType.getFloat(context, "radius");

															if(!getPersistentState(context).respawnGroups.containsKey(tag)) {
																context.getSource().sendError(Text.of("No respawn group with that tag exists!"));

																return -1;
															}
															getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(tag, delay, false, 0, radius, context.getSource().getServer(), getPersistentState(context).respawnGroups.get(tag)));

															context.getSource().sendFeedback(() -> Text.of("Edited respawn group!"), false);

															return Command.SINGLE_SUCCESS;
														})
												)
												.executes(context -> {
													String tag = StringArgumentType.getString(context, "tag");
													float delay = FloatArgumentType.getFloat(context, "delay");

													if(!getPersistentState(context).respawnGroups.containsKey(tag)) {
														context.getSource().sendError(Text.of("No respawn group with that tag exists!"));

														return -1;
													}
													getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(tag, delay, false, 0, 0, context.getSource().getServer(), getPersistentState(context).respawnGroups.get(tag)));

													context.getSource().sendFeedback(() -> Text.of("Edited respawn group!"), false);

													return Command.SINGLE_SUCCESS;
												})
										)
										.executes(context -> {
											String tag = StringArgumentType.getString(context, "tag");

											if(!getPersistentState(context).respawnGroups.containsKey(tag)) {
												context.getSource().sendError(Text.of("No respawn group with that tag exists!"));

												return -1;
											}

											getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(tag, 0, false, 0, 0, context.getSource().getServer(), getPersistentState(context).respawnGroups.get(tag)));

											context.getSource().sendFeedback(() -> Text.of("Edited respawn group!"), false);

											return Command.SINGLE_SUCCESS;
										})
								)
						)
						.then(CommandManager.literal("remove")
								.then(CommandManager.argument("tag", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(getPersistentState(context).respawnGroups.keySet(), builder))
										.executes(context -> {
											String tag = StringArgumentType.getString(context, "tag");

											if(!getPersistentState(context).respawnGroups.containsKey(tag)) {
												context.getSource().sendError(Text.of("No respawn group with that tag exists!"));

												return -1;
											}

											getPersistentState(context).respawnGroups.remove(tag);

											context.getSource().sendFeedback(() -> Text.of("Removed respawn group!"), false);

											return Command.SINGLE_SUCCESS;
										})))
						.then(CommandManager.literal("reset")
								.then(CommandManager.literal("all").executes(context -> {
									for(RespawnGroup respawnGroup : getPersistentState(context).respawnGroups.values()){
										respawnGroup.reset(context.getSource().getServer());
									}

									context.getSource().sendFeedback(() -> Text.of("Reset all respawn groups!"), false);

									return Command.SINGLE_SUCCESS;
								}))
								.then(CommandManager.argument("tag", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(getPersistentState(context).respawnGroups.keySet(), builder))
										.executes(context -> {
											String tag = StringArgumentType.getString(context, "tag");

											if(!getPersistentState(context).respawnGroups.containsKey(tag)) {
												context.getSource().sendError(Text.of("No respawn group with that tag exists!"));

												return -1;
											}

											getPersistentState(context).respawnGroups.get(tag).reset(context.getSource().getServer());

											context.getSource().sendFeedback(() -> Text.of("Reset respawn group!"), false);

											return Command.SINGLE_SUCCESS;
										})
								)
						)
						.then(CommandManager.literal("list")
								.executes(context -> {
									context.getSource().sendFeedback(() -> Text.literal("Current Respawn Groups:"), false);

									int index = 1;
									for(RespawnGroup respawnGroup : getPersistentState(context).respawnGroups.values()){
										int localIndex = index;
										context.getSource().sendFeedback(() -> Text.literal(localIndex +". " + respawnGroup.getTag()), false);

										index++;
									}

									return Command.SINGLE_SUCCESS;
								})
						)
						.then(CommandManager.literal("freeze")
								.then(CommandManager.literal("all").executes(context -> {
									for(RespawnGroup respawnGroup : getPersistentState(context).respawnGroups.values()){
										respawnGroup.freeze();
									}

									context.getSource().sendFeedback(() -> Text.of("Froze all respawn groups!"), false);

									return Command.SINGLE_SUCCESS;
								}))
								.then(CommandManager.argument("tag", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(getPersistentState(context).respawnGroups.keySet(), builder))
										.executes(context -> {
											String tag = StringArgumentType.getString(context, "tag");

											if(!getPersistentState(context).respawnGroups.containsKey(tag)) {
												context.getSource().sendError(Text.of("No respawn group with that tag exists!"));

												return -1;
											}

											getPersistentState(context).respawnGroups.get(tag).freeze();

											context.getSource().sendFeedback(() -> Text.of("Froze respawn group!"), false);

											return Command.SINGLE_SUCCESS;
										})
								)
						)
						.then(CommandManager.literal("unfreeze")
								.then(CommandManager.literal("all").executes(context -> {
									for(RespawnGroup respawnGroup : getPersistentState(context).respawnGroups.values()){
										respawnGroup.unfreeze();
									}

									context.getSource().sendFeedback(() -> Text.of("Unfroze all respawn groups!"), false);

									return Command.SINGLE_SUCCESS;
								}))
								.then(CommandManager.argument("tag", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(getPersistentState(context).respawnGroups.keySet(), builder))
										.executes(context -> {
											String tag = StringArgumentType.getString(context, "tag");

											if(!getPersistentState(context).respawnGroups.containsKey(tag)) {
												context.getSource().sendError(Text.of("No respawn group with that tag exists!"));

												return -1;
											}

											getPersistentState(context).respawnGroups.get(tag).unfreeze();

											context.getSource().sendFeedback(() -> Text.of("Unfroze respawn group!"), false);

											return Command.SINGLE_SUCCESS;
										})
								)
						)
		);
	}

	private UtilsPersistentState getPersistentState(CommandContext<ServerCommandSource> context){
		return UtilsPersistentState.getServerState(context.getSource().getServer());
	}

	private static Set<String> getTags(CommandContext<ServerCommandSource> context) {
		Set<String> set = Sets.newHashSet();

        for (Entity entity : context.getSource().getWorld().iterateEntities()) {
            set.addAll(entity.getCommandTags());
        }

		return set;
	}

	private void tick(MinecraftServer server) {
		for(RespawnGroup respawnGroup : UtilsPersistentState.getServerState(server).respawnGroups.values()) {
			respawnGroup.tick(server);
		}
	}
}