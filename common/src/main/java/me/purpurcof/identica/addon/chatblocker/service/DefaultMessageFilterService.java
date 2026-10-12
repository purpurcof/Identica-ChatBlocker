package me.purpurcof.identica.addon.chatblocker.service;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.pipeline.PendingScenarioService;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Blocks a connection while Identica still waits for it to authenticate, register or migrate. Identica follows the
 * scenarios itself, including a player who leaves before finishing one.
 */
@RequiredArgsConstructor
public class DefaultMessageFilterService implements MessageFilterService {

    private final PendingScenarioService pendingScenarios;

    @Override
    public boolean isBlocked(@NotNull UUID connectionUniqueId) {
        // PacketEvents has no UUID for a connection that has not logged in yet; Identica follows none of those
        return connectionUniqueId != null && pendingScenarios.isPending(connectionUniqueId);
    }
}
