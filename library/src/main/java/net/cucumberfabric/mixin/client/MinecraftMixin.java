package net.cucumberfabric.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow protected abstract Thread getRunningThread();

    @Redirect(
            method = "destroy",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/System;exit(I)V"
            )
    )
    private void redirectExit(int status) {
        // swallow the exit call
    }
}
