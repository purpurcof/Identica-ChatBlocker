package me.purpurcof.identica.addon.chatblocker.collector;

import me.whereareiam.identica.IdenticaAPI;
import me.whereareiam.identica.model.config.Messages;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class DefaultIdenticaMessageScanner implements IdenticaMessageScanner {

    private static final Pattern MINIMESSAGE_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    private Set<String> plainTextFragments = Collections.emptySet();

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
        } catch (Exception ignored) {}
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
        if (obj == null) return;

        switch (obj) {
            case String s -> result.add(s);
            case Collection<?> col -> {
                for (Object item : col) {
                    collectStrings(item, result);
                }
            }

            default -> {
                Class<?> clazz = obj.getClass();
                if (clazz.getName().startsWith("java.")) return;
                if (clazz.isEnum()) return;

                for (Field field : clazz.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(obj);
                        collectStrings(value, result);
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}