package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import net.cucumbergametest.annotation.TestHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

public class ServerSteps {
    @Given("a server starts")
    public void aServerStarts() {
        System.out.println("Server Given step printline");

    }

    @Then("server has started")
    public void serverHasStarted() {
        System.out.println("Server Then step printline");
    }
}
