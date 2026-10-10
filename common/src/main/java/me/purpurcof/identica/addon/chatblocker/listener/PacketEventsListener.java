package me.purpurcof.identica.addon.chatblocker.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisguisedChat;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import lombok.RequiredArgsConstructor;
import me.purpurcof.identica.addon.chatblocker.collector.DefaultIdenticaMessageScanner;
import me.purpurcof.identica.addon.chatblocker.service.DefaultMessageFilterService;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class PacketEventsListener implements PacketListener {

    private static final Logger LOGGER = Logger.getLogger(PacketEventsListener.class.getName());

    private final Supplier<DefaultMessageFilterService> filterService;
    private final Supplier<DefaultIdenticaMessageScanner> messageScanner;

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (!isChatPacket(event)) return;
        if (event.isCancelled()) return;

        DefaultMessageFilterService filter = filterService.get();
        DefaultIdenticaMessageScanner scanner = messageScanner.get();
        if (filter == null || scanner == null) return;

        UUID playerUUID = event.getUser().getUUID();
        if (!filter.isBlocked(playerUUID)) return;

        String text = extractText(event);
        if (text == null) return;

        if (!scanner.matchesAny(text)) {
            LOGGER.log(Level.FINE, "Blocked chat packet for {0}: {1}", new Object[]{playerUUID, text});
            event.setCancelled(true);
        }
    }

    private boolean isChatPacket(PacketSendEvent event) {
        String name = event.getPacketType().getName();
        return name.equals("SYSTEM_CHAT_MESSAGE")
                || name.equals("CHAT_MESSAGE")
                || name.equals("DISGUISED_CHAT");
    }

    private String extractText(PacketSendEvent event) {
        try {
            String name = event.getPacketType().getName();
            switch (name) {
                case "SYSTEM_CHAT_MESSAGE" -> {
                    WrapperPlayServerSystemChatMessage wrapper = new WrapperPlayServerSystemChatMessage(event);
                    return PlainTextComponentSerializer.plainText().serialize(wrapper.getMessage());
                }

                case "CHAT_MESSAGE" -> {
                    WrapperPlayServerChatMessage wrapper = new WrapperPlayServerChatMessage(event);
                    return PlainTextComponentSerializer.plainText().serialize(wrapper.getMessage().getChatContent());
                }

                case "DISGUISED_CHAT" -> {
                    WrapperPlayServerDisguisedChat wrapper = new WrapperPlayServerDisguisedChat(event);
                    return PlainTextComponentSerializer.plainText().serialize(wrapper.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to extract chat text", e);
        }

        return null;
    }
}