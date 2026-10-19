package me.purpurcof.identica.addon.chatblocker.collector;

public interface IdenticaMessageScanner {

    void scan();

    /**
     * @return {@code true} when at least one message template is loaded. While this is {@code false} the
     * scanner cannot tell Identica's messages from anything else, so callers must not filter with it.
     */
    boolean isReady();

    boolean matchesAny(String plainText);
}