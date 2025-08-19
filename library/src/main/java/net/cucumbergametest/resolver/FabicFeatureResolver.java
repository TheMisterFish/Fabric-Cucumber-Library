package net.cucumbergametest.resolver;

import org.junit.platform.engine.support.discovery.DiscoveryIssueReporter;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;
import org.junit.platform.engine.support.discovery.SelectorResolver;

import java.util.function.Predicate;

public class FabicFeatureResolver implements SelectorResolver {
//    private final ConfigurationParameters config;

    public FabicFeatureResolver(EngineDiscoveryRequestResolver.InitializationContext<?> context) {
//        this.config = context.getDiscoveryRequest().getConfigurationParameters();
    }


    public FabicFeatureResolver(Object configuration, Predicate<String> packageFilter, DiscoveryIssueReporter issueReporter) {
    }
}