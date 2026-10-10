package me.purpurcof.identica.addon.chatblocker;

import me.purpurcof.identica.addon.chatblocker.collector.DefaultIdenticaMessageScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Default Identica Message Scanner")
class DefaultIdenticaMessageScannerTest {

    private final DefaultIdenticaMessageScanner scanner = new DefaultIdenticaMessageScanner();

    @BeforeEach
    void reset() throws Exception {
        setPatterns(Collections.emptySet());
    }

    @DisplayName("Returns false when not scanned (empty patterns)")
    @Test
    void matchesAnyReturnsFalseWhenNotScanned() {
        assertFalse(scanner.matchesAny("anything"));
    }

    @DisplayName("Returns false for null input")
    @Test
    void matchesAnyReturnsFalseForNull() {
        assertFalse(scanner.matchesAny(null));
    }

    @DisplayName("Returns false for empty input")
    @Test
    void matchesAnyReturnsFalseForEmpty() {
        assertFalse(scanner.matchesAny(""));
    }

    @DisplayName("Returns true when input matches a pattern")
    @Test
    void matchesAnyReturnsTrueWhenPatternMatches() throws Exception {
        setPatterns(Set.of(Pattern.compile("Hello World", Pattern.DOTALL)));

        assertTrue(scanner.matchesAny("Hello World"));
        assertTrue(scanner.matchesAny("prefix Hello World suffix"));
    }

    @DisplayName("Returns false when input does not match any pattern")
    @Test
    void matchesAnyReturnsFalseWhenNoPatternMatch() throws Exception {
        setPatterns(Set.of(Pattern.compile("Hello World", Pattern.DOTALL)));

        assertFalse(scanner.matchesAny("Goodbye World"));
        assertFalse(scanner.matchesAny("Helloworld"));
    }

    @DisplayName("Matches with placeholder wildcard")
    @Test
    void matchesWithPlaceholderWildcard() throws Exception {
        setPatterns(Set.of(Pattern.compile(Pattern.quote("Try again in ") + ".{0,64}?" + Pattern.quote("s."), Pattern.DOTALL)));

        assertTrue(scanner.matchesAny("Try again in 30s."));
        assertTrue(scanner.matchesAny("Try again in 60s."));
        assertTrue(scanner.matchesAny("Try again in s."));
    }

