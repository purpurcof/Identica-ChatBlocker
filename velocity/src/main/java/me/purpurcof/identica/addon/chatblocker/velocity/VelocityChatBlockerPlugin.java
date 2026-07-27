package me.purpurcof.identica.addon.chatblocker.velocity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import me.purpurcof.identica.addon.chatblocker.listener.PacketEventsListener;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import me.purpurcof.identica.addon.chatblocker.collector.DefaultIdenticaMessageScanner;
import me.purpurcof.identica.addon.chatblocker.service.DefaultMessageFilterService;
import me.whereareiam.identica.IdenticaAPI;
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
    private DefaultMessageFilterService filterService;
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

        server.getScheduler().buildTask(this, this::initServices)
                .delay(2, TimeUnit.SECONDS)
                .schedule();

        logger.info("Identica-ChatBlocker initialized");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Identica-ChatBlocker shutting down");
    }

    private void initServices() {
        if (!IdenticaAPI.isInitialized()) {
            logger.warn("IdenticaAPI not initialized, chat blocker will not work");
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

        logger.info("Identica-ChatBlocker services ready");
    }

    public DefaultMessageFilterService getFilterService() {
        return filterService;
    }

    public DefaultIdenticaMessageScanner getMessageScanner() {
        return messageScanner;
    }
}