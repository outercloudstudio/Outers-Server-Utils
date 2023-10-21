package outercloud.server_utils;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Set;

public class UtilsPersistentState extends PersistentState {
    public HashMap<String, RespawnGroup> respawnGroups = new HashMap<>();

    private static final Type<UtilsPersistentState> TYPE = new Type<>(UtilsPersistentState::new, UtilsPersistentState::createFromNbt, null);

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        ServerUtils.LOGGER.info("Writing from nbt");

        NbtCompound respawnGroupsData = new NbtCompound();

        for(RespawnGroup respawnGroup : respawnGroups.values()) {
            respawnGroup.writeNbt(respawnGroupsData);
        }

        nbt.put("respawnGroups", respawnGroupsData);

        try {
            ServerUtils.LOGGER.info(String.valueOf(respawnGroupsData));
        } catch(Exception exception) {

        }

        return nbt;
    }

    public static UtilsPersistentState createFromNbt(NbtCompound nbt) {
        ServerUtils.LOGGER.info("Creating from nbt");

        UtilsPersistentState state = new UtilsPersistentState();

        NbtCompound respawnGroupsData = nbt.getCompound("respawnGroups");

        ServerUtils.LOGGER.info(String.valueOf(respawnGroupsData));

        for(String tag : respawnGroupsData.getKeys()) {
            ServerUtils.LOGGER.info("Creating spawn group " + tag);
            state.respawnGroups.put(tag, new RespawnGroup(tag, respawnGroupsData.getCompound(tag)));
        }

        return state;
    }

    public static UtilsPersistentState getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        UtilsPersistentState state = persistentStateManager.getOrCreate(TYPE, "ocsudl");

        state.markDirty();

        return state;
    }
}
