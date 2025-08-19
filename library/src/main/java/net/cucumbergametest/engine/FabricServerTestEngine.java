package net.cucumbergametest.engine;

import io.cucumber.core.feature.FeatureIdentifier;
import io.cucumber.gherkin.GherkinParser;
import io.cucumber.junit.platform.engine.CucumberTestEngine;
import net.cucumbergametest.descriptor.FabricServerEngineDescriptor;
import net.cucumbergametest.descriptor.FabricServerTestDescriptor;
import net.cucumbergametest.resolver.CustomFileContainerSelectorResolver;
import net.cucumbergametest.resolver.CustomOrderingVisitor;
import net.cucumbergametest.resolver.FabicFeatureResolver;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.junit.platform.engine.*;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.cucumber.core.feature.FeatureIdentifier.isFeature;
import static io.cucumber.junit.platform.engine.Constants.FEATURES_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.JUNIT_PLATFORM_DISCOVERY_AS_ROOT_ENGINE_PROPERTY_NAME;
import static org.junit.platform.engine.support.discovery.DiscoveryIssueReporter.deduplicating;
import static org.junit.platform.engine.support.discovery.DiscoveryIssueReporter.forwarding;

public class FabricServerTestEngine extends HierarchicalTestEngine<FabricEngineExecutionContext> {

    public static final String ID = "cucumber-fabric-server";
    public static final String FILTER_TAG = "@GameTestServer";
    private static final Logger log = LoggerFactory.getLogger(FabricServerTestEngine.class);

    private final CucumberTestEngine delegate = new CucumberTestEngine();

    private final GherkinParser gherkinParser = GherkinParser.builder().build();

    private final ClassLoader customClassLoader =
            new URLClassLoader(
                    new URL[]{ /* your jar URLs */},
                    getClass().getClassLoader()
            );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
        var root = delegate.discover(request, uniqueId);
        ConfigurationParameters configurationParameters = request.getConfigurationParameters();
        TestSource testSource = createEngineTestSource(configurationParameters);
        FabricServerEngineDescriptor engineDescriptor = new FabricServerEngineDescriptor(uniqueId, testSource);

        DiscoveryIssueReporter issueReporter = deduplicating(forwarding( //
                request.getDiscoveryListener(), //
                engineDescriptor.getUniqueId() //
        ));

        // Early out if Cucumber is the root engine and discovery has been
        // explicitly disabled. Workaround for:
        // https://github.com/sbt/sbt-jupiter-interface/issues/142
        if (!supportsDiscoveryAsRootEngine(configurationParameters) && isRootEngine(uniqueId)) {
            return engineDescriptor;
        }

        root.getChildren().forEach(testDescriptor -> {
            engineDescriptor.addChild(recursiveTestDescriptor(testDescriptor));
        });
//
//        EngineDiscoveryRequestResolver<FabricServerEngineDescriptor> resolver = EngineDiscoveryRequestResolver
//                .<FabricServerEngineDescriptor>builder()
//                .addSelectorResolver(context -> new CustomFileContainerSelectorResolver( //
//                        FeatureIdentifier::isFeature //
//                ))
//                .addResourceContainerSelectorResolver(resource -> isFeature(resource.getName()))
//                .addSelectorResolver(context -> new FabicFeatureResolver(
//                        context.getEngineDescriptor().getConfiguration(), //
//                        context.getPackageFilter(), //
//                        context.getIssueReporter() //
//                ))
//                .addTestDescriptorVisitor(context -> new CustomOrderingVisitor(
//                        context.getDiscoveryRequest().getConfigurationParameters() //
//                ))
//                .build();
//
//        List<Path> featureFiles = findAllFeatureFiles();

//        resolver.resolve(request, engineDescriptor);

//        for (Path featurePath : featureFiles) {
//            UniqueId featureId = uniqueId.append("feature", featurePath.toString());
//            try (Stream<Envelope> envelopes = gherkinParser.parse(featurePath)) {
//                FabricServerTestDescriptor featureDescriptor = new FabricServerTestDescriptor(featureId, "Feature");
//                envelopes
//                        .map(Envelope::getPickle)
//                        .filter(Objects::nonNull)
//                        .filter(Optional::isPresent)
//                        .filter(pickle -> pickle.get().getTags().stream()
//                                .map(PickleTag::getName)
//                                .anyMatch(FILTER_TAG::contains))
//                        .forEach(pickle -> {
//                            String scenarioName = pickle.get().getName();
//                            UniqueId scenarioId = featureId.append("scenario", scenarioName);
//                            FileSource fileSource = FileSource.from(featurePath.toFile());
//                            FabricServerTestDescriptor scenarioDesc =
//                                    new FabricServerTestDescriptor(scenarioId, scenarioName, fileSource);
//
//                            featureDescriptor.addChild(scenarioDesc);
//                        });
//
//                if (!featureDescriptor.getChildren().isEmpty()) {
//                    engineDescriptor.addChild(featureDescriptor);
//                } else {
//                    System.out.println("KAAS");
//                }
//
//            } catch (IOException e) {
//                issueReporter.reportIssue(DiscoveryIssue.create(
//                        DiscoveryIssue.Severity.ERROR,
//                        "Failed to parse feature " + featurePath + ": " + e.getMessage()
//                ));
//            }
//        }

