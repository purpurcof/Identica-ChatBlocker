package me.purpurcof.identica.addon.chatblocker.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
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

        User user = event.getUser();
        UUID playerUUID = user.getUUID();
        if (!filter.isBlocked(playerUUID)) return;

        if (isPlayerChat(event, user)) {
            // chat from other players or the server is never an Identica message
            cancel(event, playerUUID);
            return;
        }

        // without any message templates Identica's own messages cannot be told apart, so filtering would hide them
        if (!scanner.isReady()) return;

        String text = extractText(event);
        if (text == null) return;

        if (!scanner.matchesAny(text)) cancel(event, playerUUID);
    }

    private static boolean isPlayerChat(PacketSendEvent event, User user) {
        String name = event.getPacketType().getName();
        if (name.equals("DISGUISED_CHAT")) return true;
        if (!name.equals("CHAT_MESSAGE")) return false;

        ClientVersion version = user.getClientVersion();
        if (version == null) return false;
        return version.isNewerThanOrEquals(ClientVersion.V_1_19);
    }

    private static void cancel(PacketSendEvent event, UUID playerUUID) {
        // the text is deliberately not logged: it can be another player's private message
        LOGGER.log(Level.FINE, "Blocked {0} packet for {1}", new Object[]{event.getPacketType().getName(), playerUUID});
        event.setCancelled(true);
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