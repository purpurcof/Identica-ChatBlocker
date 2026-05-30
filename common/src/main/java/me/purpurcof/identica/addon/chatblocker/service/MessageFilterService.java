package me.purpurcof.identica.addon.chatblocker.service;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface MessageFilterService {

    boolean isBlocked(@NotNull UUID connectionUniqueId);
}