        return engineDescriptor;
    }

    public FabricServerTestDescriptor recursiveTestDescriptor(TestDescriptor testDescriptor) {
        FabricServerTestDescriptor fabricServerTestDescriptor;
        if (testDescriptor.getSource().isPresent()) {
            fabricServerTestDescriptor =
                    new FabricServerTestDescriptor(testDescriptor.getUniqueId(), testDescriptor.getDisplayName(), testDescriptor.getSource().get());
        } else {
            fabricServerTestDescriptor =
                    new FabricServerTestDescriptor(testDescriptor.getUniqueId(), testDescriptor.getDisplayName());
        }

        if(!testDescriptor.getChildren().isEmpty()){
            testDescriptor.getChildren().forEach(childDescriptor -> {
                fabricServerTestDescriptor.addChild(recursiveTestDescriptor(childDescriptor));
            });
        }

        return fabricServerTestDescriptor;
    }

    private List<Path> findAllFeatureFiles() {
        try {
            // Simple example: walk src/test/resources
            Path root = Paths.get("src", "test", "resources");
            if (!Files.exists(root)) {
                return Collections.emptyList();
            }
            try (Stream<Path> stream = Files.walk(root)) {
                return stream
                        .filter(p -> p.toString().endsWith(".feature"))
                        .toList();
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    @Override
    protected FabricEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        return new FabricEngineExecutionContext(request, customClassLoader);
    }

    private static TestSource createEngineTestSource(ConfigurationParameters configurationParameters) {
        // Workaround. Test Engines do not normally have test source.
        // Maven does not count tests that do not have a ClassSource somewhere
        // in the test descriptor tree.
        // Gradle will report all tests as coming from an "Unknown Class"
        // See: https://github.com/cucumber/cucumber-jvm/pull/2498
        System.out.println(configurationParameters.keySet());
        if (configurationParameters.get(FEATURES_PROPERTY_NAME).isPresent()) {
            return ClassSource.from(FabricServerTestEngine.class);
        }
        return null;
    }

    private static boolean supportsDiscoveryAsRootEngine(ConfigurationParameters configurationParameters) {
        return configurationParameters.getBoolean(JUNIT_PLATFORM_DISCOVERY_AS_ROOT_ENGINE_PROPERTY_NAME)
                .orElse(true);
    }

    private boolean isRootEngine(UniqueId uniqueId) {
        UniqueId cucumberRootEngineId = UniqueId.forEngine(getId());
        return uniqueId.hasPrefix(cucumberRootEngineId);
    }
//
//    @Override
//    public void execute(ExecutionRequest request) {
//        if (!request.getRootTestDescriptor().getChildren().isEmpty()) {
//            Thread current = Thread.currentThread();
//            ClassLoader original = current.getContextClassLoader();
//            try {
//                current.setContextClassLoader(customClassLoader);
//                delegate.execute(request);
//            } finally {
//                current.setContextClassLoader(original);
//            }
//        }
//    }

//            Thread thread = Thread.currentThread();
//            ClassLoader prev = thread.getContextClassLoader();
//
//            if (fabricClassLoader == null) {
//                fabricClassLoader = createServerLoader();
//            }
//
//            thread.setContextClassLoader(fabricClassLoader);
//
//            try {
//                System.out.println(Thread.currentThread().getContextClassLoader());
//                delegate = withLoader(fabricClassLoader, CucumberTestEngine::new);
//                delegate.execute(request);
//            } finally {
//                thread.setContextClassLoader(prev);
//            }

    private <T> T withLoader(ClassLoader loader, Supplier<T> action) {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(loader);
            return action.get();
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    private ClassLoader createServerLoader() {
        try {
            Path runDir = Paths.get("cucumber_run");
            if (!Files.exists(runDir.resolve("run"))) {
                Files.createDirectories(runDir);
            }

            //fabric properties
            System.setProperty("fabric.development", "true");
            System.setProperty("fabric.log.level", "info");
            System.setProperty("fabric.modsFolder", runDir.resolve("mods").toAbsolutePath().toString());
            //custom properties
            System.setProperty("custom.server.dir", runDir.toAbsolutePath().toString());
            System.setProperty("custom.server.eula.location", runDir.toAbsolutePath().toString());

            Path path = runDir.resolve("server.properties");
            Properties properties = new Properties();
            new DedicatedServerProperties(properties);

            //set properties here

            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                properties.store(writer, "Minecraft server properties");
            } catch (IOException ex) {
                throw new UncheckedIOException(String.format("Failed to store properties to file: %s", path), ex);
            }

            Knot.launch(new String[]{
                    "--nogui"
            }, EnvType.SERVER);

            System.out.println(Thread.currentThread().getContextClassLoader());
            return Thread.currentThread().getContextClassLoader();
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to create run directory", ioe);
        }
    }
}
