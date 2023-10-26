package outercloud.simple_npcs;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class RespawnGroup {
    private String tag;
    private float delay;
    private int amount;

    private int timer;

    private ArrayList<RegistryKey<World>> worlds = new ArrayList<>();
    private ArrayList<Vec3d> positions = new ArrayList<>();
    private ArrayList<NbtCompound> nbts = new ArrayList<>();

    private ArrayList<UUID> spawnedEntities = new ArrayList<>();

    public RespawnGroup(String tag, float delay, int amount, MinecraftServer server, RespawnGroup source) {
        source.cleanup(server);

        this.tag = tag;
        this.delay = delay;
        this.amount = amount;
        this.timer = MathHelper.floor(delay * 20);

        this.worlds = source.worlds;
        this.positions = source.positions;
        this.nbts = source.nbts;

        if(nbts.isEmpty()) return;

        while(spawnedEntities.size() < amount) {
            spawnedEntities.add(null);
        }
    }

    public RespawnGroup(String tag, float delay, int amount, MinecraftServer server) {
        this.tag = tag;
        this.delay = delay;
        this.amount = amount;
        this.timer = MathHelper.floor(delay * 20);

        for(ServerWorld world : server.getWorlds()) {
            for(Entity entity : world.iterateEntities()) {
                if(!entity.getCommandTags().contains(this.tag)) continue;

                SimpleNpcs.deselect(entity);

                worlds.add(world.getRegistryKey());
                positions.add(entity.getPos());

                NbtCompound nbt = new NbtCompound();
                nbt.putString("id", Registries.ENTITY_TYPE.getId(entity.getType()).toString());
                entity.writeNbt(nbt);
                nbt.remove("UUID");

                nbts.add(nbt);

                spawnedEntities.add(entity.getUuid());
            }
        }

        if(nbts.isEmpty()) return;

        while(spawnedEntities.size() < amount) {
            spawnedEntities.add(null);
        }
    }

    public RespawnGroup(String tag, NbtCompound nbt) {
        this.tag = tag;
        delay = nbt.getFloat("delay");
        amount = nbt.getInt("amount");

        for(NbtElement element : nbt.getList("datas", NbtElement.COMPOUND_TYPE)) {
            NbtCompound spawnDataNbt = (NbtCompound) element;

            positions.add(new Vec3d(spawnDataNbt.getFloat("x"), spawnDataNbt.getFloat("y"), spawnDataNbt.getFloat("z")));
            worlds.add(RegistryKey.of(RegistryKeys.WORLD, new Identifier(spawnDataNbt.getString("world"))));
            nbts.add((NbtCompound) spawnDataNbt.get("data"));
        }

        for(NbtElement element : nbt.getList("spawnedEntities", NbtElement.COMPOUND_TYPE)) {
            NbtCompound spawnDataNbt = (NbtCompound) element;

            spawnedEntities.add(UUID.fromString(spawnDataNbt.getString("id")));
        }
    }

    public void writeNbt(NbtCompound nbt) {
        NbtCompound data = new NbtCompound();

        data.putFloat("delay", delay);
        data.putInt("amount", amount);

        NbtList spawnDatas = new NbtList();

        for(int index = 0; index < nbts.size(); index++){
            NbtCompound spawnData = new NbtCompound();
            spawnData.putFloat("x", (float) positions.get(index).x);
            spawnData.putFloat("y", (float) positions.get(index).y);
            spawnData.putFloat("z", (float) positions.get(index).z);

            spawnData.putString("world", worlds.get(index).getValue().toString());

            spawnData.put("data", nbts.get(index));

            spawnDatas.add(spawnData);
        }

        data.put("datas", spawnDatas);

        NbtList spawnedEntitiesData = new NbtList();

        for(UUID uuid : spawnedEntities){
            NbtCompound spawnedEntityData = new NbtCompound();
            spawnedEntityData.putString("id", uuid.toString());

            spawnedEntitiesData.add(spawnedEntityData);
        }

        data.put("spawnedEntities", spawnedEntitiesData);

        nbt.put(tag, data);
    }


    private Entity getEntity(UUID uuid, MinecraftServer server) {
        for(ServerWorld world : server.getWorlds()) {
            for(Entity entity : world.iterateEntities()) {
                if(entity.getUuid().equals(uuid)) return entity;
            }
        }

        return null;
    }

    public void tick(MinecraftServer server) {
        for(int index = 0; index < spawnedEntities.size(); index++) {
            Entity entity = getEntity(spawnedEntities.get(index), server);

            if(entity != null && entity.isAlive()) continue;

            timer--;

            if(timer > 0) break;

            Random random = new Random();

            int randomSpawnDataIndex = random.nextInt(nbts.size());

            ServerWorld world = server.getWorld(worlds.get(randomSpawnDataIndex));
            Vec3d position = positions.get(randomSpawnDataIndex);
            NbtCompound nbt = nbts.get(randomSpawnDataIndex);

            Entity newEntity = EntityType.loadEntityWithPassengers(nbt, world, (createdEntity) -> {
                createdEntity.refreshPositionAndAngles(position.x, position.y, position.z, createdEntity.getYaw(), createdEntity.getPitch());

                return createdEntity;
            });

            world.spawnNewEntityAndPassengers(newEntity);

            spawnedEntities.set(index, newEntity.getUuid());
        }

        if(timer <= 0) timer = MathHelper.floor(delay * 20);
    }

    public void reset(MinecraftServer server) {
        for(int index = 0; index < spawnedEntities.size(); index++) {
            Entity entity = getEntity(spawnedEntities.get(index), server);

            if(entity != null && entity.isAlive()) entity.discard();

            Random random = new Random();

            int randomSpawnDataIndex = random.nextInt(nbts.size());

            ServerWorld world = server.getWorld(worlds.get(randomSpawnDataIndex));
            Vec3d position = positions.get(randomSpawnDataIndex);
            NbtCompound nbt = nbts.get(randomSpawnDataIndex);

            Entity newEntity = EntityType.loadEntityWithPassengers(nbt, world, (createdEntity) -> {
                createdEntity.refreshPositionAndAngles(position.x, position.y, position.z, createdEntity.getYaw(), createdEntity.getPitch());

                return createdEntity;
            });

            world.spawnNewEntityAndPassengers(newEntity);

            spawnedEntities.set(index, newEntity.getUuid());
        }

        timer = MathHelper.floor(delay * 20);
    }

    public void cleanup(MinecraftServer server) {
        for (UUID spawnedEntity : spawnedEntities) {
            Entity entity = getEntity(spawnedEntity, server);

            if (entity != null && entity.isAlive()) entity.discard();
        }
    }

    public String getTag() {
        return tag;
    }
}
