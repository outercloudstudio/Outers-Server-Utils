package outercloud.server_utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Random;

public class RespawnGroup {
    private String tag;
    private float delay;
    private int amount;

    private int timer;

    private ArrayList<ServerWorld> worlds = new ArrayList<>();
    private ArrayList<Vec3d> positions = new ArrayList<>();
    private ArrayList<NbtCompound> nbts = new ArrayList<>();

    private ArrayList<Entity> spawnedEntities = new ArrayList<>();

    public RespawnGroup(String tag, float delay, int amount, MinecraftServer server) {
        this.tag = tag;
        this.delay = delay;
        this.amount = amount;
        this.timer = MathHelper.floor(delay * 20);

        for(ServerWorld world : server.getWorlds()) {
            for(Entity entity : world.iterateEntities()) {
                if(!entity.getCommandTags().contains(this.tag)) continue;

                worlds.add(world);
                positions.add(entity.getPos());

                NbtCompound nbt = new NbtCompound();
                nbt.putString("id", Registries.ENTITY_TYPE.getId(entity.getType()).toString());
                entity.writeNbt(nbt);
                nbt.remove("UUID");

                nbts.add(nbt);

                spawnedEntities.add(entity);
            }
        }

        if(nbts.isEmpty()) return;

        while(spawnedEntities.size() < amount) {
            spawnedEntities.add(null);
        }
    }

    public void tick() {
        for(int index = 0; index < spawnedEntities.size(); index++) {
            Entity entity = spawnedEntities.get(index);

            if(entity != null && entity.isAlive()) continue;

            timer--;

            if(timer > 0) break;

            Random random = new Random();

            int randomSpawnDataIndex = random.nextInt(nbts.size());

            ServerWorld world = worlds.get(randomSpawnDataIndex);
            Vec3d position = positions.get(randomSpawnDataIndex);
            NbtCompound nbt = nbts.get(randomSpawnDataIndex);

            Entity newEntity = EntityType.loadEntityWithPassengers(nbt, world, (createdEntity) -> {
                createdEntity.refreshPositionAndAngles(position.x, position.y, position.z, createdEntity.getYaw(), createdEntity.getPitch());

                return createdEntity;
            });

            world.spawnNewEntityAndPassengers(newEntity);

            spawnedEntities.set(index, newEntity);
        }

        if(timer <= 0) timer = MathHelper.floor(delay * 20);
    }

    public void reset() {
        for(int index = 0; index < spawnedEntities.size(); index++) {
            Entity entity = spawnedEntities.get(index);

            if(entity != null && entity.isAlive()) entity.kill();

            Random random = new Random();

            int randomSpawnDataIndex = random.nextInt(nbts.size());

            ServerWorld world = worlds.get(randomSpawnDataIndex);
            Vec3d position = positions.get(randomSpawnDataIndex);
            NbtCompound nbt = nbts.get(randomSpawnDataIndex);

            Entity newEntity = EntityType.loadEntityWithPassengers(nbt, world, (createdEntity) -> {
                createdEntity.refreshPositionAndAngles(position.x, position.y, position.z, createdEntity.getYaw(), createdEntity.getPitch());

                return createdEntity;
            });

            world.spawnNewEntityAndPassengers(newEntity);

            spawnedEntities.set(index, newEntity);
        }

        timer = MathHelper.floor(delay * 20);
    }
}
