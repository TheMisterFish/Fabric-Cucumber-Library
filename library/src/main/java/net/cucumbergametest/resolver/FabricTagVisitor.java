package net.cucumbergametest.resolver;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;

public class FabricTagVisitor implements TestDescriptor.Visitor {
    private final String forbiddenTag;

    public FabricTagVisitor(EngineDiscoveryRequestResolver.InitializationContext<?> context) {
        this.forbiddenTag = context.getDiscoveryRequest()
                .getConfigurationParameters()
                .get("my.skip.tag")
                .orElse("@wip");
    }

    @Override
    public void visit(TestDescriptor descriptor) {
      
    }
}