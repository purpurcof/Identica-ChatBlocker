package me.purpurcof.identica.addon.chatblocker.testing.environment;

import me.whereareiam.anvil.integration.junit.AnvilEnvironment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Starts a fresh network around a test: a Velocity proxy with Identica, Credential, PacketEvents and the packaged
 * ChatBlocker addon, an {@code auth} server for authentication and registration steps, and a {@code lobby} that
 * completed players reach.
 *
 * <pre>{@code
 * @Test
 * @ChatBlockerNetwork
 * void withholdsChat(ScenarioContext anvil) {
 * }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@AnvilEnvironment(ChatBlockerScenarios.class)
public @interface ChatBlockerNetwork {}
