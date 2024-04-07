package outercloud.simple_npcs.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import outercloud.simple_npcs.SimpleNpcs;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "onDeath", at = @At("TAIL"))
    void onDied(DamageSource damageSource, CallbackInfo ci){
        LivingEntity me = (LivingEntity) (Object) this;

        if(me.getWorld().isClient) return;

        SimpleNpcs.entityDied(me);
    }
}
