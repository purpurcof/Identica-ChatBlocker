package me.purpurcof.identica.addon.chatblocker.collector;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Binding;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import me.whereareiam.configura.ConfigDocument;
import me.whereareiam.identica.IdenticaAPI;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.Reloadable;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
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
    // far above what a legitimate match needs (a few thousand steps), far below what a pathological one burns
    private static final long MAX_MATCH_STEPS = 2_000_000L;

    @Override
    public void reload() {
        scan();
    }

    @Override
    public void scan() {
        try {
            Set<String> rawStrings = new HashSet<>();
            for (Object bean : loadMessageBeans()) {
                collectStrings(bean, rawStrings);
            }

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
    public boolean isReady() {
        return !patterns.isEmpty();
    }

    @Override
    public boolean matchesAny(String plainText) {
        if (plainText == null || plainText.isEmpty()) return false;

        BoundedCharSequence bounded = new BoundedCharSequence(plainText, MAX_MATCH_STEPS);
        try {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(bounded).find()) return true;
            }
        } catch (BoundedCharSequence.BudgetExceededException e) {
            LOGGER.log(Level.FINE, "Message matching exceeded its step budget; treating it as not an Identica message");
        }

        return false;
    }

    /**
     * Every message bean Identica has loaded.
     *
     * <p>Two sources are needed: the root injector knows the core beans, but provider modules (credential,
     * premium, ...) live in <em>child</em> injectors whose bindings the root injector does not list. Those
     * beans are reachable through the config providers that registered themselves as reloadables.
     */
    protected Collection<Object> loadMessageBeans() {
        List<Object> beans = new ArrayList<>();

        collectFromRootInjector(beans);
        collectFromRegisteredProviders(beans);

        return beans;
    }

    private void collectFromRootInjector(List<Object> beans) {
        Injector injector = IdenticaAPI.getService(Injector.class);
        Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();

        for (Map.Entry<Key<?>, Binding<?>> entry : bindings.entrySet()) {
            Key<?> key = entry.getKey();
            Binding<?> binding = entry.getValue();

            try {
                java.lang.reflect.Type type = key.getTypeLiteral().getType();
                if (!(type instanceof Class<?> clazz)) continue;
                if (!ConfigDocument.class.isAssignableFrom(clazz)) continue;

                addDistinct(beans, binding.getProvider().get());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get message bean: " + key, e);
            }
        }
    }

    private void collectFromRegisteredProviders(List<Object> beans) {
        try {
            Registry<Reloadable> registry = IdenticaAPI.getService(Key.get(new TypeLiteral<Registry<Reloadable>>() {}));
            for (Reloadable reloadable : registry.values()) {
                // every config provider registers itself here and exposes its current bean through get()
                if (!(reloadable instanceof Provider<?> provider)) continue;

                try {
                    Object bean = provider.get();
                    if (bean != null && bean.getClass().getSimpleName().endsWith("Messages")) addDistinct(beans, bean);
                } catch (RuntimeException e) {
                    LOGGER.log(Level.FINE, "Skipping config provider " + reloadable.getClass().getName(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to read the registered config providers", e);
        }
    }

    private static void addDistinct(List<Object> beans, Object bean) {
        if (bean == null) return;
        for (Object existing : beans) {
            if (existing == bean) return;
        }

        beans.add(bean);
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