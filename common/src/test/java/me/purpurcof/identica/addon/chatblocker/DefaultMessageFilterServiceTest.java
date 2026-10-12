package me.purpurcof.identica.addon.chatblocker;

import me.purpurcof.identica.addon.chatblocker.service.DefaultMessageFilterService;
import me.whereareiam.identica.pipeline.PendingScenarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Default Message Filter Service")
class DefaultMessageFilterServiceTest {

    private final PendingScenarioService pendingScenarios = mock(PendingScenarioService.class);
    private final DefaultMessageFilterService service = new DefaultMessageFilterService(pendingScenarios);

    @DisplayName("Blocks a connection while Identica waits for it to authenticate, register or migrate")
    @Test
    void blocksPendingConnection() {
        UUID connection = UUID.randomUUID();
        when(pendingScenarios.isPending(connection)).thenReturn(true);

        assertTrue(service.isBlocked(connection));
    }

    @DisplayName("Lets a connection through once nothing is pending for it")
    @Test
    void allowsConnectionWithoutPendingScenario() {
        assertFalse(service.isBlocked(UUID.randomUUID()));
    }

    @SuppressWarnings("DataFlowIssue")
    @DisplayName("A connection without a UUID is never blocked (and does not throw)")
    @Test
    void nullConnectionIsNotBlocked() {
        assertFalse(service.isBlocked(null));
    }
}
