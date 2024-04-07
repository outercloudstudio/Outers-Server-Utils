package outercloud.simple_entity_respawns;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;

public class PersistentState extends net.minecraft.world.PersistentState {
    public HashMap<String, RespawnGroup> respawnGroups = new HashMap<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound respawnGroupsData = new NbtCompound();

        for(String tag : respawnGroups.keySet()) {
            respawnGroupsData.put(tag, respawnGroups.get(tag).writeNbt());
        }

        nbt.put("respawn_groups", respawnGroupsData);

        return nbt;
    }

    public static PersistentState createFromNbt(NbtCompound nbt, MinecraftServer server) {
        PersistentState state = new PersistentState();

        NbtCompound respawnGroupsData = nbt.getCompound("respawn_groups");

        for(String tag : respawnGroupsData.getKeys()) {
            state.respawnGroups.put(tag, new RespawnGroup(respawnGroupsData.getCompound(tag), server));
        }

        return state;
    }

    public static PersistentState getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        PersistentState state = persistentStateManager.getOrCreate((NbtCompound nbt) -> PersistentState.createFromNbt(nbt, server), PersistentState::new, "simple_entity_respawns");

        state.markDirty();

        return state;
    }
}
