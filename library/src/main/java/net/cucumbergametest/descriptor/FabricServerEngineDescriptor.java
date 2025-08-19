package net.cucumbergametest.descriptor;
import net.cucumbergametest.engine.FabricEngineExecutionContext;
import net.cucumbergametest.engine.FabricServerTestEngine;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class FabricServerEngineDescriptor extends EngineDescriptor implements Node<FabricEngineExecutionContext> {
    static final String ENGINE_ID = FabricServerTestEngine.ID;
    private final TestSource source;

    public FabricServerEngineDescriptor(UniqueId uniqueId, TestSource source) {
        super(uniqueId, "FabricServer");
        this.source = source;
    }

    @Override
    public Optional<TestSource> getSource() {
        return Optional.ofNullable(this.source);
    }

    @Override
    public FabricEngineExecutionContext prepare(FabricEngineExecutionContext context) {
        return ifChildren(context, FabricEngineExecutionContext::startTestRun);
    }

//    @Override
//    public FabricEngineExecutionContext before(FabricEngineExecutionContext context) {
//        return ifChildren(context, FabricEngineExecutionContext::runBeforeAllHooks);
//    }
//
//    @Override
//    public void after(FabricEngineExecutionContext context) {
//        ifChildren(context, FabricEngineExecutionContext::runAfterAllHooks);
//    }



    @Override
    public void cleanUp(FabricEngineExecutionContext context) {
        ifChildren(context, FabricEngineExecutionContext::finishTestRun);
    }

    /*
     * Problem: The JUnit Platform will always execute all engines that
     * participated in discovery. In combination with the JUnit Platform Suite
     * Engine this may result in CucumberEngine being executed multiple times.
     * To ensure Cucumber only performs works if/when there are tests to run we
     * don't do anything unless there are tests. I.e. only when this test
     * descriptor has children.
     */
    private FabricEngineExecutionContext ifChildren(
            FabricEngineExecutionContext context, Consumer<FabricEngineExecutionContext> action
    ) {
        if (!getChildren().isEmpty()) {
            action.accept(context);
        }
        return context;
    }

    public Object getConfiguration() {
        // no0op
        return new Object();
    }
}
