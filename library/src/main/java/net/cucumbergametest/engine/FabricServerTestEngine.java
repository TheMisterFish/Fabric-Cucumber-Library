package net.cucumbergametest.engine;

import io.cucumber.junit.platform.engine.CucumberTestEngine;
import net.cucumbergametest.config.FabricRunConfiguration;
import net.cucumbergametest.descriptor.FabricServerEngineDescriptor;
import net.cucumbergametest.descriptor.FabricServerTestDescriptor;
import org.junit.platform.engine.*;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

import static io.cucumber.junit.platform.engine.Constants.FEATURES_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.JUNIT_PLATFORM_DISCOVERY_AS_ROOT_ENGINE_PROPERTY_NAME;

public class FabricServerTestEngine extends HierarchicalTestEngine<FabricEngineExecutionContext> {

    public static final String ID = "cucumber-fabric-server";

    private final CucumberTestEngine delegate = new CucumberTestEngine();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
        TestDescriptor root = delegate.discover(request, uniqueId);

        ConfigurationParameters configurationParameters = request.getConfigurationParameters();
        TestSource testSource = createEngineTestSource(configurationParameters);
        FabricRunConfiguration configuration = new FabricRunConfiguration(configurationParameters);
        FabricServerEngineDescriptor engineDescriptor = new FabricServerEngineDescriptor(uniqueId, configuration, testSource);

        if (!supportsDiscoveryAsRootEngine(configurationParameters) && isRootEngine(uniqueId)) {
            return engineDescriptor;
        }

        root.getChildren().forEach(testDescriptor -> {
            engineDescriptor.addChild(recursiveTestDescriptor(testDescriptor));
        });

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

        if (!testDescriptor.getChildren().isEmpty()) {
            testDescriptor.getChildren().forEach(childDescriptor -> {
                fabricServerTestDescriptor.addChild(recursiveTestDescriptor(childDescriptor));
            });
        }

        return fabricServerTestDescriptor;
    }

    @Override
    protected FabricEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        FabricRunConfiguration configuration = getFabricRunConfiguration(request);
        return new FabricEngineExecutionContext(configuration);
    }

    private FabricRunConfiguration getFabricRunConfiguration(ExecutionRequest request) {
        FabricServerEngineDescriptor engineDescriptor = (FabricServerEngineDescriptor) request.getRootTestDescriptor();
        return engineDescriptor.getConfiguration();
    }

    private static TestSource createEngineTestSource(ConfigurationParameters configurationParameters) {
        // Workaround. Test Engines do not normally have test source.
        // Maven does not count tests that do not have a ClassSource somewhere
        // in the test descriptor tree.
        // Gradle will report all tests as coming from an "Unknown Class"
        // See: https://github.com/cucumber/cucumber-jvm/pull/2498
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
}
