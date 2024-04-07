package outercloud.simple_npcs;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import outercloud.simple_npcs.bridge.EntityMixinBridge;

public class RespawningEntity {
    public NbtCompound nbt;
    public float delay;
    public ServerWorld world;

    public RespawningEntity(LivingEntity entity, float delay, ServerWorld world) {
        nbt = ((EntityMixinBridge) entity).getInitialNbt();

        if(!nbt.contains("id")) nbt.putString("id", Registries.ENTITY_TYPE.getId(entity.getType()).toString());
        if(nbt.contains("UUID")) nbt.remove("UUID");

        this.delay = delay;
        this.world = world;
    }

    public RespawningEntity(NbtCompound nbt, MinecraftServer server) {
        this.nbt = nbt.getCompound("nbt");
        delay = nbt.getFloat("delay");
        world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, new Identifier(nbt.getString("world"))));
    }

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.put("nbt", nbt);
        nbt.putFloat("delay", delay);
        nbt.putString("world", world.getRegistryKey().getValue().toString());

        return nbt;
    }
}
