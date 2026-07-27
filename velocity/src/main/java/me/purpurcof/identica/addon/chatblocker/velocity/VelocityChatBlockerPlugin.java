package me.purpurcof.identica.addon.chatblocker.velocity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import me.purpurcof.identica.addon.chatblocker.listener.PacketEventsListener;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import lombok.Getter;
import com.velocitypowered.api.proxy.ProxyServer;
import me.purpurcof.identica.addon.chatblocker.collector.DefaultIdenticaMessageScanner;
import me.purpurcof.identica.addon.chatblocker.service.DefaultMessageFilterService;
import me.whereareiam.identica.IdenticaAPI;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "identica-chatblocker",
        name = "Identica-ChatBlocker",
        version = "1.0.0",
        description = "Blocks chat messages during authentication",
        authors = {"purpurcof"},
        dependencies = {
                @Dependency(id = "identica"),
                @Dependency(id = "packetevents")
        }
)
public class VelocityChatBlockerPlugin {

    private final Logger logger;
    private final ProxyServer server;
    @Getter
    private DefaultMessageFilterService filterService;
    @Getter
    private DefaultIdenticaMessageScanner messageScanner;

    @Inject
    public VelocityChatBlockerPlugin(
            Logger logger,
            ProxyServer server
    ) {
        this.logger = logger;
        this.server = server;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        PacketEvents.getAPI().getEventManager().registerListener(
                new PacketEventsListener(this::getFilterService, this::getMessageScanner),
                PacketListenerPriority.NORMAL
        );

        if (IdenticaAPI.isInitialized()) {
            initServices();
        } else {
            scheduleInit(0);
        }

        logger.info("Identica-ChatBlocker initialized");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Identica-ChatBlocker shutting down");
    }

    private void scheduleInit(int attempt) {
        if (attempt > 5) {
            logger.warn("IdenticaAPI not available after " + attempt + " attempts, giving up");
            return;
        }
        long delay = (long) Math.pow(2, attempt) * 500;
        server.getScheduler().buildTask(this, () -> initServices(attempt)).delay(delay, TimeUnit.MILLISECONDS).schedule();
    }

    private void initServices() {
        initServices(0);
    }

    private void initServices(int attempt) {
        if (!IdenticaAPI.isInitialized()) {
            String delayMsg = attempt <= 5
                    ? "retrying in " + ((long) Math.pow(2, attempt + 1) * 500) + "ms"
                    : "giving up after " + attempt + " attempts";
            logger.warn("IdenticaAPI not initialized, " + delayMsg);
            if (attempt <= 5) {
                scheduleInit(attempt + 1);
            }
            return;
        }

        ReplicationSystem replicationSystem = IdenticaAPI.getReplicationSystem();
        ReplicatedCache<UUID> blockedCache = replicationSystem
                .cache("chatblocker:blocked")
                .defaultTtl(300_000)
                .replicated(ReplicationType.identity(UUID.class));
        filterService = new DefaultMessageFilterService(blockedCache);
        IdenticaAPI.getEventManager().register(filterService);

        messageScanner = new DefaultIdenticaMessageScanner();
        messageScanner.scan();

        IdenticaAPI.getService(Key.get(new TypeLiteral<Registry<Reloadable>>() {})).register(messageScanner);

        logger.info("Identica-ChatBlocker services ready");
    }

}