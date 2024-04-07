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
import outercloud.simple_npcs.bridge.EntityMixinBridge;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

public class RespawnGroup {
    public float delay = 0;
    public float radius = 10;

    public ArrayList<RespawningEntity> respawningEntities = new ArrayList<>();

    public RespawnGroup(float delay, float radius) {
        this.delay = delay;
        this.radius = radius;
    }

    public RespawnGroup(NbtCompound nbt, MinecraftServer server) {
        delay = nbt.getFloat("delay");
        radius = nbt.getFloat("radius");

        for(NbtElement nbtElement: nbt.getList("entities", NbtElement.COMPOUND_TYPE)) {
            respawningEntities.add(new RespawningEntity((NbtCompound) nbtElement, server));
        }
    }

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.putFloat("delay", delay);
        nbt.putFloat("radius", radius);

        NbtList entities = new NbtList();

        for(RespawningEntity entity: respawningEntities) {
            entities.add(entity.writeNbt());
        }

        nbt.put("entities", entities);

        return nbt;
    }

    public void tick() {
        for(int index = 0; index < respawningEntities.size(); index++) {
            RespawningEntity entity = respawningEntities.get(index);

            entity.delay -= 1f / 20f;

            if(entity.delay > 0f) continue;

            respawningEntities.remove(index);
            index--;

            Entity newEntity = EntityType.loadEntityWithPassengers(entity.nbt, entity.world, (createdEntity) -> createdEntity);

            if(newEntity == null) return;

            ((EntityMixinBridge) newEntity).setInitialNbt(entity.nbt);

            entity.world.spawnNewEntityAndPassengers(newEntity);
        }
    }
}
