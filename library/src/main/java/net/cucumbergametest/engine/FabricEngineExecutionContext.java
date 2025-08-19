package net.cucumbergametest.engine;

import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.logging.Logger;
import io.cucumber.core.logging.LoggerFactory;
import io.cucumber.core.options.CucumberOptionsAnnotationParser;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.plugin.PluginFactory;
import io.cucumber.core.plugin.Plugins;
import io.cucumber.core.runtime.*;
import io.cucumber.junit.platform.engine.CucumberEngineExecutionContext;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;

import java.time.Clock;
import java.util.Optional;
import java.util.function.Supplier;

import static io.cucumber.core.runtime.SynchronizedEventBus.synchronize;

public class FabricEngineExecutionContext implements EngineExecutionContext {
    private static final Logger log = LoggerFactory.getLogger(CucumberEngineExecutionContext.class);

    private CucumberExecutionContext context;
    private final boolean isParallelExecutionEnabled ;
    private final ClassLoader classloader;
    private RuntimeOptions options = RuntimeOptions.defaultOptions();

    public FabricEngineExecutionContext(ExecutionRequest request, ClassLoader classloader) {
        Optional<TestSource> sourceOpt = request.getRootTestDescriptor().getSource();
        Optional<Class<?>> testClassOpt = sourceOpt
                .filter(src -> src instanceof ClassSource)
                .map(src -> ((ClassSource) src).getJavaClass());

        ConfigurationParameters configParams = request.getConfigurationParameters();
        boolean parallelEnabled = Boolean.parseBoolean(
                configParams.get("cucumber.execution.parallel.enabled").orElse("false")
        );

        if(testClassOpt.isPresent()) {
            CucumberOptionsAnnotationParser parser = new CucumberOptionsAnnotationParser();
            this.options = parser.parse(testClassOpt.get()).build();
        }
        this.isParallelExecutionEnabled = parallelEnabled;
        this.classloader = classloader;

    }

    private CucumberExecutionContext createCucumberExecutionContext() {
        Supplier<ClassLoader> classLoader = () -> this.classloader;

        UuidGeneratorServiceLoader uuidGeneratorServiceLoader = new UuidGeneratorServiceLoader(classLoader,
                options);
        EventBus bus = synchronize(
                new TimeServiceEventBus(Clock.systemUTC(), uuidGeneratorServiceLoader.loadUuidGenerator()));
        ObjectFactoryServiceLoader objectFactoryServiceLoader = new ObjectFactoryServiceLoader(classLoader,
                options);
        Plugins plugins = new Plugins(new PluginFactory(), options);
        ExitStatus exitStatus = new ExitStatus(options);
        plugins.addPlugin(exitStatus);

        RunnerSupplier runnerSupplier;

        if (isParallelExecutionEnabled) {
            plugins.setSerialEventBusOnEventListenerPlugins(bus);
            ObjectFactorySupplier objectFactorySupplier = new ThreadLocalObjectFactorySupplier(
                    objectFactoryServiceLoader);
            BackendSupplier backendSupplier = new BackendServiceLoader(classLoader, objectFactorySupplier);
            runnerSupplier = new ThreadLocalRunnerSupplier(options, bus, backendSupplier, objectFactorySupplier);
        } else {
            plugins.setEventBusOnEventListenerPlugins(bus);
            ObjectFactorySupplier objectFactorySupplier = new SingletonObjectFactorySupplier(
                    objectFactoryServiceLoader);
            BackendSupplier backendSupplier = new BackendServiceLoader(classLoader, objectFactorySupplier);
            runnerSupplier = new SingletonRunnerSupplier(options, bus, backendSupplier, objectFactorySupplier);
        }
        return new CucumberExecutionContext(bus, exitStatus, runnerSupplier);
    }

    public void startTestRun() {
        log.debug(() -> "Starting Fabric test run");
        context = createCucumberExecutionContext();
        context.startTestRun();
    }

    public void finishTestRun() {
        log.debug(() -> "Finishing Fabric test run");
        context.finishTestRun();
    }
}
