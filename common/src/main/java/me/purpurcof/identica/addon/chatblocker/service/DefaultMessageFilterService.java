package me.purpurcof.identica.addon.chatblocker.service;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.type.event.EventOrder;
import me.whereareiam.identica.event.scenario.authentication.AuthenticationRequiredEvent;
import me.whereareiam.identica.event.scenario.authentication.AuthenticationResolvedEvent;
import me.whereareiam.identica.event.scenario.registration.RegistrationRequiredEvent;
import me.whereareiam.identica.event.scenario.registration.RegistrationResolvedEvent;
import me.whereareiam.identica.event.scenario.migration.MigrationRequiredEvent;
import me.whereareiam.identica.event.scenario.migration.MigrationResolvedEvent;
import me.whereareiam.identica.replication.cache.ReplicatedCache;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class DefaultMessageFilterService implements MessageFilterService, EventListener {

    private static final long CACHE_GET_TIMEOUT_MS = 250;
    private static final Logger LOGGER = Logger.getLogger(DefaultMessageFilterService.class.getName());

    private final ReplicatedCache<UUID> blockedCache;

    @Override
    public boolean isBlocked(@NotNull UUID connectionUniqueId) {
        try {
            Optional<UUID> result = blockedCache.get(connectionUniqueId.toString())
                    .completeOnTimeout(Optional.empty(), CACHE_GET_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> {
                        LOGGER.log(Level.FINE, "Cache read failed for " + connectionUniqueId, ex);
                        return Optional.empty();
                    })
                    .join();
            return result.isPresent();
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Unexpected error reading blocked state for " + connectionUniqueId, t);
            return false;
        }
    }

    @IdenticEvent(EventOrder.HIGH)
    public void onAuthenticationRequired(AuthenticationRequiredEvent event) {
        UUID connectionId = event.getConnectionUniqueId();
        blockedCache.put(connectionId.toString(), connectionId);
    }

    @IdenticEvent(EventOrder.HIGH)
    public void onAuthenticationResolved(AuthenticationResolvedEvent event) {
        UUID connectionId = event.getConnectionUniqueId();
        blockedCache.invalidate(connectionId.toString());
    }

    @IdenticEvent(EventOrder.HIGH)
    public void onRegistrationRequired(RegistrationRequiredEvent event) {
        UUID connectionId = event.getConnectionUniqueId();
        blockedCache.put(connectionId.toString(), connectionId);
    }

    @IdenticEvent(EventOrder.HIGH)
    public void onRegistrationResolved(RegistrationResolvedEvent event) {
        UUID connectionId = event.getConnectionUniqueId();
        blockedCache.invalidate(connectionId.toString());
    }

    @IdenticEvent(EventOrder.HIGH)
    public void onMigrationRequired(MigrationRequiredEvent event) {
        UUID connectionId = event.getConnectionUniqueId();
        blockedCache.put(connectionId.toString(), connectionId);
    }

    @IdenticEvent(EventOrder.HIGH)
    public void onMigrationResolved(MigrationResolvedEvent event) {
        UUID connectionId = event.getConnectionUniqueId();
        blockedCache.invalidate(connectionId.toString());
    }
}