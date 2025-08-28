package net.cucumberfabric.mixin.client;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
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

    @ModifyVariable(
            method = "<init>",
            at = @At("HEAD"),
            index = 1,
            argsOnly = true
    )
    private static GameConfig injectAssetFolder(GameConfig value) throws IOException {
        File file1 = value.location.gameDirectory;
        File file2 = value.location.resourcePackDirectory;
        File file3 = value.location.assetDirectory;
        String string = value.location.assetIndex;

        if (value.location.assetIndex == null || value.location.assetIndex.isEmpty()) {
            Path assets = Path.of(System.getProperty("user.home"), ".gradle/caches/fabric-loom/assets");
            if (Files.exists(assets) && Files.isDirectory(assets)) {
                file3 = assets.toAbsolutePath().toFile();

                String assetId = SharedConstants.getCurrentVersion().getId() + "-";
                Path indexesPath = assets.resolve("indexes");

                try (Stream<Path> stream = Files.list(indexesPath)) {
                    Optional<String> maxFilename = stream
                            .filter(Files::isRegularFile)
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .filter(name -> name.startsWith(assetId))
                            .filter(name -> name.endsWith(".json"))
                            .max(Comparator.comparingInt(name -> extractNumber(name, assetId)));

                    if (maxFilename.isPresent()) {
                        string = maxFilename.get().replace(".json", "");
                    }
                }
            }
        }
        GameConfig.FolderData folderData = new GameConfig.FolderData(file1, file2, file3, string);

        return new GameConfig(value.user, value.display, folderData, value.game, value.quickPlay);
    }

    @Unique
    private static int extractNumber(String filename, String prefix) {
        try {
            String numberPart = filename
                    .substring(prefix.length(), filename.lastIndexOf(".json"));
            return Integer.parseInt(numberPart);
        } catch (Exception e) {
            return -1;
        }
    }
}
