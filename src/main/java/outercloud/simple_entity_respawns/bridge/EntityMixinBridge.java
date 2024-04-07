package outercloud.simple_entity_respawns.bridge;

import net.minecraft.nbt.NbtCompound;

public interface EntityMixinBridge {
    NbtCompound getInitialNbt();

    void setInitialNbt(NbtCompound nbt);
}
