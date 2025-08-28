package net.cucumberfabric.mixin.server;

import net.minecraft.server.dedicated.DedicatedServerSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.nio.file.Path;
import java.nio.file.Paths;

@Mixin(DedicatedServerSettings.class)
public class DedicatedServerSettingsMixin {
    @Unique
    private final static String forcedDir;

    static {
        forcedDir = System.getProperty("cucumberfabric.server-dir");
    }

    @ModifyVariable(
            method = "<init>(Ljava/nio/file/Path;)V",
            at = @At("HEAD"),
            index = 1,
            argsOnly = true
    )
    private static Path modifyConstructorPath(Path original) {
        if (forcedDir != null && !forcedDir.isBlank()) {
            return Paths.get(forcedDir, "server.properties");
        }
        return original;
    }
}