    @DisplayName("Blocks non-Identica chat messages")
    @Test
    void blocksNonIdenticaMessages() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("Welcome back, ") + ".{0,64}?" + Pattern.quote("."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("You have ") + ".{0,64}?" + Pattern.quote(" tries left."), Pattern.DOTALL)
        ));

        assertFalse(scanner.matchesAny("Steve joined the game"));
        assertFalse(scanner.matchesAny("Notch: yes"));
        assertFalse(scanner.matchesAny("hello everyone"));
    }

    @DisplayName("Real Identica strings: credential prompts pass the filter")
    @Test
    void realIdenticaCredentialPromptsPass() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("Welcome back to our server."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Please log in to proceed."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("To create your password account, you have to"), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("walk through some registration steps."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("To finish your password account registration,"), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("repeat the same password you entered before."), Pattern.DOTALL)
        ));

        assertTrue(scanner.matchesAny("Welcome back to our server."));
        assertTrue(scanner.matchesAny("Please log in to proceed."));
        assertTrue(scanner.matchesAny("To create your password account, you have to"));
        assertTrue(scanner.matchesAny("walk through some registration steps."));
    }

    @DisplayName("Real Identica strings: placeholder messages pass the filter")
    @Test
    void realIdenticaPlaceholderMessagesPass() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("You have ") + ".{0,64}?" + Pattern.quote(" tries left."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Try again in ") + ".{0,64}?" + Pattern.quote("s."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Welcome back, ") + ".{0,64}?" + Pattern.quote("."), Pattern.DOTALL)
        ));

        assertTrue(scanner.matchesAny("You have 3 tries left."));
        assertTrue(scanner.matchesAny("Try again in 30s."));
        assertTrue(scanner.matchesAny("Welcome back, Steve."));
    }

    @DisplayName("Real Identica strings: enrollment provider list passes")
    @Test
    void realIdenticaEnrollmentProviderListPasses() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("Select an authentication method for account"), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("It can be changed later on."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Available providers:"), Pattern.DOTALL)
        ));

        assertTrue(scanner.matchesAny("Select an authentication method for account"));
        assertTrue(scanner.matchesAny("It can be changed later on."));
        assertTrue(scanner.matchesAny("Available providers:"));
    }

    @DisplayName("Short fragments below minimum length are rejected by compileTemplate")
    @Test
    void shortFragmentsAreRejected() throws Exception {
        var method = DefaultIdenticaMessageScanner.class.getDeclaredMethod("compileTemplate", String.class);
        method.setAccessible(true);

        assertNull(method.invoke(scanner, "short"));
        assertNull(method.invoke(scanner, "abc"));
    }

    @DisplayName("Real Identica strings: all credential prompts pass the filter")
    @Test
    void realIdenticaAllCredentialPromptsPass() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("Welcome back to our server."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Please log in to proceed."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("To create your password account, you have to"), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("walk through some registration steps."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("To finish your password account registration,"), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("repeat the same password you entered before."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Verification required for your account."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("To migrate this account to the credential provider,"), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("first verify your current password."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Your current password was verified."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Now choose the password you want to use with the credential provider."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Repeat the same password to finish your"), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("migration to the credential provider."), Pattern.DOTALL)
        ));

        assertTrue(scanner.matchesAny("Welcome back to our server."));
        assertTrue(scanner.matchesAny("Please log in to proceed."));
        assertTrue(scanner.matchesAny("To create your password account, you have to"));
        assertTrue(scanner.matchesAny("walk through some registration steps."));
        assertTrue(scanner.matchesAny("To finish your password account registration,"));
        assertTrue(scanner.matchesAny("repeat the same password you entered before."));
        assertTrue(scanner.matchesAny("Verification required for your account."));
        assertTrue(scanner.matchesAny("To migrate this account to the credential provider,"));
        assertTrue(scanner.matchesAny("first verify your current password."));
        assertTrue(scanner.matchesAny("Your current password was verified."));
        assertTrue(scanner.matchesAny("Now choose the password you want to use with the credential provider."));
        assertTrue(scanner.matchesAny("Repeat the same password to finish your"));
        assertTrue(scanner.matchesAny("migration to the credential provider."));
    }

    @DisplayName("Real Identica strings: all credential status messages pass the filter")
    @Test
    void realIdenticaAllCredentialStatusMessagesPass() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("Registration is disabled."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Your account is already registered."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Passwords do not match."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("No pending registration."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Invalid password."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("No password account found."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("No pending login."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Invalid verification code."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("A verification method is required before login."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Your selected verification method is unavailable."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Your current password is invalid."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("No pending credential migration verification."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential migration is disabled."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Migration passwords do not match."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("No pending credential migration password step."), Pattern.DOTALL)
        ));

        assertTrue(scanner.matchesAny("Registration is disabled."));
        assertTrue(scanner.matchesAny("Your account is already registered."));
        assertTrue(scanner.matchesAny("Passwords do not match."));
        assertTrue(scanner.matchesAny("No pending registration."));
        assertTrue(scanner.matchesAny("Invalid password."));
        assertTrue(scanner.matchesAny("No password account found."));
        assertTrue(scanner.matchesAny("No pending login."));
        assertTrue(scanner.matchesAny("Invalid verification code."));
        assertTrue(scanner.matchesAny("A verification method is required before login."));
        assertTrue(scanner.matchesAny("Your selected verification method is unavailable."));
        assertTrue(scanner.matchesAny("Your current password is invalid."));
        assertTrue(scanner.matchesAny("No pending credential migration verification."));
        assertTrue(scanner.matchesAny("Credential migration is disabled."));
        assertTrue(scanner.matchesAny("Migration passwords do not match."));
        assertTrue(scanner.matchesAny("No pending credential migration password step."));
    }

    @DisplayName("Real Identica strings: all credential completion messages pass the filter")
    @Test
    void realIdenticaAllCredentialCompletionMessagesPass() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("Welcome back, ") + ".{0,64}?" + Pattern.quote("."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Your existing session was reused."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("You were authenticated via password."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Welcome, ") + ".{0,64}?" + Pattern.quote("."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("You just registered via credential provider."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Please take a moment to read the server rules."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("We hope you enjoy your stay."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential provider migration completed."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Your account now authenticates via credential provider."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Use credential login from now on."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Enjoy your stay."), Pattern.DOTALL)
        ));

        assertTrue(scanner.matchesAny("Welcome back, Steve."));
        assertTrue(scanner.matchesAny("Your existing session was reused."));
        assertTrue(scanner.matchesAny("You were authenticated via password."));
        assertTrue(scanner.matchesAny("Welcome, Steve."));
        assertTrue(scanner.matchesAny("You just registered via credential provider."));
        assertTrue(scanner.matchesAny("Please take a moment to read the server rules."));
        assertTrue(scanner.matchesAny("We hope you enjoy your stay."));
        assertTrue(scanner.matchesAny("Credential provider migration completed."));
        assertTrue(scanner.matchesAny("Your account now authenticates via credential provider."));
        assertTrue(scanner.matchesAny("Use credential login from now on."));
        assertTrue(scanner.matchesAny("Enjoy your stay."));
    }

    @DisplayName("Real Identica strings: all credential password messages pass the filter")
    @Test
    void realIdenticaAllCredentialPasswordMessagesPass() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("Credential is too short."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential is too long."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential cannot contain spaces."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential needs an uppercase letter."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential needs a lowercase letter."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential needs a number."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential needs a special character."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential updated."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Current password is invalid."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("You must be logged in to change password."), Pattern.DOTALL)
        ));

        assertTrue(scanner.matchesAny("Credential is too short."));
        assertTrue(scanner.matchesAny("Credential is too long."));
        assertTrue(scanner.matchesAny("Credential cannot contain spaces."));
        assertTrue(scanner.matchesAny("Credential needs an uppercase letter."));
        assertTrue(scanner.matchesAny("Credential needs a lowercase letter."));
        assertTrue(scanner.matchesAny("Credential needs a number."));
        assertTrue(scanner.matchesAny("Credential needs a special character."));
        assertTrue(scanner.matchesAny("Credential updated."));
        assertTrue(scanner.matchesAny("Current password is invalid."));
        assertTrue(scanner.matchesAny("You must be logged in to change password."));
    }

    @DisplayName("Real Identica strings: all credential command messages pass the filter")
    @Test
    void realIdenticaAllCredentialCommandMessagesPass() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("You are about to migrate to the credential provider."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("After this, this account will authenticate through credentials."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Please rejoin the server to proceed with"), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("migration to the credential provider."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential migration cancelled."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential migration request expired."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("No pending credential migration."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential migration already pending."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential is already your primary provider."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential account registered."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential account deleted."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("No password account found."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Credential account already registered."), Pattern.DOTALL)
        ));

        assertTrue(scanner.matchesAny("You are about to migrate to the credential provider."));
        assertTrue(scanner.matchesAny("After this, this account will authenticate through credentials."));
        assertTrue(scanner.matchesAny("Please rejoin the server to proceed with"));
        assertTrue(scanner.matchesAny("migration to the credential provider."));
        assertTrue(scanner.matchesAny("Credential migration cancelled."));
        assertTrue(scanner.matchesAny("Credential migration request expired."));
        assertTrue(scanner.matchesAny("No pending credential migration."));
        assertTrue(scanner.matchesAny("Credential migration already pending."));
        assertTrue(scanner.matchesAny("Credential is already your primary provider."));
        assertTrue(scanner.matchesAny("Credential account registered."));
        assertTrue(scanner.matchesAny("Credential account deleted."));
        assertTrue(scanner.matchesAny("No password account found."));
        assertTrue(scanner.matchesAny("Credential account already registered."));
    }

    @DisplayName("Real Identica strings: all MessagesDefaults prompts pass the filter")
    @Test
    void realIdenticaAllMessagesDefaultsPromptsPass() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("Select an authentication method for account"), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("It can be changed later on."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Available providers:"), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("No providers available for this account."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Too many resume attempts."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Please try again in ") + ".{0,64}?" + Pattern.quote("s."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Your session was interrupted."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Logged in from another location."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Connection handshake was denied."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Unable to evaluate prepare policy."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Journey stage pipeline incomplete."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Journey step returned no status."), Pattern.DOTALL)
        ));

        assertTrue(scanner.matchesAny("Select an authentication method for account"));
        assertTrue(scanner.matchesAny("It can be changed later on."));
        assertTrue(scanner.matchesAny("Available providers:"));
        assertTrue(scanner.matchesAny("No providers available for this account."));
        assertTrue(scanner.matchesAny("Too many resume attempts."));
        assertTrue(scanner.matchesAny("Please try again in 30s."));
        assertTrue(scanner.matchesAny("Your session was interrupted."));
        assertTrue(scanner.matchesAny("Logged in from another location."));
        assertTrue(scanner.matchesAny("Connection handshake was denied."));
        assertTrue(scanner.matchesAny("Unable to evaluate prepare policy."));
        assertTrue(scanner.matchesAny("Journey stage pipeline incomplete."));
        assertTrue(scanner.matchesAny("Journey step returned no status."));
    }

    @DisplayName("Real Identica strings: all MessagesDefaults scenario messages pass the filter")
    @Test
    void realIdenticaAllMessagesDefaultsScenarioMessagesPass() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("Authentication could not be completed."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Unable to establish your session."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Username conflict detected."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Please join using the correct entrypoint:"), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Registration could not be completed."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("An account for this identity already exists."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Migration could not be completed."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Your pending migration was cancelled."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("You are still using your previous login provider."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Authentication is already in progress."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Registration is already in progress."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Migration is already in progress."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Your time for authentication ran out."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Your time for registration ran out."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Your time for migration ran out."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Please wait, processing your request."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Your previous request is still processing."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Please try again in a moment."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Authentication pipeline incomplete."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Registration pipeline incomplete."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Migration pipeline incomplete."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("No target server available for routing"), Pattern.DOTALL)
        ));

        assertTrue(scanner.matchesAny("Authentication could not be completed."));
        assertTrue(scanner.matchesAny("Unable to establish your session."));
        assertTrue(scanner.matchesAny("Username conflict detected."));
        assertTrue(scanner.matchesAny("Please join using the correct entrypoint:"));
        assertTrue(scanner.matchesAny("Registration could not be completed."));
        assertTrue(scanner.matchesAny("An account for this identity already exists."));
        assertTrue(scanner.matchesAny("Migration could not be completed."));
        assertTrue(scanner.matchesAny("Your pending migration was cancelled."));
        assertTrue(scanner.matchesAny("You are still using your previous login provider."));
        assertTrue(scanner.matchesAny("Authentication is already in progress."));
        assertTrue(scanner.matchesAny("Registration is already in progress."));
        assertTrue(scanner.matchesAny("Migration is already in progress."));
        assertTrue(scanner.matchesAny("Your time for authentication ran out."));
        assertTrue(scanner.matchesAny("Your time for registration ran out."));
        assertTrue(scanner.matchesAny("Your time for migration ran out."));
        assertTrue(scanner.matchesAny("Please wait, processing your request."));
        assertTrue(scanner.matchesAny("Your previous request is still processing."));
        assertTrue(scanner.matchesAny("Please try again in a moment."));
        assertTrue(scanner.matchesAny("Authentication pipeline incomplete."));
        assertTrue(scanner.matchesAny("Registration pipeline incomplete."));
        assertTrue(scanner.matchesAny("Migration pipeline incomplete."));
        assertTrue(scanner.matchesAny("No target server available for routing"));
    }

    @DisplayName("Real Identica strings: all MessagesDefaults error messages pass the filter")
    @Test
    void realIdenticaAllMessagesDefaultsErrorMessagesPass() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("Connection setup did not produce a context."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Authentication journey could not continue."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Registration journey could not continue."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Migration journey could not continue."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Unable to load provider profile."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Unable to synchronize username."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Account data is missing."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Unable to validate provider."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Unable to link provider."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Policy stage did not return a result."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Unable to review account."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Unable to verify account status."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Unable to create account."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Unable to build a session."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Journey did not return a result."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Journey could not start."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Finalization did not return a result."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("No providers available, if this issue persists"), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Please contact a server administrator."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("No providers matched, if this issue persists"), Pattern.DOTALL)
        ));

        assertTrue(scanner.matchesAny("Connection setup did not produce a context."));
        assertTrue(scanner.matchesAny("Authentication journey could not continue."));
        assertTrue(scanner.matchesAny("Registration journey could not continue."));
        assertTrue(scanner.matchesAny("Migration journey could not continue."));
        assertTrue(scanner.matchesAny("Unable to load provider profile."));
        assertTrue(scanner.matchesAny("Unable to synchronize username."));
        assertTrue(scanner.matchesAny("Account data is missing."));
        assertTrue(scanner.matchesAny("Unable to validate provider."));
        assertTrue(scanner.matchesAny("Unable to link provider."));
        assertTrue(scanner.matchesAny("Policy stage did not return a result."));
        assertTrue(scanner.matchesAny("Unable to review account."));
        assertTrue(scanner.matchesAny("Unable to verify account status."));
        assertTrue(scanner.matchesAny("Unable to create account."));
        assertTrue(scanner.matchesAny("Unable to build a session."));
        assertTrue(scanner.matchesAny("Journey did not return a result."));
        assertTrue(scanner.matchesAny("Journey could not start."));
        assertTrue(scanner.matchesAny("Finalization did not return a result."));
        assertTrue(scanner.matchesAny("No providers available, if this issue persists"));
        assertTrue(scanner.matchesAny("Please contact a server administrator."));
        assertTrue(scanner.matchesAny("No providers matched, if this issue persists"));
    }

    @DisplayName("Real Identica strings: enrollment entry format with placeholders passes")
    @Test
    void realIdenticaEnrollmentEntryFormatPasses() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("[") + ".{0,64}?" + Pattern.quote("]:") + ".{0,64}?" + Pattern.quote("{description}"), Pattern.DOTALL)
        ));

        assertTrue(scanner.matchesAny("[premium]: {description}"));
        assertTrue(scanner.matchesAny("[credential]: {description}"));
    }

    @DisplayName("Real Identica strings: all non-Identica messages are blocked")
    @Test
    void realIdenticaNonIdenticaMessagesAreBlocked() throws Exception {
        setPatterns(Set.of(
                Pattern.compile(Pattern.quote("Welcome back to our server."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("Please log in to proceed."), Pattern.DOTALL),
                Pattern.compile(Pattern.quote("You have ") + ".{0,64}?" + Pattern.quote(" tries left."), Pattern.DOTALL)
        ));

        assertFalse(scanner.matchesAny("Steve joined the game"));
        assertFalse(scanner.matchesAny("Notch: yes"));
        assertFalse(scanner.matchesAny("hello everyone"));
        assertFalse(scanner.matchesAny("tp Steve"));
        assertFalse(scanner.matchesAny("gamemode creative"));
        assertFalse(scanner.matchesAny("I said hello"));
        assertFalse(scanner.matchesAny("welcome to the server"));
        assertFalse(scanner.matchesAny("Please log in to the game"));
    }

    private void setPatterns(Set<Pattern> patterns) throws Exception {
        Field field = DefaultIdenticaMessageScanner.class.getDeclaredField("patterns");
        field.setAccessible(true);
        field.set(scanner, patterns);
    }
}