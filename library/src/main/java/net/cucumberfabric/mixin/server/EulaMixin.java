package net.cucumberfabric.mixin.server;

import net.minecraft.server.Eula;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.nio.file.Path;
import java.nio.file.Paths;

@Mixin(Eula.class)
public class EulaMixin {
    private final static String forcedDir;

    static {
        forcedDir = System.getProperty("cucumberfabric.eula-location");
    }

    @ModifyVariable(
            method = "<init>(Ljava/nio/file/Path;)V",
            at = @At("HEAD"),
            index = 1,
            argsOnly = true
    )
    private static Path modifyConstructorPath(Path original) {
        if (forcedDir != null && !forcedDir.isBlank()) {
            return Paths.get(forcedDir, "eula.txt");
        }
        return original;
    }
}
