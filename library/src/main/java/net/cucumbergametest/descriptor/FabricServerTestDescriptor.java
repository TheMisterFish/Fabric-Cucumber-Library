package net.cucumbergametest.descriptor;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

import java.util.Optional;

public class FabricServerTestDescriptor extends AbstractTestDescriptor {


    public FabricServerTestDescriptor(UniqueId uniqueId, String displayName) {
        super(uniqueId, displayName);
    }

    public FabricServerTestDescriptor(UniqueId uniqueId, String displayName, TestSource source) {
        super(uniqueId, displayName, source);
    }

    @Override
    public Optional<? extends TestDescriptor> findByUniqueId(UniqueId uniqueId) {
        return super.findByUniqueId(uniqueId);
    }

    @Override
    public Type getType() {
        return super.getChildren().isEmpty() ? Type.TEST : Type.CONTAINER;
    }
}
