package outercloud.simple_npcs.bridge;

import net.minecraft.nbt.NbtCompound;

public interface EntityMixinBridge {
    NbtCompound getInitialNbt();

    void setInitialNbt(NbtCompound nbt);
}
