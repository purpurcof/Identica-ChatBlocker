package me.purpurcof.identica.addon.chatblocker.collector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Message Bean Detection")
class MessageBeanDetectionTest {

    // shaped like Identica's documents; only the simple class name matters
    static class Messages {
    }

    static class UsernameMessages {
    }

    static class CredentialMessages {
    }

    static class Settings {
    }

    static class Commands {
    }

    static class Replication {
    }

    @DisplayName("Core and provider message documents are recognised")
    @Test
    void recognisesMessageDocuments() {
        assertTrue(DefaultIdenticaMessageScanner.isMessageBean(new Messages()));
        assertTrue(DefaultIdenticaMessageScanner.isMessageBean(new UsernameMessages()));
        assertTrue(DefaultIdenticaMessageScanner.isMessageBean(new CredentialMessages()));
    }

    @DisplayName("Documents without player-facing text must not widen the allowed chat")
    @Test
    void ignoresOtherDocuments() {
        assertFalse(DefaultIdenticaMessageScanner.isMessageBean(new Settings()));
        assertFalse(DefaultIdenticaMessageScanner.isMessageBean(new Commands()));
        assertFalse(DefaultIdenticaMessageScanner.isMessageBean(new Replication()));
        assertFalse(DefaultIdenticaMessageScanner.isMessageBean(null));
    }
}
