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

public class DefaultMessageFilterService implements MessageFilterService, EventListener {

    private static final long DEFAULT_TTL_MS = 300_000;

    private final Cache<String> blockedCache;

    public DefaultMessageFilterService(Cache<String> blockedCache) {
        this.blockedCache = blockedCache;
    }

    @Override
    public boolean isBlocked(@NotNull UUID connectionUniqueId) {
        return blockedCache.get(connectionUniqueId.toString()).join().isPresent();
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