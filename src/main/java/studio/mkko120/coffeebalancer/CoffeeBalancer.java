package studio.mkko120.coffeebalancer;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Plugin(
        id = "CoffeeBalancer",
        name = "CoffeeBalancer",
        version = "1.0-SNAPSHOT",
        url = "mkko120.pl",
        description = "Balances players between velocity lobbies",
        authors = {"mkko120"}
)
public class CoffeeBalancer {

    private final ProxyServer server;
    private final Logger logger;
    private final Path directory;

    private final ArrayList<RegisteredServer> lobbies = new ArrayList<>();
    private final ArrayList<UUID> connectedPlayers = new ArrayList<>();


    @Inject
    public CoffeeBalancer(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.directory = dataDirectory;

        logger.info("CoffeeBalancer is enabling...");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        for (String serverName : server.getConfiguration().getAttemptConnectionOrder()) {
            server.getServer(serverName).ifPresent(lobbies::add);
        }

        logger.info("Loaded CoffeeBalancer!");
        logger.info("The following servers are treated as lobbies: " + lobbies.stream().map(server -> server.getServerInfo().getName()).collect(Collectors.joining(", ")));
    }

    @Subscribe
    public void onLeave(DisconnectEvent event) {
        connectedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onKick(KickedFromServerEvent event) {
        if (event.kickedDuringServerConnect())
            return;
        Optional<RegisteredServer> opt = server.getAllServers().stream().filter(server -> server != event.getServer()).filter(lobbies::contains).min(Comparator.comparingInt(s -> s.getPlayersConnected().size()));
        opt.ifPresent(registeredServer -> event.setResult(KickedFromServerEvent.RedirectPlayer.create(registeredServer)));
    }

    @Subscribe
    public void onJoin(ServerPreConnectEvent event) {
        if (!connectedPlayers.contains(event.getPlayer().getUniqueId())) {
            connectedPlayers.add(event.getPlayer().getUniqueId());
            Optional<RegisteredServer> opt = server.getAllServers().stream().filter(lobbies::contains).min(Comparator.comparingInt(s -> s.getPlayersConnected().size()));
            if (opt.isEmpty()) {
                logger.warn("No valid lobby servers were detected, so joining player '" + event.getPlayer().getUsername() + "' was connected to the default server.");
                return;
            }
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(opt.get()));
        }
    }
}
