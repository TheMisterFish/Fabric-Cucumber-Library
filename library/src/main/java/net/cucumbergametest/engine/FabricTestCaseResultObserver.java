package net.cucumbergametest.engine;

import io.cucumber.core.eventbus.EventBus;
import io.cucumber.junit.platform.engine.TestCaseResultObserver;
import io.cucumber.junit.platform.engine.UndefinedStepException;
import io.cucumber.plugin.event.EventPublisher;
import org.opentest4j.TestAbortedException;

import java.util.function.Function;

public class FabricTestCaseResultObserver  implements AutoCloseable {

    private final io.cucumber.core.runtime.TestCaseResultObserver delegate;

    public FabricTestCaseResultObserver(io.cucumber.core.runtime.TestCaseResultObserver delegate) {
        this.delegate = delegate;
    }

    static TestCaseResultObserver observe(EventBus bus) {
        return new TestCaseResultObserver(bus);
    }

    void assertTestCasePassed() {
        delegate.assertTestCasePassed(
                TestAbortedException::new,
                Function.identity(),
                UndefinedStepException::new,
                Function.identity());
    }

    @Override
    public void close() {
        delegate.close();
    }

}
