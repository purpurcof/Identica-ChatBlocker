package me.purpurcof.identica.addon.chatblocker.collector;

public interface IdenticaMessageScanner {

    void scan();

    boolean matchesAny(String plainText);
}