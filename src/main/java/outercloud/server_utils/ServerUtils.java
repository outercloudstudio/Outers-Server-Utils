package outercloud.server_utils;

import com.google.common.collect.Sets;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

public class ServerUtils implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ocsudl");

	private HashMap<String, RespawnGroup> respawnGroups = new HashMap<>();

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));

		ServerTickEvents.END_SERVER_TICK.register(this::tick);
	}

	private ArrayList<Entity> selectedEntities = new ArrayList<>();
	private ServerWorld selectWorld;
	private Vec3d selectBoxStart;
	private Vec3d selectBoxEnd;

	private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				CommandManager.literal("select")
						.requires(source -> source.hasPermissionLevel(4))
						.then(CommandManager.literal("box")
								.then(CommandManager.literal("start").executes(context -> {
									if (!context.getSource().isExecutedByPlayer()) return -1;

									ServerPlayerEntity player = context.getSource().getPlayer();

									selectBoxStart = player.getPos();

									return Command.SINGLE_SUCCESS;
								}))
								.then(CommandManager.literal("end").executes(context -> {
									if (!context.getSource().isExecutedByPlayer()) return -1;

									ServerPlayerEntity player = context.getSource().getPlayer();

									selectBoxEnd = player.getEyePos();
									selectWorld = player.getServerWorld();

									return Command.SINGLE_SUCCESS;
								}))
								.then(CommandManager.literal("apply").executes(context -> {
									if (!context.getSource().isExecutedByPlayer()) return -1;

									if(selectBoxStart == null) return -1;
									if(selectBoxEnd == null) return -1;
									if(selectWorld == null) return -1;

									for(Entity otherEntity : selectWorld.getOtherEntities(null, new Box(selectBoxStart, selectBoxEnd))) {
										if(selectedEntities.contains(otherEntity)) continue;

										selectedEntities.add(otherEntity);
									}

									selectBoxStart = null;
									selectBoxEnd = null;
									selectWorld = null;

									return Command.SINGLE_SUCCESS;
								}))
								.then(CommandManager.literal("cancel").executes(context -> {
									if (!context.getSource().isExecutedByPlayer()) return -1;

									selectBoxStart = null;
									selectBoxEnd = null;
									selectWorld = null;

									return Command.SINGLE_SUCCESS;
								}))
						)
						.executes(context -> {
							if (!context.getSource().isExecutedByPlayer()) return -1;

							ServerPlayerEntity player = context.getSource().getPlayer();

							Vec3d start = player.getCameraPosVec(1);
							Vec3d direction = player.getRotationVec(1);
							Vec3d end = start.add(direction.x * 20f, direction.y * 20f, direction.z * 20f);

							Box box = Box.from(start.lerp(end, 0.5f)).expand(10f);
							EntityHitResult result = raycast(player, start, end, box, entity -> true, 20f);

							if (result == null) return 0;

							if (result.getType() != HitResult.Type.ENTITY) return -1;

							Entity entity = (result).getEntity();

							if(selectedEntities.contains(entity)) return 0;

							selectedEntities.add(entity);

							return Command.SINGLE_SUCCESS;
						})
		);

		dispatcher.register(
				CommandManager.literal("deselect")
						.requires(source -> source.hasPermissionLevel(4))
						.then(CommandManager.literal("box")
								.then(CommandManager.literal("start").executes(context -> {
									if (!context.getSource().isExecutedByPlayer()) return -1;

									ServerPlayerEntity player = context.getSource().getPlayer();

									selectBoxStart = player.getPos();

									return Command.SINGLE_SUCCESS;
								}))
								.then(CommandManager.literal("end").executes(context -> {
									if (!context.getSource().isExecutedByPlayer()) return -1;

									ServerPlayerEntity player = context.getSource().getPlayer();

									selectBoxEnd = player.getEyePos();
									selectWorld = player.getServerWorld();

									return Command.SINGLE_SUCCESS;
								}))
								.then(CommandManager.literal("apply").executes(context -> {
									if (!context.getSource().isExecutedByPlayer()) return -1;

									if(selectBoxStart == null) return -1;
									if(selectBoxEnd == null) return -1;
									if(selectWorld == null) return -1;

									for(Entity otherEntity : selectWorld.getOtherEntities(null, new Box(selectBoxStart, selectBoxEnd))) {
										if(!selectedEntities.contains(otherEntity)) continue;

										selectedEntities.remove(otherEntity);
									}

									selectBoxStart = null;
									selectBoxEnd = null;
									selectWorld = null;

									return Command.SINGLE_SUCCESS;
								}))
								.then(CommandManager.literal("cancel").executes(context -> {
									if (!context.getSource().isExecutedByPlayer()) return -1;

									selectBoxStart = null;
									selectBoxEnd = null;
									selectWorld = null;

									return Command.SINGLE_SUCCESS;
								}))
						)
						.then(CommandManager.literal("all").executes(context -> {
							if (!context.getSource().isExecutedByPlayer()) return -1;

							selectedEntities.clear();

							return Command.SINGLE_SUCCESS;
						}))
						.executes(context -> {
							if (!context.getSource().isExecutedByPlayer()) return -1;

							ServerPlayerEntity player = context.getSource().getPlayer();

							Vec3d start = player.getCameraPosVec(1);
							Vec3d direction = player.getRotationVec(1);
							Vec3d end = start.add(direction.x * 20f, direction.y * 20f, direction.z * 20f);

							Box box = Box.from(start.lerp(end, 0.5f)).expand(10f);
							EntityHitResult result = raycast(player, start, end, box, entity -> true, 20f);

							if (result == null) return 0;

							if (result.getType() != HitResult.Type.ENTITY) return -1;

							Entity entity = result.getEntity();

							if(!selectedEntities.contains(entity)) return 0;

							selectedEntities.remove(entity);

							return Command.SINGLE_SUCCESS;
						})
		);

		dispatcher.register(
				CommandManager.literal("respawn")
						.then(CommandManager.literal("create")
								.then(CommandManager.argument("tag", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(getTags(context), builder))
										.then(CommandManager.argument("delay", FloatArgumentType.floatArg(0))
												.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
														.executes(context -> {
															String tag = StringArgumentType.getString(context, "tag");
															float delay = FloatArgumentType.getFloat(context, "delay");
															int amount = IntegerArgumentType.getInteger(context, "amount");

															if(respawnGroups.containsKey(tag)) return -1;

															respawnGroups.put(tag, new RespawnGroup(tag, delay, amount, context.getSource().getServer()));

															return Command.SINGLE_SUCCESS;
														}))
												.executes(context -> {
													String tag = StringArgumentType.getString(context, "tag");
													float delay = FloatArgumentType.getFloat(context, "delay");

													if(respawnGroups.containsKey(tag)) return -1;

													respawnGroups.put(tag, new RespawnGroup(tag, delay, 1, context.getSource().getServer()));

													return Command.SINGLE_SUCCESS;
												}))
										.executes(context -> {
											String tag = StringArgumentType.getString(context, "tag");

											if(respawnGroups.containsKey(tag)) return -1;

											respawnGroups.put(tag, new RespawnGroup(tag, 0, 1, context.getSource().getServer()));

											return Command.SINGLE_SUCCESS;
										})))
						.then(CommandManager.literal("remove")
								.then(CommandManager.argument("tag", StringArgumentType.word())
										.suggests((context, builder) -> CommandSource.suggestMatching(getTags(context), builder))
										.executes(context -> {
											String tag = StringArgumentType.getString(context, "tag");

											if(!respawnGroups.containsKey(tag)) return -1;

											respawnGroups.remove(tag);

											return Command.SINGLE_SUCCESS;
										})))
						.then(CommandManager.literal("reset")
								.executes(context -> {
									for(RespawnGroup respawnGroup : respawnGroups.values()){
										respawnGroup.reset();
									}

									return Command.SINGLE_SUCCESS;
								}))
		);
	}

	private static Set<String> getTags(CommandContext<ServerCommandSource> context) {
		Set<String> set = Sets.newHashSet();

        for (Entity entity : context.getSource().getWorld().iterateEntities()) {
            set.addAll(entity.getCommandTags());
        }

		return set;
	}

	private void drawParticleLine(ServerWorld world, ParticleEffect particle, Vec3d from, Vec3d to, float density) {
		Vec3d direction = to.subtract(from).normalize();

		for(float distance = 0; distance < from.distanceTo(to); distance += 1f / density) {
			world.spawnParticles(particle, from.x + direction.x * distance, from.y + direction.y * distance, from.z + direction.z * distance, 1, 0, 0, 0, 0);
		}
	}

	private void drawBox(ServerWorld world, ParticleEffect particle, Vec3d from, Vec3d to, float density) {
		drawParticleLine(world, particle, from, new Vec3d(to.x, from.y, from.z), density);
		drawParticleLine(world, particle, from, new Vec3d(from.x, to.y, from.z), density);
		drawParticleLine(world, particle, new Vec3d(to.x, from.y, from.z), new Vec3d(to.x, from.y, to.z), density);
		drawParticleLine(world, particle, new Vec3d(from.x, from.y, to.z), new Vec3d(to.x, from.y, to.z), density);

		drawParticleLine(world, particle, new Vec3d(from.x, to.y, from.z), new Vec3d(to.x, to.y, from.z), density);
		drawParticleLine(world, particle, new Vec3d(from.x, to.y, from.z), new Vec3d(from.x, to.y, to.z), density);
		drawParticleLine(world, particle, new Vec3d(to.x, to.y, from.z), new Vec3d(to.x, to.y, to.z), density);
		drawParticleLine(world, particle, new Vec3d(from.x, to.y, to.z), new Vec3d(to.x, to.y, to.z), density);

		drawParticleLine(world, particle, from, new Vec3d(from.x, from.y, to.z), density);
		drawParticleLine(world, particle, new Vec3d(to.x, from.y, from.z), new Vec3d(to.x, to.y, from.z), density);
		drawParticleLine(world, particle, new Vec3d(from.x, from.y, to.z), new Vec3d(from.x, to.y, to.z), density);
		drawParticleLine(world, particle, new Vec3d(to.x, from.y, to.z), new Vec3d(to.x, to.y, to.z), density);
	}

	private int tickCount = 0;

	private void tick(MinecraftServer server) {
		if(tickCount % 10 == 0) {
			for(int entityIndex = 0; entityIndex < selectedEntities.size(); entityIndex++) {
				Entity entity = selectedEntities.get(entityIndex);

				if(!entity.isAlive()) {
					selectedEntities.remove(entity);

					entityIndex--;

					continue;
				}

				ServerWorld world = (ServerWorld) entity.getWorld();

				world.spawnParticles(ParticleTypes.SCULK_SOUL, entity.getX(), entity.getBoundingBox().maxY + 0.4, entity.getZ(), 1, 0, 0, 0, 0);
			}
		}

		if(selectBoxStart != null && selectBoxEnd != null && selectWorld != null)
			drawBox(selectWorld, ParticleTypes.COMPOSTER, selectBoxStart, selectBoxEnd, 2f);


		for(RespawnGroup respawnGroup : respawnGroups.values()) {
			respawnGroup.tick();
		}

		tickCount++;
	}

	private EntityHitResult raycast(Entity entity, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate, double maxDistance) {
		double closestDistance = maxDistance;
		Entity closestEntity = null;
		Vec3d position = null;

        for (Entity otherEntity : entity.getWorld().getOtherEntities(entity, box, predicate)) {
            Box otherEntityBox = otherEntity.getBoundingBox().expand(otherEntity.getTargetingMargin());

            Optional<Vec3d> optional = otherEntityBox.raycast(min, max);

            if (otherEntityBox.contains(min)) {
                if (closestDistance >= 0.0) {
                    closestEntity = otherEntity;
                    position = optional.orElse(min);
                    closestDistance = 0.0;
                }
            } else if (optional.isPresent()) {
                Vec3d intersectPosition = optional.get();

                double distanceToIntersect = min.distanceTo(intersectPosition);

                if (distanceToIntersect < closestDistance) {
					closestEntity = otherEntity;
					position = intersectPosition;
					closestDistance = distanceToIntersect;
                }
            }
        }

		if (closestEntity == null) return null;

		return new EntityHitResult(closestEntity, position);
	}
}