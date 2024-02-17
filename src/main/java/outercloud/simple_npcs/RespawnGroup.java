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
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class RespawnGroup {
    private String tag = "none";
    private float delay = 0;
    private int amount = 0;
    private boolean random = true;
    private float radius = 10;
    private boolean frozen = false;

    private int timer;

    private ArrayList<RegistryKey<World>> worlds = new ArrayList<>();
    private ArrayList<Vec3d> positions = new ArrayList<>();
    private ArrayList<NbtCompound> nbts = new ArrayList<>();

    private ArrayList<UUID> spawnedEntities = new ArrayList<>();
    private ArrayList<Vec3d> spawnedPositions = new ArrayList<>();

    public RespawnGroup(String tag, float delay, boolean random, int amount, float radius, MinecraftServer server, RespawnGroup source) {
        source.cleanup(server);

        this.tag = tag;
        this.delay = delay;
        this.random = random;
        this.amount = amount;
        this.timer = MathHelper.floor(delay * 20);
        this.radius = radius;
        this.frozen = source.frozen;

        this.worlds = source.worlds;
        this.positions = source.positions;
        this.nbts = source.nbts;

        this.spawnedEntities = new ArrayList<>();
        this.spawnedPositions = new ArrayList<>();

        for(int i = 0; i < source.spawnedEntities.size(); i++) {
            spawnedEntities.add(source.spawnedEntities.get(i));
            spawnedPositions.add(source.spawnedPositions.get(i));
        }

        if(nbts.isEmpty()) return;

        while(spawnedEntities.size() < amount) {
            spawnedEntities.add(null);
            spawnedPositions.add(null);
        }

        reset(server);
    }

    public RespawnGroup(String tag, float delay, boolean random, int amount, float radius, MinecraftServer server) {
        this.tag = tag;
        this.delay = delay;
        this.random = random;
        this.amount = amount;
        this.radius = radius;
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
                spawnedPositions.add(entity.getPos());
            }
        }

        if(nbts.isEmpty()) return;

        while(spawnedEntities.size() < amount) {
            spawnedEntities.add(null);
            spawnedPositions.add(null);
        }
    }

    public RespawnGroup(String tag, NbtCompound nbt) {
        this.tag = tag;
        delay = nbt.getFloat("delay");
        if(nbt.getKeys().contains("random")) random = nbt.getBoolean("random");
        if(nbt.getKeys().contains("frozen")) random = nbt.getBoolean("frozen");
        amount = nbt.getInt("amount");
        if(nbt.getKeys().contains("radius")) random = nbt.getBoolean("radius");


        for(NbtElement element : nbt.getList("datas", NbtElement.COMPOUND_TYPE)) {
            NbtCompound spawnDataNbt = (NbtCompound) element;

            positions.add(new Vec3d(spawnDataNbt.getFloat("x"), spawnDataNbt.getFloat("y"), spawnDataNbt.getFloat("z")));
            worlds.add(RegistryKey.of(RegistryKeys.WORLD, new Identifier(spawnDataNbt.getString("world"))));
            nbts.add((NbtCompound) spawnDataNbt.get("data"));
        }

        for(NbtElement element : nbt.getList("spawnedEntities", NbtElement.COMPOUND_TYPE)) {
            NbtCompound spawnDataNbt = (NbtCompound) element;

            String uuidString = spawnDataNbt.getString("id");

            if(!uuidString.equals("null")) {
                spawnedEntities.add(UUID.fromString(uuidString));
            } else {
                spawnedEntities.add(null);
            }

            NbtCompound position = spawnDataNbt.getCompound("position");

            if(position == null) {
                spawnedPositions.add(null);
            } else {
                spawnedPositions.add(new Vec3d(position.getFloat("x"), position.getFloat("y"), position.getFloat("z")));
            }
        }
    }

    public void writeNbt(NbtCompound nbt) {
        NbtCompound data = new NbtCompound();

        data.putFloat("delay", delay);
        data.putBoolean("random", random);
        data.putBoolean("frozen", frozen);
        data.putInt("amount", amount);
        data.putFloat("radius", radius);

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

        for(int i = 0; i < spawnedEntities.size(); i++){
            UUID uuid = spawnedEntities.get(i);
            Vec3d position = spawnedPositions.get(i);

            NbtCompound spawnedEntityData = new NbtCompound();

            if(uuid != null) {
                spawnedEntityData.putString("id", uuid.toString());
            } else {
                spawnedEntityData.putString("id", "null");
            }

            if(position != null) {
                NbtCompound positionCompound = new NbtCompound();
                positionCompound.putFloat("x", (float) position.x);
                positionCompound.putFloat("y", (float) position.y);
                positionCompound.putFloat("z", (float) position.z);

                spawnedEntityData.put("position", positionCompound);
            }

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
        if(frozen) return;

        for(int index = 0; index < spawnedEntities.size(); index++) {
            Entity entity = getEntity(spawnedEntities.get(index), server);

            Vec3d position = Vec3d.ZERO;

            if(index < positions.size()) position = positions.get(index);

            if(radius != 0 && entity != null && entity.isAlive() && entity.getPos().distanceTo(position) > radius) entity.teleport(position.x, position.y, position.z);

            if(entity != null && entity.isAlive()) continue;

            timer--;

            if(timer > 0) break;

            this.spawnEntity(index, server);
        }

        if(timer <= 0) timer = MathHelper.floor(delay * 20);
    }

    public void reset(MinecraftServer server) {
        for(int index = 0; index < spawnedEntities.size(); index++) {
            Entity entity = getEntity(spawnedEntities.get(index), server);

            if(entity != null && entity.isAlive()) entity.discard();

            this.spawnEntity(index, server);
        }

        timer = MathHelper.floor(delay * 20);
    }

    private void spawnEntity(int index, MinecraftServer server) {
        Random randomGenerator = new Random();

        int spawnDataIndex = index;

        if(random || index >= nbts.size()) spawnDataIndex = randomGenerator.nextInt(nbts.size());

        ServerWorld world = server.getWorld(worlds.get(spawnDataIndex));
        Vec3d position = positions.get(spawnDataIndex);
        NbtCompound nbt = nbts.get(spawnDataIndex);

        if(world == null) return;

        if(!world.isChunkLoaded(ChunkSectionPos.getSectionCoord(position.x), ChunkSectionPos.getSectionCoord(position.z))) return;

        Entity newEntity = EntityType.loadEntityWithPassengers(nbt, world, (createdEntity) -> {
            createdEntity.refreshPositionAndAngles(position.x, position.y, position.z, createdEntity.getYaw(), createdEntity.getPitch());

            return createdEntity;
        });

        if(newEntity == null) return;

        world.spawnNewEntityAndPassengers(newEntity);

        spawnedEntities.set(index, newEntity.getUuid());

        if(positions.size() < nbts.size()) positions.add(newEntity.getPos());
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

    public void freeze() {
        frozen = true;
    }

    public void unfreeze() {
        frozen = false;
    }
}
