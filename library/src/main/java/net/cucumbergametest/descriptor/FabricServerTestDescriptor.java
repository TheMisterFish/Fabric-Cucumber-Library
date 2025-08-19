package net.cucumbergametest.descriptor;

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

public class FabricServerTestDescriptor extends AbstractTestDescriptor {


    public FabricServerTestDescriptor(UniqueId uniqueId, String displayName) {
        super(uniqueId, displayName);
    }

    public FabricServerTestDescriptor(UniqueId uniqueId, String displayName, TestSource source) {
        super(uniqueId, displayName, source);
    }


    @Override
    public Type getType() {
        return Type.TEST;
    }
}
