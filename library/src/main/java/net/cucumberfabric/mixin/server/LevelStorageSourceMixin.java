package net.cucumberfabric.mixin.server;

import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.nio.file.Path;
import java.nio.file.Paths;

@Mixin(LevelStorageSource.class)
public class LevelStorageSourceMixin {
    private static String forcedDir;

    static {
        forcedDir = System.getProperty("cucumberfabric.server-dir");
    }

    @ModifyVariable(
            method = "<init>",
            at = @At("HEAD"),
            index = 1,
            argsOnly = true
    )
    private static Path redirectPath(Path original) {
        if (forcedDir != null && !forcedDir.isBlank()) {
            return Paths.get(forcedDir);
        }
        return original;
    }
}
