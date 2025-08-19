package net.cucumbergametest.mixin;

import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void injectGameTestTick(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        GameTestTicker.SINGLETON.tick();
    }
}
