package me.purpurcof.identica.addon.chatblocker.bungeecord;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import me.purpurcof.identica.addon.chatblocker.collector.DefaultIdenticaMessageScanner;
import me.purpurcof.identica.addon.chatblocker.listener.PacketEventsListener;
import me.purpurcof.identica.addon.chatblocker.service.DefaultMessageFilterService;
import me.whereareiam.identica.IdenticaAPI;
import me.whereareiam.identica.model.replication.ReplicationType;
import me.whereareiam.identica.replication.ReplicationSystem;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BungeeCordChatBlockerPlugin extends Plugin {

    @Getter
    private DefaultMessageFilterService filterService;
    @Getter
    private DefaultIdenticaMessageScanner messageScanner;

    @Override
    public void onEnable() {
        PacketEvents.getAPI().getEventManager().registerListener(
                new PacketEventsListener(this::getFilterService, this::getMessageScanner),
                PacketListenerPriority.NORMAL
        );

        if (IdenticaAPI.isInitialized()) {
            initServices();
        } else {
            ProxyServer.getInstance().getScheduler().schedule(this, this::initServices, 2, TimeUnit.SECONDS);
        }

        getLogger().info("Identica-ChatBlocker initialized");
    }

    @Override
    public void onDisable() {
        getLogger().info("Identica-ChatBlocker shutting down");
    }

    private void initServices() {
        if (!IdenticaAPI.isInitialized()) {
            getLogger().warning("IdenticaAPI not initialized, chat blocker will not work");
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

        getLogger().info("Identica-ChatBlocker services ready");
    }

}