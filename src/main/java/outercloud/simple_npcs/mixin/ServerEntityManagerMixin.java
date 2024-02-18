package outercloud.simple_npcs.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.EntityTrackingStatus;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import outercloud.simple_npcs.RespawnGroup;

import java.util.List;

@Mixin(ServerEntityManager.class)
public class ServerEntityManagerMixin {
    @Inject(method = "updateTrackingStatus(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/entity/EntityTrackingStatus;)V", at = @At("HEAD"))
    void stopTracking(ChunkPos chunkPos, EntityTrackingStatus trackingStatus, CallbackInfo ci) {
        if(trackingStatus != EntityTrackingStatus.HIDDEN) return;

        ServerEntityManager<EntityLike> me = (ServerEntityManager<EntityLike>) (Object) this;

        SectionedEntityCache<EntityLike> cache = ((ServerEntityManagerAccessorMixin)me).getCache();
        List<EntityTrackingSection<EntityLike>> trackingSections = cache.getTrackingSections(chunkPos.toLong()).toList();

        for(EntityTrackingSection<EntityLike> trackingSection: trackingSections) {
            List<EntityLike> entities = trackingSection.stream().filter(entityLike -> entityLike instanceof Entity).filter(entityLike -> RespawnGroup.entityInARespawnGroup((Entity) entityLike)).toList();

            for(EntityLike entityLike: entities) {
                entityLike.setRemoved(Entity.RemovalReason.DISCARDED);
            }
        }
    }
}
