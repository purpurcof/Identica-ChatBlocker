package me.purpurcof.identica.addon.chatblocker;

import me.purpurcof.identica.addon.chatblocker.service.DefaultMessageFilterService;
import me.whereareiam.identica.event.scenario.authentication.AuthenticationRequiredEvent;
import me.whereareiam.identica.event.scenario.authentication.AuthenticationResolvedEvent;
import me.whereareiam.identica.event.scenario.migration.MigrationRequiredEvent;
import me.whereareiam.identica.event.scenario.migration.MigrationResolvedEvent;
import me.whereareiam.identica.event.scenario.registration.RegistrationRequiredEvent;
import me.whereareiam.identica.event.scenario.registration.RegistrationResolvedEvent;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Default Message Filter Service")
class DefaultMessageFilterServiceTest {

    @SuppressWarnings("unchecked")
    private final ReplicatedCache<UUID> blockedCache = (ReplicatedCache<UUID>) mock(ReplicatedCache.class);

    private final DefaultMessageFilterService service = new DefaultMessageFilterService(blockedCache);

    @BeforeEach
    void setUp() {
        when(blockedCache.get(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(blockedCache.put(any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));
        when(blockedCache.invalidate(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

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

        when(blockedCache.get(eq(connectionId.toString())))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(connectionId)));
        assertTrue(service.isBlocked(connectionId));

        when(blockedCache.get(eq(connectionId.toString())))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        assertFalse(service.isBlocked(connectionId));
    }

    @DisplayName("Puts connectionId into cache on AuthenticationRequired")
    @Test
    void putsOnAuthenticationRequired() {
        UUID connectionId = UUID.randomUUID();

        service.onAuthenticationRequired(authRequired(connectionId));

        verify(blockedCache).put(eq(connectionId.toString()), eq(connectionId));
    }

    @DisplayName("Invalidates connectionId on AuthenticationResolved")
    @Test
    void invalidatesOnAuthenticationResolved() {
        UUID connectionId = UUID.randomUUID();

        service.onAuthenticationResolved(authResolved(connectionId));

        verify(blockedCache).invalidate(eq(connectionId.toString()));
    }

    @DisplayName("Puts connectionId on RegistrationRequired")
    @Test
    void putsOnRegistrationRequired() {
        UUID connectionId = UUID.randomUUID();

        service.onRegistrationRequired(regRequired(connectionId));

        verify(blockedCache).put(eq(connectionId.toString()), eq(connectionId));
    }

    @DisplayName("Invalidates connectionId on RegistrationResolved")
    @Test
    void invalidatesOnRegistrationResolved() {
        UUID connectionId = UUID.randomUUID();

        service.onRegistrationResolved(regResolved(connectionId));

        verify(blockedCache).invalidate(eq(connectionId.toString()));
    }

    @DisplayName("Puts connectionId on MigrationRequired")
    @Test
    void putsOnMigrationRequired() {
        UUID connectionId = UUID.randomUUID();

        service.onMigrationRequired(migRequired(connectionId));

        verify(blockedCache).put(eq(connectionId.toString()), eq(connectionId));
    }

    @DisplayName("Invalidates connectionId on MigrationResolved")
    @Test
    void invalidatesOnMigrationResolved() {
        UUID connectionId = UUID.randomUUID();

        service.onMigrationResolved(migResolved(connectionId));

        verify(blockedCache).invalidate(eq(connectionId.toString()));
    }

    @DisplayName("Returns false when cache get times out")
    @Test
    void isBlockedReturnsFalseOnCacheTimeout() {
        UUID connectionId = UUID.randomUUID();

        when(blockedCache.get(any())).thenReturn(new CompletableFuture<>());

        assertFalse(service.isBlocked(connectionId));
    }

    @DisplayName("Returns false when cache get throws exception")
    @Test
    void isBlockedReturnsFalseOnCacheException() {
        UUID connectionId = UUID.randomUUID();

        CompletableFuture<Optional<UUID>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("cache unavailable"));
        when(blockedCache.get(any())).thenReturn(failed);

        assertFalse(service.isBlocked(connectionId));
    }
}
