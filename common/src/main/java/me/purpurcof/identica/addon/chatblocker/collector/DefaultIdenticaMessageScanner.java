package me.purpurcof.identica.addon.chatblocker.collector;

import me.whereareiam.identica.IdenticaAPI;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.model.config.Messages;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class DefaultIdenticaMessageScanner implements IdenticaMessageScanner, Reloadable {

    private volatile Set<String> plainTextFragments = Collections.emptySet();

    private static final Logger LOGGER = Logger.getLogger(DefaultIdenticaMessageScanner.class.getName());
    private static final Pattern MINIMESSAGE_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    private static final int MAX_RECURSION_DEPTH = 8;

    @Override
    public void reload() {
        scan();
    }

    @Override
    public void scan() {
        try {
            Messages messages = IdenticaAPI.getService(Messages.class);
            Set<String> rawStrings = new HashSet<>();
            collectStrings(messages, rawStrings);

            Set<String> fragments = new HashSet<>();
            for (String s : rawStrings) {
                if (s == null || s.isBlank()) continue;
                String cleaned = PLACEHOLDER.matcher(s).replaceAll("");
                cleaned = MINIMESSAGE_TAG.matcher(cleaned).replaceAll("").strip();

                if (cleaned.length() >= 3) {
                    fragments.add(cleaned);
                }
            }

            this.plainTextFragments = Collections.unmodifiableSet(fragments);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to scan Identica messages", e);
        }
    }

    @Override
    public boolean matchesAny(String plainText) {
        if (plainText == null || plainText.isEmpty()) return false;
        for (String fragment : plainTextFragments) {
            if (plainText.contains(fragment)) return true;
        }

        return false;
    }

    private void collectStrings(Object obj, Set<String> result) {
        collectStrings(obj, result, new IdentityHashMap<>(), 0);
    }

    private void collectStrings(Object obj, Set<String> result, Map<Object, Boolean> visited, int depth) {
        if (obj == null) return;
        if (depth > MAX_RECURSION_DEPTH) return;
        if (visited.containsKey(obj)) return;
        visited.put(obj, Boolean.TRUE);

        try {
            if (obj instanceof String s) {
                result.add(s);
                return;
            }

            if (obj instanceof Collection<?> col) {
                for (Object item : col) {
                    collectStrings(item, result, visited, depth + 1);
                }

                return;
            }

            Class<?> clazz = obj.getClass();
            if (clazz.getName().startsWith("java.")) return;
            if (clazz.isEnum()) return;

            for (Field field : clazz.getDeclaredFields()) {
                boolean accessible = field.canAccess(obj);
                try {
                    if (!accessible) {
                        field.setAccessible(true);
                    }

                    Object value = field.get(obj);
                    collectStrings(value, result, visited, depth + 1);
                } catch (SecurityException se) {
                    LOGGER.log(Level.FINE, "Security manager denied access to field: " + field.getName(), se);
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Exception ignored while accessing field", e);
                } finally {
                    if (!accessible) {
                        try {
                            field.setAccessible(false);
                        } catch (Exception ignore) {}
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Unexpected error during message collection", t);
        }
    }
}