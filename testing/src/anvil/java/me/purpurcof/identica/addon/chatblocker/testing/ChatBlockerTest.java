package me.purpurcof.identica.addon.chatblocker.testing;

import me.purpurcof.identica.addon.chatblocker.testing.environment.DefaultNetwork;
import me.purpurcof.identica.addon.chatblocker.testing.environment.IdenticaNetwork;
import me.purpurcof.identica.addon.chatblocker.testing.journey.Player;
import me.whereareiam.anvil.api.scenario.ScenarioContext;
import me.whereareiam.anvil.integration.junit.AnvilTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * The packaged addon on a real Velocity proxy next to Identica and PacketEvents. While a player authenticates, chat
 * the server sends them is withheld but Identica's own messages arrive; once they have authenticated, chat arrives.
 * The backend sends a player's chat back to them, which is how the tests observe chat.
 */
final class ChatBlockerTest {
	private static final String PASSWORD = "Secret-123";

	@Test
	@AnvilTest(DefaultNetwork.class)
	void startsOnceIdenticaIsReady(ScenarioContext anvil) {
		anvil.processes().proxy(IdenticaNetwork.PROXY).console()
				.await("Identica-ChatBlocker initialized", 0, Duration.ofSeconds(60));
	}

	@Test
	@AnvilTest(DefaultNetwork.class)
	void withholdsChatWhileThePlayerAuthenticates(ScenarioContext anvil) {
		// joining already shows that Identica's own prompt reaches the player
		Player.join(anvil, "Alice")
				.chat("withheld-before-registering")
				.doesNotSeeText("withheld-before-registering");
	}

	@Test
	@AnvilTest(DefaultNetwork.class)
	void stillDeliversIdenticasOwnMessages(ScenarioContext anvil) {
		// Registration is a conversation of Identica's messages; it only completes if each of them reaches the player.
		Player.join(anvil, "Alice")
				.register(PASSWORD);
	}

	@Test
	@AnvilTest(DefaultNetwork.class)
	void deliversChatOnceThePlayerHasRegistered(ScenarioContext anvil) {
		Player.join(anvil, "Alice")
				.register(PASSWORD)
				.chat("delivered-after-registering")
				.seesText("delivered-after-registering");
	}
}
