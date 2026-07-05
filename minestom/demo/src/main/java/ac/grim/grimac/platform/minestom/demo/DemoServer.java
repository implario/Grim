package ac.grim.grimac.platform.minestom.demo;

import ac.grim.grimac.platform.minestom.api.GrimMinestom;
import net.kyori.adventure.util.TriState;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

import java.nio.file.Path;

/**
 * Minimal flat-world server for manually testing Grim on Minestom.
 * Run with: ./gradlew :minestom:demo:run  (sets -Dminestom.viewable-packet=false)
 * Then connect with a vanilla (or cheat) client on localhost:25565.
 */
public final class DemoServer {

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 41, 0));
        });
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event ->
                event.getPlayer().setGameMode(GameMode.SURVIVAL));

        // Everyone is an admin on the demo server so /grim commands just work.
        GrimMinestom grim = GrimMinestom.builder(Path.of("config", "grimac"))
                .permissionResolver((sender, node) -> TriState.TRUE)
                .build();
        grim.enable();

        server.start("0.0.0.0", 25565);
    }
}
