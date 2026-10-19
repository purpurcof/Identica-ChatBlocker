package me.purpurcof.identica.addon.chatblocker.bungeecord;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import me.purpurcof.identica.addon.chatblocker.collector.DefaultIdenticaMessageScanner;
import me.purpurcof.identica.addon.chatblocker.listener.PacketEventsListener;
import me.purpurcof.identica.addon.chatblocker.service.DefaultMessageFilterService;
import me.whereareiam.identica.IdenticaAPI;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

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

        IdenticaAPI.whenReady(this::initServices);
    }

    @Override
    public void onDisable() {
        getLogger().info("Identica-ChatBlocker shutting down");
    }

    private void initServices(IdenticaAPI identica) {
        filterService = new DefaultMessageFilterService(identica.getPendingScenarioService());

        messageScanner = new DefaultIdenticaMessageScanner(identica);
        messageScanner.scan();
        identica.getEventManager().register(messageScanner);

        getLogger().info("Identica-ChatBlocker initialized");
    }
}
