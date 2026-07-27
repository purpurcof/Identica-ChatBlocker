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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class DefaultMessageFilterService implements MessageFilterService, EventListener {

    private final ReplicatedCache<UUID> blockedCache;

    private final Set<UUID> blockedConnections = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isBlocked(@NotNull UUID connectionUniqueId) {
        return blockedConnections.contains(connectionUniqueId);
    }

    @IdenticEvent(EventOrder.HIGH)
    public void onAuthenticationRequired(AuthenticationRequiredEvent event) {
        UUID connectionId = event.getConnectionUniqueId();
        blockedConnections.add(connectionId);
        blockedCache.put(connectionId.toString(), connectionId);
    }

    @IdenticEvent(EventOrder.HIGH)
    public void onAuthenticationResolved(AuthenticationResolvedEvent event) {
        UUID connectionId = event.getConnectionUniqueId();
        blockedConnections.remove(connectionId);
        blockedCache.invalidate(connectionId.toString());
    }

    @IdenticEvent(EventOrder.HIGH)
    public void onRegistrationRequired(RegistrationRequiredEvent event) {
        UUID connectionId = event.getConnectionUniqueId();
        blockedConnections.add(connectionId);
        blockedCache.put(connectionId.toString(), connectionId);
    }

    @IdenticEvent(EventOrder.HIGH)
    public void onRegistrationResolved(RegistrationResolvedEvent event) {
        UUID connectionId = event.getConnectionUniqueId();
        blockedConnections.remove(connectionId);
        blockedCache.invalidate(connectionId.toString());
    }

    @IdenticEvent(EventOrder.HIGH)
    public void onMigrationRequired(MigrationRequiredEvent event) {
        UUID connectionId = event.getConnectionUniqueId();
        blockedConnections.add(connectionId);
        blockedCache.put(connectionId.toString(), connectionId);
    }

    @IdenticEvent(EventOrder.HIGH)
    public void onMigrationResolved(MigrationResolvedEvent event) {
        UUID connectionId = event.getConnectionUniqueId();
        blockedConnections.remove(connectionId);
        blockedCache.invalidate(connectionId.toString());
    }
}