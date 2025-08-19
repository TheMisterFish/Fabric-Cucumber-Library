package net.cucumbergametest.resolver;

import net.cucumbergametest.descriptor.FabricServerTestDescriptor;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.TestDescriptor;

import java.util.List;

public class CustomOrderingVisitor implements TestDescriptor.Visitor {

    public CustomOrderingVisitor(ConfigurationParameters configurationParameters) {
        // todo
    }

    @Override
    public void visit(TestDescriptor descriptor) {
        descriptor.orderChildren(children -> {
            // Ok. All TestDescriptors are AbstractCucumberTestDescriptor
            @SuppressWarnings("rawtypes")
            List<FabricServerTestDescriptor> cucumberDescriptors = (List) children;
            return children;
        });
    }

}