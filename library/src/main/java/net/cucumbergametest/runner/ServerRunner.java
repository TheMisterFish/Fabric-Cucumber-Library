//package net.cucumbergametest.runner;
//
//import io.cucumber.junit.Cucumber;
//import net.fabricmc.api.EnvType;
//import net.fabricmc.loader.impl.launch.knot.Knot;
//import net.minecraft.server.dedicated.DedicatedServerProperties;
//import org.junit.runner.Description;
//import org.junit.runner.Runner;
//import org.junit.runner.notification.RunNotifier;
//import org.junit.runners.model.InitializationError;
//
//import java.io.IOException;
//import java.io.UncheckedIOException;
//import java.io.Writer;
//import java.lang.reflect.Constructor;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.List;
//import java.util.Properties;
//
//public class ServerRunner  extends Runner {
//    private final Runner delegate;
//
//    /**
//     * Called by JUnit when you annotate your test with
//     * @RunWith(ServerRunner.class)
//     */
//    public ServerRunner(Class<?> testClass) throws InitializationError {
//        try {
//            // Build your isolated loader
//            ClassLoader isolated = buildIsolatedLoader();
//
//            // Grab the single-arg Cucumber constructor
//            @SuppressWarnings("unchecked")
//            Class<? extends Runner> cucumberClass =
//                    (Class<? extends Runner>) Class.forName("io.cucumber.junit.Cucumber");
//            Constructor<? extends Runner> ctor =
//                    cucumberClass.getConstructor(Class.class);
//
//            // TEMP: swap in the isolated loader so scanning loads your steps
//            Thread current = Thread.currentThread();
//            ClassLoader original = current.getContextClassLoader();
//            current.setContextClassLoader(isolated);
//            try {
//                // Instantiate the runner; all @CucumberOptions scanning uses `isolated`
//                this.delegate = ctor.newInstance(testClass);
//            } finally {
//                // restore the app’s normal loader
//                current.setContextClassLoader(original);
//            }
//
//        } catch (Exception e) {
//            throw new InitializationError(e);
//        }
//    }
//
//    private static ClassLoader buildIsolatedLoader() throws Exception {
//        try {
//            Path runDir = Paths.get("cucumber_run");
//            if (!Files.exists(runDir.resolve("run"))) {
//                Files.createDirectories(runDir);
//            }
//
//            //fabric properties
//            System.setProperty("fabric.development", "true");
//            System.setProperty("fabric.log.level", "info");
//            System.setProperty("fabric.modsFolder", runDir.resolve("mods").toAbsolutePath().toString());
//            //custom properties
//            System.setProperty("custom.server.dir", runDir.toAbsolutePath().toString());
//            System.setProperty("custom.server.eula.location", runDir.toAbsolutePath().toString());
//
//            Path path = runDir.resolve("server.properties");
//            Properties properties = new Properties();
//            new DedicatedServerProperties(properties);
//
//            //set properties here
//
//            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
//                properties.store(writer, "Minecraft server properties");
//            } catch (IOException ex) {
//                throw new UncheckedIOException(String.format("Failed to store properties to file: %s", path), ex);
//            }
//
//            Knot.launch(new String[]{
//                    "--nogui"
//            }, EnvType.SERVER);
//
//            return new ChildFirstClassLoader(
//                    new URL[]{  },
//                    Thread.currentThread().getContextClassLoader()
//            );
//        } catch (IOException ioe) {
//            throw new RuntimeException("Failed to create run directory", ioe);
//        }
//    }
//
//    @Override
//    public Description getDescription() {
//        return delegate.getDescription();
//    }
//
//    @Override
//    public void run(RunNotifier notifier) {
//        delegate.run(notifier);
//    }
//
//    public static class ChildFirstClassLoader extends URLClassLoader {
//        public ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
//            super(urls, parent);
//        }
//
//        @Override
//        protected Class<?> loadClass(String name, boolean resolve)
//                throws ClassNotFoundException {
//            synchronized (getClassLoadingLock(name)) {
//                if (name.startsWith("steps") || name.endsWith("steps") || name.startsWith("net.cucumbergametest")) {
//                    try {
//                        Class<?> c = findClass(name);
//                        if (resolve) {
//                            resolveClass(c);
//                        }
//                        return c;
//                    } catch (ClassNotFoundException ignored) {
//                        // fall through to parent
//                    }
//                }
//                return super.loadClass(name, resolve);
//            }
//        }
//    }
//}