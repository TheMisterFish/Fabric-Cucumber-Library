package net.cucumberfabric.mixin;

import net.minecraft.server.players.StoredUserList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.File;
import java.nio.file.Paths;

@Mixin(StoredUserList.class)
public class StoredUserListMixin {
    @Unique
    private final static String forcedDir;

    static {
        forcedDir = System.getProperty("cucumberfabric.server-dir");
    }

    @ModifyVariable(
            method = "<init>",
            at = @At("HEAD"),
            index = 1,
            argsOnly = true
    )
    private static File redirectPath(File value) {
        if (forcedDir != null && !forcedDir.isBlank()) {
            return Paths.get(forcedDir).resolve(value.getName()).toFile();
        }
        return value;
    }
}
