package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import net.cucumbergametest.annotation.TestHelper;
import net.minecraft.gametest.framework.GameTestHelper;

public class ClientSteps {

    @TestHelper
    GameTestHelper gameTestHelper;

    @Given("a client starts")
    public void aClientStarts() {
        System.out.println("Client Given step printline");
    }

    @Then("client has started")
    public void clientHasStarted() {
        System.out.println("Client Then step printline");
    }
}
