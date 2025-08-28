package runners;

import io.cucumber.java.AfterAll;
import io.cucumber.java.AfterStep;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.BeforeStep;
import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;

//@Suite
//@SuiteDisplayName("Fabric Server tests")
//@IncludeEngines(FabricTestEngine.FABRIC_ENGINE_ID)
//@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "steps,runners")
//@ConfigurationParameter(key = ENVTYPE_PROPERTY_NAME, value = "server")
//@SelectPackages("features")
//@IncludeTags("GameTestServer")
public class ServerRunner {

    @BeforeAll
    public static void beforeAll() {
        System.out.println("beforeAll");
    }

    @BeforeSuite
    public static void beforeSuite() {
        System.out.println("beforeSuite");
    }

    @BeforeStep
    public void beforeStep() {
        System.out.println("beforeStep?");
    }

    @AfterSuite
    public static void afterSuite() {
        System.out.println("afterSuite");
    }

    @AfterAll
    public static void afterAll() {
        System.out.println("afterAll");
    }

    @AfterStep
    public void afterStep() {
        System.out.println("afterStep");
    }
}