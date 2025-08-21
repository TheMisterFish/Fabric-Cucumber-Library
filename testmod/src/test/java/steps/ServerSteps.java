package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public class ServerSteps {
    @Given("a server starts")
    public void aServerStarts() {
        System.out.println(ServerSteps.class.getClassLoader());
        System.out.println("Server Given step printline");

    }

    @Then("server has started")
    public void serverHasStarted() {
        System.out.println("Server Then step printline");
        MinecraftServer minecraftServer = (MinecraftServer) FabricLoader.getInstance().getGameInstance();
        System.out.println(minecraftServer.isReady());
    }
}
