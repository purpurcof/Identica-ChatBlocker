package me.purpurcof.identica.addon.chatblocker;

import me.purpurcof.identica.addon.chatblocker.collector.DefaultIdenticaMessageScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Default Identica Message Scanner")
class DefaultIdenticaMessageScannerTest {

    private final DefaultIdenticaMessageScanner scanner = new DefaultIdenticaMessageScanner();

    @BeforeEach
    void reset() throws Exception {
        setPlainTextFragments(Collections.emptySet());
    }

    @DisplayName("Returns false when not scanned (empty fragments)")
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

    @DisplayName("Returns true when input contains any fragment")
    @Test
    void matchesAnyReturnsTrueWhenFragmentMatches() throws Exception {
        setPlainTextFragments(Set.of("Hello World", "test"));

        assertTrue(scanner.matchesAny("Hello World"));
        assertTrue(scanner.matchesAny("prefix Hello World suffix"));
    }

    @DisplayName("Returns false when input does not contain any fragment")
    @Test
    void matchesAnyReturnsFalseWhenNoFragmentMatch() throws Exception {
        setPlainTextFragments(Set.of("Hello World"));

        assertFalse(scanner.matchesAny("Goodbye World"));
        assertFalse(scanner.matchesAny("Helloworld"));
    }

    @DisplayName("Returns true when input matches shorter fragment inside longer text")
    @Test
    void matchesAnyWithPartialContainment() throws Exception {
        setPlainTextFragments(Set.of("Hello"));

        assertTrue(scanner.matchesAny("Hello World"));
        assertTrue(scanner.matchesAny("Say Hello"));
    }

    @DisplayName("Returns false when no fragment matches")
    @ParameterizedTest
    @ValueSource(strings = {"other", "xyz", "completely different text"})
    void matchesAnyReturnsFalseWhenNotMatching(String input) throws Exception {
        setPlainTextFragments(Set.of("Hello World"));

        assertFalse(scanner.matchesAny(input));
    }

    @DisplayName("Matches multiple fragments correctly")
    @Test
    void matchesAnyWithMultipleFragments() throws Exception {
        setPlainTextFragments(Set.of("first", "second", "third"));

        assertTrue(scanner.matchesAny("this is the first one"));
        assertTrue(scanner.matchesAny("second place"));
        assertTrue(scanner.matchesAny("the third option"));
        assertFalse(scanner.matchesAny("something else"));
    }

    private void setPlainTextFragments(Set<String> fragments) throws Exception {
        Field field = DefaultIdenticaMessageScanner.class.getDeclaredField("plainTextFragments");
        field.setAccessible(true);
        field.set(scanner, fragments);
    }
}