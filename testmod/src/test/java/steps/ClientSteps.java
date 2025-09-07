package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import net.cucumberfabric.helper.client.GuiHelper;

import java.util.concurrent.TimeUnit;

public class ClientSteps {

    @Given("a client starts")
    public void aClientStarts() {
        System.out.println("Client Given step printline");
        GuiHelper.listButtons().forEach(button -> {
            System.out.println(button);
        });
        GuiHelper.pressButton("singleplayer");
        System.out.println("singleplayer pressed");
        GuiHelper.listButtons().forEach(button -> {
            System.out.println(button);
        });
    }

    @Then("client has started")
    public void clientHasStarted() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);
        System.out.println("Client Then step printline");
    }
}
