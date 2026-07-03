package me.purpurcof.identica.addon.chatblocker.service;

import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.scenario.authentication.AuthenticationRequiredEvent;
import me.whereareiam.identica.event.scenario.authentication.AuthenticationResolvedEvent;
import me.whereareiam.identica.event.scenario.registration.RegistrationRequiredEvent;
import me.whereareiam.identica.event.scenario.registration.RegistrationResolvedEvent;
import me.whereareiam.identica.event.scenario.migration.MigrationRequiredEvent;
import me.whereareiam.identica.event.scenario.migration.MigrationResolvedEvent;
import me.whereareiam.identica.replication.cache.base.Cache;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultMessageFilterService implements MessageFilterService, EventListener {

    private static final long DEFAULT_TTL_MS = 300_000;
    private static final long CACHE_GET_TIMEOUT_MS = 250;
    private static final Logger LOGGER = Logger.getLogger(DefaultMessageFilterService.class.getName());

    private final Cache<String> blockedCache;

    public DefaultMessageFilterService(Cache<String> blockedCache) {
        this.blockedCache = blockedCache;
    }

    @Override
    public boolean isBlocked(@NotNull UUID connectionUniqueId) {
        try {
            return blockedCache.get(connectionUniqueId.toString())
                    .get(CACHE_GET_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .isPresent();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to read blocked state for " + connectionUniqueId, e);
            return false;
        }
    }

    @IdenticEvent
    public void onAuthenticationRequired(AuthenticationRequiredEvent event) {
        blockedCache.put(event.getConnectionUniqueId().toString(), "1", DEFAULT_TTL_MS);
    }

    @IdenticEvent
    public void onAuthenticationResolved(AuthenticationResolvedEvent event) {
        blockedCache.invalidate(event.getConnectionUniqueId().toString());
    }

    @IdenticEvent
    public void onRegistrationRequired(RegistrationRequiredEvent event) {
        blockedCache.put(event.getConnectionUniqueId().toString(), "1", DEFAULT_TTL_MS);
    }

    @IdenticEvent
    public void onRegistrationResolved(RegistrationResolvedEvent event) {
        blockedCache.invalidate(event.getConnectionUniqueId().toString());
    }

    @IdenticEvent
    public void onMigrationRequired(MigrationRequiredEvent event) {
        blockedCache.put(event.getConnectionUniqueId().toString(), "1", DEFAULT_TTL_MS);
    }

    @IdenticEvent
    public void onMigrationResolved(MigrationResolvedEvent event) {
        blockedCache.invalidate(event.getConnectionUniqueId().toString());
    }
}