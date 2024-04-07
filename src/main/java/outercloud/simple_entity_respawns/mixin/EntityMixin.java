package outercloud.simple_entity_respawns.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import outercloud.simple_entity_respawns.RespawnGroup;
import outercloud.simple_entity_respawns.SimpleEntityRespawns;
import outercloud.simple_entity_respawns.bridge.EntityMixinBridge;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityMixinBridge {
    private NbtCompound initialNbt;
    private Vec3d origin;

    private boolean addingTag = false;

    @Inject(method = "addCommandTag", at = @At("HEAD"))
    void addTagHead(String tag, CallbackInfoReturnable<Boolean> cir){
        if(!(((Entity) (Object) this) instanceof LivingEntity)) return;

        LivingEntity me = ((LivingEntity) (Object) this);

        if(((Entity) (Object) this).getCommandTags().size() >= 1024) return; // Return if adding the tag would fail

        if(SimpleEntityRespawns.getRespawnGroup(me) != null) return; // Return if we are already in a respawn group

        addingTag = true;
    }

    @Inject(method = "addCommandTag", at = @At("TAIL"))
    void addTagTail(String tag, CallbackInfoReturnable<Boolean> cir){
        if(!addingTag) return;
        addingTag = false;

        LivingEntity me = ((LivingEntity) (Object) this);

        origin = me.getPos();

        initialNbt = new NbtCompound();
        me.writeNbt(initialNbt);

        if(initialNbt.contains("initial_nbt")) initialNbt.remove("initial_nbt");
    }

    @Inject(method="tick", at = @At("HEAD"))
    void tick(CallbackInfo ci) {
        if(!(((Entity) (Object) this) instanceof LivingEntity)) return;

        LivingEntity me = ((LivingEntity) (Object) this);

        if(me.getWorld().isClient) return;

        RespawnGroup respawnGroup = SimpleEntityRespawns.getRespawnGroup(me);

        if(respawnGroup == null) return;

        if(origin == null) return;

        if(origin.distanceTo(me.getPos()) < respawnGroup.radius) return;

        me.teleport(origin.x, origin.y, origin.z);
    }

    @Inject(method="writeNbt", at = @At("HEAD"))
    void writeNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        if(initialNbt != null) nbt.put("initial_nbt", initialNbt);

        if(origin != null) nbt.putDouble("origin_x", origin.x);
        if(origin != null) nbt.putDouble("origin_y", origin.y);
        if(origin != null) nbt.putDouble("origin_z", origin.z);
    }

    @Inject(method="readNbt", at = @At("HEAD"))
    void readNbt(NbtCompound nbt, CallbackInfo ci) {
        initialNbt = nbt.getCompound("initial_nbt");

        origin = null;
        if(nbt.contains("origin_x")) origin = new Vec3d(nbt.getDouble("origin_x"), nbt.getDouble("origin_y"), nbt.getDouble("origin_z"));
    }

    @Override
    public NbtCompound getInitialNbt() {
        return initialNbt;
    }

    @Override
    public void setInitialNbt(NbtCompound nbt) {
        initialNbt = nbt;
    }
}
