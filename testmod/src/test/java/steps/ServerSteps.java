package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import net.cucumbergametest.annotation.TestHelper;
import net.minecraft.gametest.framework.GameTestHelper;

public class ServerSteps {
    static {
        System.out.println("SERVERSTEPS LOADED BY " + ServerSteps.class.getClassLoader());
        System.out.println("SERVERSTEPS CURRENT CLASSLOADER " + Thread.currentThread().getContextClassLoader());

    }
    @TestHelper
    GameTestHelper gameTestHelper;

    @Given("a server starts")
    public void aServerStarts() {
        System.out.println("Server Given step printline");
        System.out.println(Thread.currentThread().getContextClassLoader());
        System.out.println(ServerSteps.class.getClassLoader());
    }

    @Then("server has started")
    public void serverHasStarted() {
        System.out.println("Server Then step printline");
        System.out.println(Thread.currentThread().getContextClassLoader());
        System.out.println(ServerSteps.class.getClassLoader());
    }
}
