package me.purpurcof.identica.addon.chatblocker;

import me.purpurcof.identica.addon.chatblocker.collector.DefaultIdenticaMessageScanner;
import me.whereareiam.identica.IdenticaAPI;
import me.whereareiam.identica.config.ConfigCatalog;
import me.whereareiam.identica.model.config.MessageDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Default Identica Message Scanner: bean discovery and match budget")
class DefaultIdenticaMessageScannerDiscoveryTest {

    // shaped like Identica's messages documents (a class hierarchy with lists)
    static class BaseMessages implements MessageDocument {
        String expired = "<red>Your authentication window has expired.</red>";
    }

    static class CredentialMessages extends BaseMessages {
        List<String> prompt = List.of("<gray>Use <yellow>/pass</yellow> [Password] to continue.</gray>");
    }

    private static DefaultIdenticaMessageScanner scannerWith(MessageDocument... documents) {
        ConfigCatalog catalog = mock(ConfigCatalog.class);
        when(catalog.documents(MessageDocument.class)).thenReturn(List.of(documents));
        IdenticaAPI identica = mock(IdenticaAPI.class);
        when(identica.getConfigCatalog()).thenReturn(catalog);

        return new DefaultIdenticaMessageScanner(identica);
    }

    @DisplayName("Is not ready before anything was scanned")
    @Test
    void notReadyBeforeScan() {
        assertFalse(scannerWith(new CredentialMessages()).isReady());
    }

    @DisplayName("Scans every messages document of the config catalog, including provider documents")
    @Test
    void scansAllBeans() {
        DefaultIdenticaMessageScanner scanner = scannerWith(new CredentialMessages());

        scanner.scan();

        assertTrue(scanner.isReady());
        assertTrue(scanner.matchesAny("Use /pass [Password] to continue."));
        assertTrue(scanner.matchesAny("Your authentication window has expired."));
        assertFalse(scanner.matchesAny("Steve joined the game"));
    }

    @DisplayName("Is not ready when the catalog holds no messages document")
    @Test
    void notReadyWithoutBeans() {
        DefaultIdenticaMessageScanner scanner = scannerWith();

        scanner.scan();

        assertFalse(scanner.isReady());
    }

    @DisplayName("A pathological template/message pair is cut off by the step budget instead of burning CPU")
    @Test
    void pathologicalMatchIsBounded() throws Exception {
        DefaultIdenticaMessageScanner scanner = scannerWith();
        // five placeholders compiled the way the scanner compiles them: {a} - {b} - {c} - {d} - {e} done
        String gap = ".{0,64}?";
        String regex = gap + Pattern.quote(" - ") + gap + Pattern.quote(" - ") + gap + Pattern.quote(" - ")
                + gap + Pattern.quote(" - ") + gap + Pattern.quote(" done");
        Field field = DefaultIdenticaMessageScanner.class.getDeclaredField("patterns");
        field.setAccessible(true);
        field.set(scanner, Set.of(Pattern.compile(regex, Pattern.DOTALL)));

        long start = System.nanoTime();
        boolean matched = scanner.matchesAny(" - ".repeat(80));
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertFalse(matched);
        assertTrue(millis < 1500, "matching took " + millis + " ms");
    }
}
