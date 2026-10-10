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
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class BungeeCordChatBlockerPlugin extends Plugin {

    @Getter
    private volatile DefaultMessageFilterService filterService;
    @Getter
    private volatile DefaultIdenticaMessageScanner messageScanner;

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
    }

    @Override
    public void onDisable() {
        getLogger().info("Identica-ChatBlocker shutting down");
    }

    private void scheduleInit(int attempt) {
        if (attempt >= 5) {
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
            if (attempt < 5) {
                getLogger().warning("IdenticaAPI not initialized, retrying in " + ((long) Math.pow(2, attempt + 1) * 500) + "ms");
                scheduleInit(attempt + 1);
            } else {
                getLogger().warning("IdenticaAPI not initialized, giving up after " + attempt + " attempts");
            }

            return;
        }

        filterService = new DefaultMessageFilterService();
        IdenticaAPI.getEventManager().register(filterService);

        messageScanner = new DefaultIdenticaMessageScanner();
        messageScanner.scan();

        IdenticaAPI.getService(Key.get(new TypeLiteral<Registry<Reloadable>>() {})).register(messageScanner);

        getLogger().info("Identica-ChatBlocker initialized");
    }
}
