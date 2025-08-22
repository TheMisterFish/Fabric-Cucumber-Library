import net.cucumberfabric.engine.FabricTestEngine;
import org.junit.platform.suite.api.*;

import static io.cucumber.core.options.Constants.GLUE_PROPERTY_NAME;
import static net.cucumberfabric.options.Constants.ENVTYPE_PROPERTY_NAME;

@Suite
@SuiteDisplayName("Fabric Server tests")
@IncludeEngines(FabricTestEngine.FABRIC_ENGINE_ID)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "steps")
@ConfigurationParameter(key = ENVTYPE_PROPERTY_NAME, value = "server")
@SelectPackages("features")
@IncludeTags("GameTestServer")
public class ServerRunner {
}