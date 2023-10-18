package outercloud.server_utils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerUtils implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ocsudl");

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));

		ServerTickEvents.END_SERVER_TICK.register(this::tick);
	}

	private ServerPlayerEntity currentPlayer;
	private Vec3d startPosition;
	private Vec3d endPosition;

	private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> startCommand = LiteralArgumentBuilder.literal("start");
		LiteralArgumentBuilder<ServerCommandSource> endCommand = LiteralArgumentBuilder.literal("end");
		LiteralArgumentBuilder<ServerCommandSource> cancelCommand = LiteralArgumentBuilder.literal("cancel");

		dispatcher.register(
				startCommand
						.requires(source -> source.hasPermissionLevel(4))
						.executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayer();

							currentPlayer = player;
							startPosition = player.getPos();

							return Command.SINGLE_SUCCESS;
						})
		);

		dispatcher.register(
				endCommand
						.requires(source -> source.hasPermissionLevel(4))
						.executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayer();

							endPosition = player.getPos();

							return Command.SINGLE_SUCCESS;
						})
		);

		dispatcher.register(
				cancelCommand
						.requires(source -> source.hasPermissionLevel(4))
						.executes(context -> {
							currentPlayer = null;

							return Command.SINGLE_SUCCESS;
						})
		);
	}


	private void drawParticleLine(ServerWorld world, ParticleEffect particle, Vec3d from, Vec3d to, float density) {
		Vec3d direction = to.subtract(from).normalize();

		for(float distance = 0; distance < from.distanceTo(to); distance += 1f / density) {
			world.spawnParticles(particle, from.x + direction.x * distance, from.y + direction.y * distance, from.z + direction.z * distance, 1, 0, 0, 0, 0);
		}
	}

	private void tick(MinecraftServer server) {
		if(currentPlayer == null) return;
		if(startPosition == null) return;
		if(endPosition == null) return;

		drawParticleLine(currentPlayer.getServerWorld(), ParticleTypes.COMPOSTER, startPosition, new Vec3d(endPosition.x, startPosition.y, startPosition.z), 2f);
		drawParticleLine(currentPlayer.getServerWorld(), ParticleTypes.COMPOSTER, startPosition, new Vec3d(startPosition.x, endPosition.y, startPosition.z), 2f);
		drawParticleLine(currentPlayer.getServerWorld(), ParticleTypes.COMPOSTER, new Vec3d(endPosition.x, startPosition.y, startPosition.z), new Vec3d(endPosition.x, startPosition.y, endPosition.z), 2f);
		drawParticleLine(currentPlayer.getServerWorld(), ParticleTypes.COMPOSTER, new Vec3d(startPosition.x, startPosition.y, endPosition.z), new Vec3d(endPosition.x, startPosition.y, endPosition.z), 2f);

		drawParticleLine(currentPlayer.getServerWorld(), ParticleTypes.COMPOSTER, new Vec3d(startPosition.x, endPosition.y, startPosition.z), new Vec3d(endPosition.x, endPosition.y, startPosition.z), 2f);
		drawParticleLine(currentPlayer.getServerWorld(), ParticleTypes.COMPOSTER, new Vec3d(startPosition.x, endPosition.y, startPosition.z), new Vec3d(startPosition.x, endPosition.y, endPosition.z), 2f);
		drawParticleLine(currentPlayer.getServerWorld(), ParticleTypes.COMPOSTER, new Vec3d(endPosition.x, endPosition.y, startPosition.z), new Vec3d(endPosition.x, endPosition.y, endPosition.z), 2f);
		drawParticleLine(currentPlayer.getServerWorld(), ParticleTypes.COMPOSTER, new Vec3d(startPosition.x, endPosition.y, endPosition.z), new Vec3d(endPosition.x, endPosition.y, endPosition.z), 2f);

		drawParticleLine(currentPlayer.getServerWorld(), ParticleTypes.COMPOSTER, startPosition, new Vec3d(startPosition.x, startPosition.y, endPosition.z), 2f);
		drawParticleLine(currentPlayer.getServerWorld(), ParticleTypes.COMPOSTER, new Vec3d(endPosition.x, startPosition.y, startPosition.z), new Vec3d(endPosition.x, endPosition.y, startPosition.z), 2f);
		drawParticleLine(currentPlayer.getServerWorld(), ParticleTypes.COMPOSTER, new Vec3d(startPosition.x, startPosition.y, endPosition.z), new Vec3d(startPosition.x, endPosition.y, endPosition.z), 2f);
		drawParticleLine(currentPlayer.getServerWorld(), ParticleTypes.COMPOSTER, new Vec3d(endPosition.x, startPosition.y, endPosition.z), new Vec3d(endPosition.x, endPosition.y, endPosition.z), 2f);
	}
}