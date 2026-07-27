package me.purpurcof.identica.addon.chatblocker.bungeecord;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import me.purpurcof.identica.addon.chatblocker.collector.DefaultIdenticaMessageScanner;
import me.purpurcof.identica.addon.chatblocker.listener.PacketEventsListener;
import me.purpurcof.identica.addon.chatblocker.service.DefaultMessageFilterService;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import me.whereareiam.identica.IdenticaAPI;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.Reloadable;
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
            scheduleInit(0);
        }

        getLogger().info("Identica-ChatBlocker initialized");
    }

    @Override
    public void onDisable() {
        getLogger().info("Identica-ChatBlocker shutting down");
    }

    private void scheduleInit(int attempt) {
        if (attempt > 5) {
            getLogger().warning("IdenticaAPI not available after " + attempt + " attempts, giving up");
            return;
        }
        long delay = (long) Math.pow(2, attempt) * 500;
        ProxyServer.getInstance().getScheduler().schedule(this, () -> initServices(attempt), delay, TimeUnit.MILLISECONDS);
    }

    private void initServices() {
        initServices(0);
    }

    private void initServices(int attempt) {
        if (!IdenticaAPI.isInitialized()) {
            String delayMsg = attempt <= 5
                    ? "retrying in " + ((long) Math.pow(2, attempt + 1) * 500) + "ms"
                    : "giving up after " + attempt + " attempts";
            getLogger().warning("IdenticaAPI not initialized, " + delayMsg);
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

        getLogger().info("Identica-ChatBlocker services ready");
    }

}