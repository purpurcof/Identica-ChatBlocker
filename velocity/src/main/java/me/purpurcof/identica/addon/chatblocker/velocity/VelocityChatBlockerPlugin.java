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
import lombok.Getter;
import me.purpurcof.identica.addon.chatblocker.collector.DefaultIdenticaMessageScanner;
import me.purpurcof.identica.addon.chatblocker.service.DefaultMessageFilterService;
import me.whereareiam.identica.IdenticaAPI;
import org.slf4j.Logger;

@Plugin(
        id = "identica-chatblocker",
        name = "Identica-ChatBlocker",
        version = BuildConstants.VERSION,
        description = "Blocks chat messages during authentication",
        authors = {"purpurcof"},
        dependencies = {
                @Dependency(id = "identica"),
                @Dependency(id = "packetevents")
        }
)
public class VelocityChatBlockerPlugin {

    private final Logger logger;
    @Getter
    private volatile DefaultMessageFilterService filterService;
    @Getter
    private volatile DefaultIdenticaMessageScanner messageScanner;

    @Inject
    public VelocityChatBlockerPlugin(Logger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        PacketEvents.getAPI().getEventManager().registerListener(
                new PacketEventsListener(this::getFilterService, this::getMessageScanner),
                PacketListenerPriority.NORMAL
        );

        IdenticaAPI.whenReady(this::initServices);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Identica-ChatBlocker shutting down");
    }

    private void initServices(IdenticaAPI identica) {
        filterService = new DefaultMessageFilterService(identica.getPendingScenarioService());

        messageScanner = new DefaultIdenticaMessageScanner(identica);
        messageScanner.scan();
        identica.getEventManager().register(messageScanner);

        logger.info("Identica-ChatBlocker initialized");
    }
}
