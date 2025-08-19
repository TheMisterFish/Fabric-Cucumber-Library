package net.cucumbergametest.descriptor;

import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.gherkin.Pickle;
import io.cucumber.junit.platform.engine.*;
import net.cucumbergametest.config.FabricRunConfiguration;
import net.cucumbergametest.engine.FabricEngineExecutionContext;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource;
import org.junit.platform.engine.support.hierarchical.Node;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public abstract class FabricServerTestDescriptor extends AbstractTestDescriptor {


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

    public abstract Node.SkipResult shouldBeSkipped(FabricEngineExecutionContext context);

    static class FeatureDescriptor extends FabricServerTestDescriptor implements Node<CucumberEngineExecutionContext> {

        private final Feature feature;

        FeatureDescriptor(UniqueId uniqueId, String name, TestSource source, Feature feature) {
            super(uniqueId, name, source);
            this.feature = feature;
        }

        Feature getFeature() {
            return feature;
        }

        @Override
        public CucumberEngineExecutionContext prepare(CucumberEngineExecutionContext context) {
            context.beforeFeature(feature);
            return context;
        }

        @Override
        public Type getType() {
            return Type.CONTAINER;
        }
    }

    abstract static class FeatureElementDescriptor extends FabricServerTestDescriptor
            implements Node<CucumberEngineExecutionContext> {

        private final FabricRunConfiguration configuration;
        private final io.cucumber.plugin.event.Node element;

        FeatureElementDescriptor(
                FabricRunConfiguration configuration, UniqueId uniqueId, String name, TestSource source,
                io.cucumber.plugin.event.Node element
        ) {
            super(uniqueId, name, source);
            this.configuration = configuration;
            this.element = element;
        }

        @Override
        public ExecutionMode getExecutionMode() {
            return configuration.getExecutionModeFeature();
        }

        static final class ExamplesDescriptor extends FabricServerTestDescriptor.FeatureElementDescriptor {

            ExamplesDescriptor(
                    FabricRunConfiguration configuration, UniqueId uniqueId, String name, TestSource source,
                    io.cucumber.plugin.event.Node element
            ) {
                super(configuration, uniqueId, name, source, element);
            }

            @Override
            public Type getType() {
                return Type.CONTAINER;
            }

        }

        static final class RuleDescriptor extends FabricServerTestDescriptor.FeatureElementDescriptor {

            RuleDescriptor(
                    FabricRunConfiguration configuration, UniqueId uniqueId, String name, TestSource source,
                    io.cucumber.plugin.event.Node element
            ) {
                super(configuration, uniqueId, name, source, element);
            }

            @Override
            public Type getType() {
                return Type.CONTAINER;
            }

        }

        static final class ScenarioOutlineDescriptor extends FabricServerTestDescriptor.FeatureElementDescriptor {

            ScenarioOutlineDescriptor(
                    FabricRunConfiguration configuration, UniqueId uniqueId, String name,
                    TestSource source, io.cucumber.plugin.event.Node element
            ) {
                super(configuration, uniqueId, name, source, element);
            }

            @Override
            public Type getType() {
                return Type.CONTAINER;
            }

        }
    }

    static final class PickleDescriptor extends FabricServerTestDescriptor implements Node<CucumberEngineExecutionContext> {

        private final Pickle pickle;
        private final FabricRunConfiguration configuration;

        PickleDescriptor(
                FabricRunConfiguration configuration, UniqueId uniqueId, String name, TestSource source,
                Pickle pickle
        ) {
            super(uniqueId, name, source);
            this.configuration = configuration;
            this.pickle = pickle;
        }

        Pickle getPickle() {
            return pickle;
        }

        @Override
        public Type getType() {
            return Type.TEST;
        }

        @Override
        public SkipResult shouldBeSkipped(FabricEngineExecutionContext context) {
            return Stream.of(shouldBeSkippedByTagFilter(context), shouldBeSkippedByNameFilter(context))
                    .flatMap(Optional::stream)
                    .filter(SkipResult::isSkipped)
                    .findFirst()
                    .orElseGet(SkipResult::doNotSkip);
        }

        private Optional<SkipResult> shouldBeSkippedByTagFilter(FabricEngineExecutionContext context) {
            return context.getConfiguration().tagFilter().map(expression -> {
                if (expression.evaluate(pickle.getTags())) {
                    return SkipResult.doNotSkip();
                }
                return SkipResult
                        .skip(
                                "'" + Constants.FILTER_TAGS_PROPERTY_NAME + "=" + expression
                                        + "' did not match this scenario");
            });
        }

        private Optional<SkipResult> shouldBeSkippedByNameFilter(FabricEngineExecutionContext context) {
            return context.getConfiguration().nameFilter().map(pattern -> {
                if (pattern.matcher(pickle.getName()).matches()) {
                    return SkipResult.doNotSkip();
                }
                return SkipResult
                        .skip("'" + Constants.FILTER_NAME_PROPERTY_NAME + "=" + pattern
                                + "' did not match this scenario");
            });
        }

        @Override
        public FabricEngineExecutionContext execute(
                FabricEngineExecutionContext context, DynamicTestExecutor dynamicTestExecutor
        ) {
            context.runTestCase(pickle);
            return context;
        }

        @Override
        public Set<ExclusiveResource> getExclusiveResources() {
            return getTags().stream()
                    .map(tag -> configuration.getExclusiveResourceConfiguration(tag.getName()))
                    .flatMap(ExclusiveResourceConfiguration::getExclusiveResources)
                    .collect(toSet());
        }

        /**
         * Returns the set of {@linkplain TestTag tags} for a pickle.
         * <p>
         * Note that Cucumber will remove the {code @} symbol from all Gherkin
         * tags. So a scenario tagged with {@code @Smoke} becomes a test tagged
         * with {@code Smoke}.
         *
         * @return the set of tags
         */
        @Override
        public Set<TestTag> getTags() {
            return pickle.getTags().stream()
                    .map(tag -> tag.substring(1))
                    .filter(TestTag::isValid)
                    .map(TestTag::create)
                    // Retain input order
                    .collect(collectingAndThen(toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
        }

        @Override
        public ExecutionMode getExecutionMode() {
            return configuration.getExecutionModeFeature();
        }
    }


}
