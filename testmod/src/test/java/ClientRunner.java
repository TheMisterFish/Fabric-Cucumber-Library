import org.junit.platform.suite.api.*;

import static io.cucumber.core.options.Constants.GLUE_PROPERTY_NAME;
import static net.cucumberfabric.options.Constants.ENVTYPE_PROPERTY_NAME;

@Suite
@SuiteDisplayName("Fabric Client tests")
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "steps")
@ConfigurationParameter(key = ENVTYPE_PROPERTY_NAME, value = "client")
@SelectPackages("features")
@IncludeTags("GameTestClient")
public class ClientRunner {
}