package me.purpurcof.identica.addon.chatblocker.testing.environment;

import me.whereareiam.anvil.api.model.scenario.AnvilScenario;
import me.whereareiam.anvil.integration.junit.AnvilScenarioFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Builds the network a {@link ChatBlockerNetwork} declaration describes.
 */
public final class ChatBlockerScenarios implements AnvilScenarioFactory<ChatBlockerNetwork> {
	@Override
	public @NotNull AnvilScenario create(@NotNull ChatBlockerNetwork declaration) {
		return IdenticaNetwork.velocity("chatblocker");
	}
}
