package me.purpurcof.identica.addon.chatblocker.testing.environment;

import me.whereareiam.anvil.api.model.scenario.AnvilScenario;
import me.whereareiam.anvil.api.scenario.AnvilScenarioDefinition;

/** The addon next to Identica and PacketEvents, as an administrator installs it. */
public final class DefaultNetwork implements AnvilScenarioDefinition {
	@Override
	public AnvilScenario define() {
		return IdenticaNetwork.velocity("chatblocker-default");
	}
}
