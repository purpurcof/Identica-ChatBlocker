package me.purpurcof.identica.addon.chatblocker.collector;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Binding;
import me.whereareiam.configura.ConfigDocument;
import me.whereareiam.identica.IdenticaAPI;
import me.whereareiam.identica.Reloadable;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

    private volatile Set<Pattern> patterns = Collections.emptySet();

    private static final Logger LOGGER = Logger.getLogger(DefaultIdenticaMessageScanner.class.getName());
    private static final Pattern MINIMESSAGE_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    private static final int MAX_RECURSION_DEPTH = 8;
    private static final int MIN_LITERAL_LENGTH = 8;
    private static final int MAX_PLACEHOLDER_GAP = 64;

    @Override
    public void reload() {
        scan();
    }

    @Override
    public void scan() {
        try {
            Set<String> rawStrings = new HashSet<>();
            collectFromAllMessageBeans(rawStrings);

            Set<Pattern> compiled = new HashSet<>();
            for (String s : rawStrings) {
                if (s == null || s.isBlank()) continue;
                String cleaned = MINIMESSAGE_TAG.matcher(s).replaceAll("").strip();
                if (cleaned.length() < MIN_LITERAL_LENGTH) continue;

                Pattern pattern = compileTemplate(cleaned);
                if (pattern != null) {
                    compiled.add(pattern);
                }
            }

            this.patterns = Collections.unmodifiableSet(compiled);
            LOGGER.info("Identica message scanner: compiled " + compiled.size() + " patterns from " + rawStrings.size() + " raw strings");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to scan Identica messages", e);
        }
    }

    @Override
    public boolean matchesAny(String plainText) {
        if (plainText == null || plainText.isEmpty()) return false;
        for (Pattern pattern : patterns) {
            if (pattern.matcher(plainText).find()) return true;
        }

        return false;
    }

    private void collectFromAllMessageBeans(Set<String> result) {
        Injector injector = IdenticaAPI.getService(Injector.class);
        Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();

        for (Map.Entry<Key<?>, Binding<?>> entry : bindings.entrySet()) {
            Key<?> key = entry.getKey();
            Binding<?> binding = entry.getValue();

            try {
                java.lang.reflect.Type type = key.getTypeLiteral().getType();
                if (!(type instanceof Class<?> clazz)) continue;
                if (!ConfigDocument.class.isAssignableFrom(clazz)) continue;

                Object instance = binding.getProvider().get();
                if (instance == null) continue;

                collectStrings(instance, result);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get message bean: " + key, e);
            }
        }
    }

    private Pattern compileTemplate(String template) {
        String[] parts = PLACEHOLDER.split(template, -1);
        int totalLiteralLength = 0;
        for (String part : parts) {
            totalLiteralLength += part.length();
        }

        if (totalLiteralLength < MIN_LITERAL_LENGTH) return null;

        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append(".{0,").append(MAX_PLACEHOLDER_GAP).append("}?");
            }

            regex.append(Pattern.quote(parts[i]));
        }

        return Pattern.compile(regex.toString(), Pattern.DOTALL);
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
            switch (obj) {
                case String s -> {
                    result.add(s);
                    return;
                }

                case Collection<?> col -> {
                    for (Object item : col) {
                        collectStrings(item, result, visited, depth + 1);
                    }

                    return;
                }

                case Map<?, ?> map -> {
                    for (Object value : map.values()) {
                        collectStrings(value, result, visited, depth + 1);
                    }

                    return;
                }

                default -> {}
            }

            if (obj.getClass().isArray()) {
                int length = Array.getLength(obj);
                for (int i = 0; i < length; i++) {
                    collectStrings(Array.get(obj, i), result, visited, depth + 1);
                }

                return;
            }

            Class<?> clazz = obj.getClass();
            if (clazz.getName().startsWith("java.")) return;
            if (clazz.isEnum()) return;

            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;

                    boolean accessible = field.canAccess(obj);
                    try {
                        if (!accessible) {
                            field.setAccessible(true);
                        }

                        Object value = field.get(obj);
                        collectStrings(value, result, visited, depth + 1);
                    } catch (SecurityException se) {
                        LOGGER.log(Level.WARNING, "Security manager denied access to field: " + field.getDeclaringClass().getName() + "." + field.getName(), se);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Exception ignored while accessing field: " + field.getDeclaringClass().getName() + "." + field.getName(), e);
                    } finally {
                        if (!accessible) {
                            try {
                                field.setAccessible(false);
                            } catch (Exception ignore) {}
                        }
                    }
                }

                clazz = clazz.getSuperclass();
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Unexpected error during message collection", t);
        }
    }
}