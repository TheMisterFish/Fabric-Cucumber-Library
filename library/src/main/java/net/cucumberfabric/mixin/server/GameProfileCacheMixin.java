package net.cucumberfabric.mixin.server;

import net.minecraft.server.players.GameProfileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.File;
import java.nio.file.Paths;

@Mixin(GameProfileCache.class)
public class GameProfileCacheMixin {
    private static String forcedDir;

    static {
        forcedDir = System.getProperty("cucumberfabric.server-dir");
    }

    @ModifyVariable(
            method = "<init>",
            at = @At("HEAD"),
            index = 2,
            argsOnly = true
    )
    private static File redirectPath(File value) {
        if (forcedDir != null && !forcedDir.isBlank()) {
            return Paths.get(forcedDir).resolve(value.getName()).toFile();
        }
        return value;
    }
}
