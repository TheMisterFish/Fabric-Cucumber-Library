package net.cucumbergametest.engine;

import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.gherkin.Pickle;
import io.cucumber.core.logging.Logger;
import io.cucumber.core.logging.LoggerFactory;
import io.cucumber.core.plugin.PluginFactory;
import io.cucumber.core.plugin.Plugins;
import io.cucumber.core.runtime.*;
import io.cucumber.junit.platform.engine.CucumberConfiguration;
import io.cucumber.junit.platform.engine.CucumberEngineExecutionContext;
import io.cucumber.junit.platform.engine.TestCaseResultObserver;
import net.cucumbergametest.config.FabricRunConfiguration;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;

import java.net.URL;
import java.net.URLClassLoader;
import java.time.Clock;
import java.util.function.Supplier;

import static io.cucumber.core.runtime.SynchronizedEventBus.synchronize;
import static io.cucumber.junit.platform.engine.TestCaseResultObserver.observe;

public class FabricEngineExecutionContext implements EngineExecutionContext {
    private static final Logger log = LoggerFactory.getLogger(CucumberEngineExecutionContext.class);

    private CucumberExecutionContext context;
    private final FabricRunConfiguration configuration;

    public FabricEngineExecutionContext(FabricRunConfiguration configuration) {
        this.configuration = configuration;
    }

    private CucumberExecutionContext createCucumberExecutionContext() {
        //TODO CHANGE INTO FABRIC SERVER CLASSLOADER
        Supplier<ClassLoader> classLoader = () -> new URLClassLoader(
                new URL[]{ /* your jar URLs */},
                getClass().getClassLoader()
        );

        UuidGeneratorServiceLoader uuidGeneratorServiceLoader = new UuidGeneratorServiceLoader(classLoader,
                configuration);
        EventBus bus = synchronize(
                new TimeServiceEventBus(Clock.systemUTC(), uuidGeneratorServiceLoader.loadUuidGenerator()));
        ObjectFactoryServiceLoader objectFactoryServiceLoader = new ObjectFactoryServiceLoader(classLoader,
                configuration);
        Plugins plugins = new Plugins(new PluginFactory(), configuration);
        ExitStatus exitStatus = new ExitStatus(configuration);
        plugins.addPlugin(exitStatus);

        RunnerSupplier runnerSupplier;

        if (configuration.isParallelExecutionEnabled()) {
            plugins.setSerialEventBusOnEventListenerPlugins(bus);
            ObjectFactorySupplier objectFactorySupplier = new ThreadLocalObjectFactorySupplier(
                    objectFactoryServiceLoader);
            BackendSupplier backendSupplier = new BackendServiceLoader(classLoader, objectFactorySupplier);
            runnerSupplier = new ThreadLocalRunnerSupplier(configuration, bus, backendSupplier, objectFactorySupplier);
        } else {
            plugins.setEventBusOnEventListenerPlugins(bus);
            ObjectFactorySupplier objectFactorySupplier = new SingletonObjectFactorySupplier(
                    objectFactoryServiceLoader);
            BackendSupplier backendSupplier = new BackendServiceLoader(classLoader, objectFactorySupplier);
            runnerSupplier = new SingletonRunnerSupplier(configuration, bus, backendSupplier, objectFactorySupplier);
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

    public FabricRunConfiguration getConfiguration() {
        return configuration;
    }

    public void runTestCase(Pickle pickle) {
        context.runTestCase((runner) -> {
            try (io.cucumber.junit.platform.engine.TestCaseResultObserver observer = observe(runner.getBus())) {
                log.debug(() -> "Executing test case " + pickle.getName());
                runner.runPickle(pickle);
                log.debug(() -> "Finished test case " + pickle.getName());
                observer.assertTestCasePassed();
            }
        });
    }
}
