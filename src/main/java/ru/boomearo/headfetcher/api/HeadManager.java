package ru.boomearo.headfetcher.api;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface HeadManager {

    ItemStack applyCachedHeadTexture(ItemStack itemStack, String playerName);

    ItemStack applyCachedHeadTexture(ItemStack itemStack, UUID uuid);

    CompletableFuture<ItemStack> applyHeadTexture(ItemStack itemStack, String playerName);

    CompletableFuture<ItemStack> applyHeadTexture(ItemStack itemStack, UUID uuid);

}
