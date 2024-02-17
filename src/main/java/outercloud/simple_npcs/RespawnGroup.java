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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class RespawnGroup {
    private class RespawnEntry {
        public ServerWorld world;
        public Vec3d position;
        public NbtCompound nbt;
    }

    private class SpawnEntry {
        public Entity entity;
        public RespawnEntry respawnEntry;
    }

    private String tag = "none";
    private float delay = 0;
    private int amount = 0;
    private boolean random = true;
    private float radius = 10;
    private boolean frozen = false;

    private int timer;

    private ArrayList<RespawnEntry> respawnEntries = new ArrayList<>();
    private ArrayList<SpawnEntry> spawnEntries = new ArrayList<>();

    private Random randomGenerator = new Random();

    // Creates a respawn group from another respawn group. Used for editing respawn groups
    public RespawnGroup(String tag, float delay, boolean random, int amount, float radius, MinecraftServer server, RespawnGroup source) {
        source.cleanup();

        this.tag = tag;
        this.delay = delay;
        this.random = random;
        this.amount = amount;
        this.radius = radius;
        this.timer = MathHelper.floor(delay * 20);

        this.frozen = source.frozen;

        this.respawnEntries = source.respawnEntries;

        if(amount == 0) amount = respawnEntries.size();

        populateRespawnEntries(server);

        while(spawnEntries.size() < amount) {
            spawnEntries.add(null);
        }

        reset(server);
    }

    // Creates a respawn group using entities with a given tag
    public RespawnGroup(String tag, float delay, boolean random, int amount, float radius, MinecraftServer server) {
        this.tag = tag;
        this.delay = delay;
        this.random = random;
        this.amount = amount;
        this.radius = radius;
        this.timer = MathHelper.floor(delay * 20);

        if(amount == 0) amount = respawnEntries.size();

        populateRespawnEntries(server);

        while(spawnEntries.size() < amount) {
            spawnEntries.add(null);
        }
    }

    // Creates a respawn group from NBT save data
    public RespawnGroup(String tag, NbtCompound nbt, MinecraftServer server) {
        this.tag = tag;
        delay = nbt.getFloat("delay");
        if(nbt.getKeys().contains("random")) random = nbt.getBoolean("random");
        if(nbt.getKeys().contains("frozen")) random = nbt.getBoolean("frozen");
        amount = nbt.getInt("amount");
        if(nbt.getKeys().contains("radius")) random = nbt.getBoolean("radius");

        for(NbtElement element : nbt.getList("datas", NbtElement.COMPOUND_TYPE)) {
            NbtCompound spawnDataNbt = (NbtCompound) element;

            RespawnEntry respawnEntry = new RespawnEntry();
            respawnEntry.position = new Vec3d(spawnDataNbt.getFloat("x"), spawnDataNbt.getFloat("y"), spawnDataNbt.getFloat("z"));
            respawnEntry.world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, new Identifier(spawnDataNbt.getString("world"))));
            respawnEntry.nbt = (NbtCompound) spawnDataNbt.get("data");

            respawnEntries.add(respawnEntry);
        }

        if(amount == 0) amount = respawnEntries.size();

        while(spawnEntries.size() < amount) {
            spawnEntries.add(null);
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

        for (RespawnEntry respawnEntry : respawnEntries) {
            NbtCompound spawnData = new NbtCompound();
            spawnData.putFloat("x", (float) respawnEntry.position.x);
            spawnData.putFloat("y", (float) respawnEntry.position.y);
            spawnData.putFloat("z", (float) respawnEntry.position.z);

            spawnData.putString("world", respawnEntry.world.getRegistryKey().getValue().toString());

            spawnData.put("data", respawnEntry.nbt);

            spawnDatas.add(spawnData);
        }

        data.put("datas", spawnDatas);

        nbt.put(tag, data);
    }

    public void tick(MinecraftServer server) {
        if(frozen) return;

        timer--;

        if(radius > 0) {
            for(SpawnEntry spawnEntry: spawnEntries) {
                if(spawnEntry == null) continue;

                if(!spawnEntry.entity.isAlive()) continue;

                if(spawnEntry.entity.getPos().distanceTo(spawnEntry.respawnEntry.position) <= radius) continue;

                spawnEntry.entity.teleport(spawnEntry.respawnEntry.position.x, spawnEntry.respawnEntry.position.y, spawnEntry.respawnEntry.position.z);
            }
        }

        if(timer > 0) return;

        for(int index = 0; index < spawnEntries.size(); index++) {
            SpawnEntry spawnEntry = spawnEntries.get(index);

            //

            if(spawnEntry != null) {
                if(spawnEntry.entity.isAlive()) continue;

                if(!spawnEntry.respawnEntry.world.isChunkLoaded(ChunkSectionPos.getSectionCoord(spawnEntry.entity.getPos().x), ChunkSectionPos.getSectionCoord(spawnEntry.entity.getPos().z))) continue;
            }

            spawnEntity(index, server);
        }

        timer = MathHelper.floor(delay * 20);
    }

    public void cleanup() {
        for(int index = 0; index < spawnEntries.size(); index++) {
            SpawnEntry spawnEntry = spawnEntries.get(index);

            if(spawnEntry == null) continue;

            spawnEntry.entity.discard();

            spawnEntries.set(index, null);
        }
    }

    public void reset(MinecraftServer server) {
        this.cleanup();

        for(int index = 0; index < amount; index++) {
            spawnEntity(index, server);
        }

        timer = MathHelper.floor(delay * 20);
    }

    private void populateRespawnEntries(MinecraftServer server) {
        for(ServerWorld world : server.getWorlds()) {
            for(Entity entity : world.iterateEntities()) {
                if(!entity.getCommandTags().contains(tag)) continue;

                RespawnEntry respawnEntry = new RespawnEntry();
                respawnEntry.world = world;
                respawnEntry.position = entity.getPos();

                NbtCompound nbt = new NbtCompound();
                nbt.putString("id", Registries.ENTITY_TYPE.getId(entity.getType()).toString());
                entity.writeNbt(nbt);
                nbt.remove("UUID");

                respawnEntry.nbt = nbt;

                respawnEntries.add(respawnEntry);
            }
        }
    }

    private void spawnEntity(int index, MinecraftServer server) {
        RespawnEntry respawnEntry;

        if(random) {
            respawnEntry = respawnEntries.get(randomGenerator.nextInt(respawnEntries.size()));
        } else {
            SpawnEntry spawnEntry = spawnEntries.get(index);

            if(spawnEntry == null) {
                respawnEntry = respawnEntries.get(index % respawnEntries.size());
            } else {
                respawnEntry = spawnEntry.respawnEntry;
            }

        }

        if(respawnEntry == null) return;

        RespawnEntry finalRespawnEntry = respawnEntry;
        Entity newEntity = EntityType.loadEntityWithPassengers(respawnEntry.nbt, respawnEntry.world, (createdEntity) -> {
            createdEntity.refreshPositionAndAngles(finalRespawnEntry.position.x, finalRespawnEntry.position.y, finalRespawnEntry.position.z, createdEntity.getYaw(), createdEntity.getPitch());

            return createdEntity;
        });

        respawnEntry.world.spawnNewEntityAndPassengers(newEntity);

        SpawnEntry spawnEntry = new SpawnEntry();
        spawnEntry.entity = newEntity;
        spawnEntry.respawnEntry = respawnEntry;

        spawnEntries.set(index, spawnEntry);
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
