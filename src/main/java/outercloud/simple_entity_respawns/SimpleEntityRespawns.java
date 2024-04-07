package outercloud.simple_entity_respawns;

import com.google.common.collect.Sets;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class SimpleEntityRespawns implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("simple_entity_respawns");

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));

		ServerTickEvents.END_SERVER_TICK.register(this::tick);
	}

	private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				CommandManager.literal("respawns")
						.requires(source -> source.hasPermissionLevel(4))
						.then(CommandManager.literal("create")
								.then(CommandManager.argument("tag", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(getTags(context), builder))
										.then(CommandManager.argument("delay", FloatArgumentType.floatArg(0))
												.then(CommandManager.argument("radius", FloatArgumentType.floatArg(0))
														.executes(context -> {
															String tag = StringArgumentType.getString(context, "tag");
															float delay = FloatArgumentType.getFloat(context, "delay");
															float radius = FloatArgumentType.getFloat(context, "radius");

															if(getPersistentState(context).respawnGroups.containsKey(tag)) {
																context.getSource().sendError(Text.of("A respawn group with that tag already exists!"));

																return -1;
															}

															getPersistentState(context).respawnGroups.put(tag, new RespawnGroup(delay, radius));

															context.getSource().sendFeedback(() -> Text.of("Created respawn group '" + tag + "'"), true);

															return Command.SINGLE_SUCCESS;
														})
												)
										)
								)
						)
						.then(CommandManager.literal("edit")
								.then(CommandManager.argument("tag", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(getPersistentState(context).respawnGroups.keySet(), builder))
										.then(CommandManager.argument("delay", FloatArgumentType.floatArg(0))
												.then(CommandManager.argument("radius", FloatArgumentType.floatArg(0))
														.executes(context -> {
															String tag = StringArgumentType.getString(context, "tag");
															float delay = FloatArgumentType.getFloat(context, "delay");
															float radius = FloatArgumentType.getFloat(context, "radius");

															if(!getPersistentState(context).respawnGroups.containsKey(tag)) {
																context.getSource().sendError(Text.of("No respawn group with that tag exists!"));

																return -1;
															}

															getPersistentState(context).respawnGroups.get(tag).delay = delay;
															getPersistentState(context).respawnGroups.get(tag).radius = radius;

															context.getSource().sendFeedback(() -> Text.of("Edited respawn group '" + tag + "'"), true);

															return Command.SINGLE_SUCCESS;
														})
												)
										)
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

											context.getSource().sendFeedback(() -> Text.of("Removed respawn group '" + tag + "'"), true);

											return Command.SINGLE_SUCCESS;
										})))
						.then(CommandManager.literal("list")
								.executes(context -> {
									context.getSource().sendFeedback(() -> Text.literal("Current Respawn Groups:"), false);

									int index = 1;
									for(String tag : getPersistentState(context).respawnGroups.keySet()){
										int localIndex = index;
										context.getSource().sendFeedback(() -> Text.literal(localIndex +". " + tag), false);

										index++;
									}

									return Command.SINGLE_SUCCESS;
								})
						)
		);
	}

	private PersistentState getPersistentState(CommandContext<ServerCommandSource> context){
		return PersistentState.getServerState(context.getSource().getServer());
	}

	private static Set<String> getTags(CommandContext<ServerCommandSource> context) {
		Set<String> set = Sets.newHashSet();

        for (Entity entity : context.getSource().getWorld().iterateEntities()) {
            set.addAll(entity.getCommandTags());
        }

		return set;
	}

	private void tick(MinecraftServer server) {
		for(RespawnGroup respawnGroup : PersistentState.getServerState(server).respawnGroups.values()) {
			respawnGroup.tick();
		}
	}

	public static RespawnGroup getRespawnGroup(LivingEntity entity) {
		PersistentState persistentState = PersistentState.getServerState(entity.getServer());

		for(String tag: entity.getCommandTags()) {
			if(!persistentState.respawnGroups.containsKey(tag)) continue;

			return persistentState.respawnGroups.get(tag);
		}

		return null;
	}

	public static void entityDied(LivingEntity entity) {
		RespawnGroup respawnGroup = getRespawnGroup(entity);

		if(respawnGroup == null) return;;

		respawnGroup.respawningEntities.add(new RespawningEntity(entity, respawnGroup.delay, (ServerWorld) entity.getWorld()));
	}
}