package me.purpurcof.identica.addon.chatblocker.testing;

import me.purpurcof.identica.addon.chatblocker.testing.environment.ChatBlockerNetwork;
import me.purpurcof.identica.addon.chatblocker.testing.journey.Journey;
import me.whereareiam.anvil.api.scenario.ScenarioContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static me.purpurcof.identica.addon.chatblocker.testing.environment.IdenticaNetwork.AUTH;
import static me.purpurcof.identica.addon.chatblocker.testing.environment.IdenticaNetwork.LOBBY;
import static me.purpurcof.identica.addon.chatblocker.testing.environment.IdenticaNetwork.PROXY;
import static me.purpurcof.identica.addon.chatblocker.testing.environment.IdenticaNetwork.READY;

/**
 * The packaged addon on a real Velocity proxy next to Identica and PacketEvents. While a player authenticates, chat
 * is withheld from them but Identica's own messages arrive; once they are done, chat arrives. The backend sends a
 * player's chat back to them, which is how the tests observe chat.
 */
class ChatBlockerTest {
	private static final String PASSWORD = "Secret-123";

	@Test
	@ChatBlockerNetwork
	void startsOnceIdenticaHasLoadedItsMessages(ScenarioContext anvil) {
		var console = anvil.processes().proxy(PROXY).console();
		// The addon compiles its patterns while it starts, so that line can precede the one reporting it is up. One
		// watch per line keeps the assertions independent of their order.
		console.watchFromStart().printed(READY, Duration.ofSeconds(60));
		// Without message patterns the addon cannot tell Identica's messages from chat and lets everything through.
		console.watchFromStart().printed("Identica message scanner: compiled", Duration.ofSeconds(60));
	}

	@Test
	@ChatBlockerNetwork
	void withholdsChatUntilThePlayerHasRegistered(ScenarioContext anvil) {
		Journey.offline(anvil, "Alice").join()
				.on(AUTH)
				.sees(m -> m.identica().getEngine().getJourney().getStep().getEnrollment().getBody())
				// Sent while the player still waits for the enrolment answer, which is the state the addon has to
				// withhold chat in; the backend would otherwise echo it straight back.
				.chat("hello before registering")
				.doesNotSeeText("hello before registering")
				.enroll()
				.sees(m -> m.credential().getScenario().getRegistration().getPrompt())
				.register(PASSWORD)
				.on(LOBBY)
				.chat("hello after registering")
				.seesText("hello after registering");
	}
}
