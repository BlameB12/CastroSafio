package com.castrosafio.bossbalance.mixin;

import com.castrosafio.bossbalance.CastroSafioBossBalance;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityRemovalMixin {

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void castrosafio$protectBossFromExecuteRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (CastroSafioBossBalance.shouldBlockDirectRemoval(self, reason)) {
            ci.cancel();
        }
    }
}
