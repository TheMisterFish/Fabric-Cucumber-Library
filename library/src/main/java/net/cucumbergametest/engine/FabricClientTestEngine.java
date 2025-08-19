package net.cucumbergametest.engine;

import io.cucumber.junit.platform.engine.CucumberTestEngine;
import org.junit.platform.engine.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class FabricClientTestEngine implements TestEngine {

    private final CucumberTestEngine delegate = new CucumberTestEngine();
    public static final String ID = "cucumber-fabric-client";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
        System.out.println("CLIENT TEST ENGINE:  discover?!");
        return delegate.discover(request, uniqueId);
    }

    @Override
    public void execute(ExecutionRequest request) {
//        System.out.println("CLIENT TEST ENGINE: execute?!");
//
//        ClassLoader loader = createClientClassLoader();
//        withLoader(loader, () -> {
//            delegate.execute(request);
//            return null;
//        });
    }

    private <T> void withLoader(ClassLoader loader, Supplier<T> action) {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(loader);
            action.get();
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    private ClassLoader createClientClassLoader() {
        try {
            String subRunDir = "run";
            Path runDir = Paths.get("cucumber_run");
            if (!Files.exists(runDir.resolve(subRunDir))) {
                Files.createDirectories(runDir);
            }

            //fabric properties
            System.setProperty("fabric.development", "true");
            System.setProperty("fabric.log.level", "info");
            System.setProperty("fabric.modsFolder", runDir.resolve("mods").toAbsolutePath().toString());
            //custom properties
            System.setProperty("custom.server.dir", runDir.toAbsolutePath().toString());

//            Knot.launch(new String[]{
//                    "--nogui",
//                    "gameDir", runDir.resolve(subRunDir).toAbsolutePath().toString()
//            }, EnvType.CLIENT);

            System.out.println(Thread.currentThread().getContextClassLoader());
            return Thread.currentThread().getContextClassLoader();
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to create run directory", ioe);
        }
    }
}
