package net.cucumbergametest.config;

import io.cucumber.core.backend.ObjectFactory;
import io.cucumber.core.eventbus.UuidGenerator;
import io.cucumber.core.feature.FeatureWithLines;
import io.cucumber.core.feature.GluePath;
import io.cucumber.core.options.ObjectFactoryParser;
import io.cucumber.core.options.PluginOption;
import io.cucumber.core.options.SnippetTypeParser;
import io.cucumber.core.options.UuidGeneratorParser;
import io.cucumber.core.plugin.NoPublishFormatter;
import io.cucumber.core.plugin.PublishFormatter;
import io.cucumber.core.snippets.SnippetType;
import io.cucumber.query.NamingStrategy;
import io.cucumber.tagexpressions.Expression;
import io.cucumber.tagexpressions.TagExpressionParser;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.support.config.PrefixedConfigurationParameters;
import org.junit.platform.engine.support.hierarchical.Node;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.cucumber.core.resource.ClasspathSupport.CLASSPATH_SCHEME_PREFIX;
import static io.cucumber.junit.platform.engine.Constants.*;
import static java.util.Objects.requireNonNull;

public class FabricRunConfiguration implements
            io.cucumber.core.plugin.Options,
            io.cucumber.core.runner.Options,
            io.cucumber.core.backend.Options,
            io.cucumber.core.eventbus.Options {

        private final ConfigurationParameters configurationParameters;

    public FabricRunConfiguration(ConfigurationParameters configurationParameters) {
            this.configurationParameters = requireNonNull(configurationParameters);
        }

        @Override
        public List<Plugin> plugins() {
            List<Plugin> plugins = configurationParameters.get(PLUGIN_PROPERTY_NAME, s -> Arrays.stream(s.split(","))
                            .map(String::trim)
                            .map(PluginOption::parse)
                            .map(pluginOption -> (Plugin) pluginOption)
                            .collect(Collectors.toList()))
                    .orElseGet(ArrayList::new);

            getPublishPlugin()
                    .ifPresent(plugins::add);

            return plugins;
        }

        private Optional<PluginOption> getPublishPlugin() {
            if (isPublishPluginEnabled()) {
                return createPublishPlugin();
            }
            return createCucumberReportsAdvertisingPlugin();
        }

        private Optional<PluginOption> createCucumberReportsAdvertisingPlugin() {
            Optional<PluginOption> noPublishOption = Optional.of(PluginOption.forClass(NoPublishFormatter.class));
            Optional<PluginOption> quiteOption = Optional.empty();
            return configurationParameters
                    .getBoolean(PLUGIN_PUBLISH_QUIET_PROPERTY_NAME)
                    .map(quite -> quite ? quiteOption : noPublishOption)
                    // Disable the banner advertising the hosted cucumber reports
                    // by default until the uncertainty around the projects future
                    // is resolved. It would not be proper to advertise a service
                    // that may be discontinued to new users.
                    // For context see: https://mattwynne.net/new-beginning
                    .orElse(quiteOption);

        }

        private Optional<PluginOption> createPublishPlugin() {
            PluginOption publishPlugin = configurationParameters
                    .get(PLUGIN_PUBLISH_TOKEN_PROPERTY_NAME)
                    .map(token -> PluginOption.forClass(PublishFormatter.class, token))
                    .orElse(PluginOption.forClass(PublishFormatter.class));
            return Optional.of(publishPlugin);
        }

        private boolean isPublishPluginEnabled() {
            return configurationParameters.getBoolean(PLUGIN_PUBLISH_ENABLED_PROPERTY_NAME)
                    // Implicitly enabled by the token if not explicitly disabled
                    .orElse(configurationParameters.get(PLUGIN_PUBLISH_TOKEN_PROPERTY_NAME).isPresent());
        }

        @Override
        public boolean isMonochrome() {
            return configurationParameters
                    .getBoolean(ANSI_COLORS_DISABLED_PROPERTY_NAME)
                    .orElse(false);
        }

        @Override
        public boolean isWip() {
            return false;
        }

        public Optional<Expression> tagFilter() {
            return configurationParameters.get(FILTER_TAGS_PROPERTY_NAME, TagExpressionParser::parse);
        }

        public Optional<Pattern> nameFilter() {
            return configurationParameters.get(FILTER_NAME_PROPERTY_NAME, Pattern::compile);
        }

        @Override
        public List<URI> getGlue() {
            return configurationParameters
                    .get(GLUE_PROPERTY_NAME, s -> Arrays.asList(s.split(",")))
                    .orElse(Collections.singletonList(CLASSPATH_SCHEME_PREFIX))
                    .stream()
                    .map(String::trim)
                    .map(GluePath::parse)
                    .collect(Collectors.toList());
        }

        @Override
        public boolean isDryRun() {
            return configurationParameters
                    .getBoolean(EXECUTION_DRY_RUN_PROPERTY_NAME)
                    .orElse(false);
        }

        @Override
        public SnippetType getSnippetType() {
            return configurationParameters
                    .get(SNIPPET_TYPE_PROPERTY_NAME, SnippetTypeParser::parseSnippetType)
                    .orElse(SnippetType.UNDERSCORE);
        }

        @Override
        public Class<? extends ObjectFactory> getObjectFactoryClass() {
            return configurationParameters
                    .get(OBJECT_FACTORY_PROPERTY_NAME, ObjectFactoryParser::parseObjectFactory)
                    .orElse(null);
        }

        @Override
        public Class<? extends UuidGenerator> getUuidGeneratorClass() {
            return configurationParameters
                    .get(UUID_GENERATOR_PROPERTY_NAME, UuidGeneratorParser::parseUuidGenerator)
                    .orElse(null);
        }

        public boolean isParallelExecutionEnabled() {
            return configurationParameters
                    .getBoolean(PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME)
                    .orElse(false);
        }

        public Node.ExecutionMode getExecutionModeFeature() {
            return configurationParameters.get(EXECUTION_MODE_FEATURE_PROPERTY_NAME,
                            value -> Node.ExecutionMode.valueOf(value.toUpperCase(Locale.US)))
                    .orElse(Node.ExecutionMode.CONCURRENT);
        }
}
