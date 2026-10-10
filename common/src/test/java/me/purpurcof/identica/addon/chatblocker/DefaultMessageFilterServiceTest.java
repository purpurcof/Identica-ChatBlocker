package me.purpurcof.identica.addon.chatblocker;

import me.purpurcof.identica.addon.chatblocker.service.DefaultMessageFilterService;
import me.whereareiam.identica.event.scenario.authentication.AuthenticationRequiredEvent;
import me.whereareiam.identica.event.scenario.authentication.AuthenticationResolvedEvent;
import me.whereareiam.identica.event.scenario.migration.MigrationRequiredEvent;
import me.whereareiam.identica.event.scenario.migration.MigrationResolvedEvent;
import me.whereareiam.identica.event.scenario.registration.RegistrationRequiredEvent;
import me.whereareiam.identica.event.scenario.registration.RegistrationResolvedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Default Message Filter Service")
class DefaultMessageFilterServiceTest {

    private final DefaultMessageFilterService service = new DefaultMessageFilterService();

    private static AuthenticationRequiredEvent authRequired(UUID connectionId) {
        AuthenticationRequiredEvent event = mock(AuthenticationRequiredEvent.class);
        when(event.getConnectionUniqueId()).thenReturn(connectionId);
        return event;
    }

    private static AuthenticationResolvedEvent authResolved(UUID connectionId) {
        AuthenticationResolvedEvent event = mock(AuthenticationResolvedEvent.class);
        when(event.getConnectionUniqueId()).thenReturn(connectionId);
        return event;
    }

    private static RegistrationRequiredEvent regRequired(UUID connectionId) {
        RegistrationRequiredEvent event = mock(RegistrationRequiredEvent.class);
        when(event.getConnectionUniqueId()).thenReturn(connectionId);
        return event;
    }

    private static RegistrationResolvedEvent regResolved(UUID connectionId) {
        RegistrationResolvedEvent event = mock(RegistrationResolvedEvent.class);
        when(event.getConnectionUniqueId()).thenReturn(connectionId);
        return event;
    }

    private static MigrationRequiredEvent migRequired(UUID connectionId) {
        MigrationRequiredEvent event = mock(MigrationRequiredEvent.class);
        when(event.getConnectionUniqueId()).thenReturn(connectionId);
        return event;
    }

    private static MigrationResolvedEvent migResolved(UUID connectionId) {
        MigrationResolvedEvent event = mock(MigrationResolvedEvent.class);
        when(event.getConnectionUniqueId()).thenReturn(connectionId);
        return event;
    }

    @DisplayName("Tracks blocked state via isBlocked method")
    @Test
    void tracksBlockedState() {
        UUID connectionId = UUID.randomUUID();

        assertFalse(service.isBlocked(connectionId));

        service.onAuthenticationRequired(authRequired(connectionId));
        assertTrue(service.isBlocked(connectionId));

        service.onAuthenticationResolved(authResolved(connectionId));
        assertFalse(service.isBlocked(connectionId));
    }

    @DisplayName("Adds to local set on RegistrationRequired")
    @Test
    void addsLocalOnRegistrationRequired() {
        UUID connectionId = UUID.randomUUID();

        service.onRegistrationRequired(regRequired(connectionId));

        assertTrue(service.isBlocked(connectionId));
    }

    @DisplayName("Removes from local set on RegistrationResolved")
    @Test
    void removesLocalOnRegistrationResolved() {
        UUID connectionId = UUID.randomUUID();
        service.onRegistrationRequired(regRequired(connectionId));

        service.onRegistrationResolved(regResolved(connectionId));

        assertFalse(service.isBlocked(connectionId));
    }

    @DisplayName("Adds to local set on MigrationRequired")
    @Test
    void addsLocalOnMigrationRequired() {
        UUID connectionId = UUID.randomUUID();

        service.onMigrationRequired(migRequired(connectionId));

        assertTrue(service.isBlocked(connectionId));
    }

    @DisplayName("Removes from local set on MigrationResolved")
    @Test
    void removesLocalOnMigrationResolved() {
        UUID connectionId = UUID.randomUUID();
        service.onMigrationRequired(migRequired(connectionId));

        service.onMigrationResolved(migResolved(connectionId));

        assertFalse(service.isBlocked(connectionId));
    }
}
