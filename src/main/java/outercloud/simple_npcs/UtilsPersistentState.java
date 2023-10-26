package outercloud.simple_npcs;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;

public class UtilsPersistentState extends PersistentState {
    public HashMap<String, RespawnGroup> respawnGroups = new HashMap<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound respawnGroupsData = new NbtCompound();

        for(RespawnGroup respawnGroup : respawnGroups.values()) {
            respawnGroup.writeNbt(respawnGroupsData);
        }

        nbt.put("respawnGroups", respawnGroupsData);

        return nbt;
    }

    public static UtilsPersistentState createFromNbt(NbtCompound nbt) {
        UtilsPersistentState state = new UtilsPersistentState();

        NbtCompound respawnGroupsData = nbt.getCompound("respawnGroups");

        for(String tag : respawnGroupsData.getKeys()) {
            state.respawnGroups.put(tag, new RespawnGroup(tag, respawnGroupsData.getCompound(tag)));
        }

        return state;
    }

    public static UtilsPersistentState getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        UtilsPersistentState state = persistentStateManager.getOrCreate(UtilsPersistentState::createFromNbt, UtilsPersistentState::new, "ocsudl");

        state.markDirty();

        return state;
    }
}
