import net.cucumberfabric.engine.FabricServerTestEngine;
import org.junit.platform.suite.api.*;

import static io.cucumber.core.options.Constants.GLUE_PROPERTY_NAME;

@Suite
@SuiteDisplayName("Fabric Server tests")
@IncludeEngines(FabricServerTestEngine.ID)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "steps")
@SelectPackages("features")
@IncludeTags("GameTestServer")
public class ServerRunner {
}