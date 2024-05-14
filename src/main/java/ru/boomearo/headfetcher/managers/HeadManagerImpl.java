package ru.boomearo.headfetcher.managers;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;
import ru.boomearo.headfetcher.HeadFetcher;
import ru.boomearo.headfetcher.PlayerUUIDData;
import ru.boomearo.headfetcher.UUIDTextureData;
import ru.boomearo.headfetcher.api.HeadManager;
import ru.boomearo.headfetcher.json.SkinProfileJson;
import ru.boomearo.headfetcher.json.UUIDDataJson;
import ru.boomearo.headfetcher.repository.DatabaseRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;

@RequiredArgsConstructor
public class HeadManagerImpl implements HeadManager {

    private static final String MOJANG_PROFILES_URL = "https://api.mojang.com/profiles/minecraft";
    private static final String MOJANG_SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final long MAX_CACHE_TIME = Duration.ofDays(3).toMillis();
    private static final Gson GSON = new Gson();

    private final HeadFetcher headFetcher;
    private final DatabaseRepository databaseRepository;

    private final ConcurrentMap<String, PlayerUUIDData> cacheUUID = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUIDTextureData> cacheTexture = new ConcurrentHashMap<>();

    private ExecutorService executor = null;

    public void load() {
        if (this.executor == null) {
            this.executor = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder()
                    .setNameFormat("HeadFetcher-thread-%d")
                    .build());
        }
    }

    public void unload() {
        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }
    }

    @Override
    public ItemStack applyCachedHeadTexture(ItemStack itemStack, String playerName) {
        Preconditions.checkArgument(itemStack != null, "ItemStack is null!");
        Preconditions.checkArgument(playerName != null, "playerName is null!");

        PlayerUUIDData uuid = this.cacheUUID.get(playerName.toLowerCase(Locale.ROOT));
        if (uuid == null) {
            return itemStack;
        }

        UUIDTextureData texture = this.cacheTexture.get(uuid.uuid());
        if (texture == null) {
            return itemStack;
        }

        return applyTexture(itemStack, texture.texture());
    }

    @Override
    public CompletableFuture<ItemStack> applyHeadTexture(ItemStack itemStack, String playerName) {
        Preconditions.checkArgument(itemStack != null, "ItemStack is null!");
        Preconditions.checkArgument(playerName != null, "playerName is null!");

        if (this.executor == null) {
            return CompletableFuture.completedFuture(itemStack);
        }

        return CompletableFuture.supplyAsync(() -> fetchHeadTextureName(itemStack, playerName), this.executor);
    }

    @Override
    public ItemStack applyCachedHeadTexture(ItemStack itemStack, UUID uuid) {
        Preconditions.checkArgument(itemStack != null, "ItemStack is null!");
        Preconditions.checkArgument(uuid != null, "uuid is null!");

        UUIDTextureData texture = this.cacheTexture.get(uuid.toString().replace("-", ""));
        if (texture == null) {
            return itemStack;
        }

        return applyTexture(itemStack, texture.texture());
    }

    @Override
    public CompletableFuture<ItemStack> applyHeadTexture(ItemStack itemStack, UUID uuid) {
        Preconditions.checkArgument(itemStack != null, "ItemStack is null!");
        Preconditions.checkArgument(uuid != null, "uuid is null!");

        if (this.executor == null) {
            return CompletableFuture.completedFuture(itemStack);
        }

        return CompletableFuture.supplyAsync(() -> fetchHeadTextureUUID(itemStack, uuid.toString().replace("-", "")), this.executor);
    }

    private ItemStack fetchHeadTextureName(ItemStack itemStack, String playerName) {
        PlayerUUIDData playerUUIDData = getUUIDByName(playerName.toLowerCase(Locale.ROOT));

        String uuid = playerUUIDData.uuid();
        if (uuid == null) {
            return itemStack;
        }

        return fetchHeadTextureUUID(itemStack, uuid);
    }

    private ItemStack fetchHeadTextureUUID(ItemStack itemStack, String uuid) {
        UUIDTextureData uuidTextureData = getSkinProfileByUUID(uuid);

        String texture = uuidTextureData.texture();
        if (texture == null) {
            return itemStack;
        }

        return applyTexture(itemStack, texture);
    }

    private ItemStack applyTexture(ItemStack itemStack, String texture) {
        NBT.modify(itemStack, readWriteItemNBT -> {
            ReadWriteNBT skullOwnerCompound = readWriteItemNBT.getOrCreateCompound("SkullOwner");

            skullOwnerCompound.setUUID("Id", UUID.randomUUID());

            skullOwnerCompound.getOrCreateCompound("Properties")
                    .getCompoundList("textures")
                    .addCompound()
                    .setString("Value", texture);
        });

        return itemStack;
    }

    private PlayerUUIDData getUUIDByName(String name) {
        PlayerUUIDData tmp = this.cacheUUID.get(name);
        if (tmp != null) {
            if ((System.currentTimeMillis() - tmp.timeAdded()) > MAX_CACHE_TIME) {
                return connectAndSaveUUID(name);
            }

            return tmp;
        }

        PlayerUUIDData playerUUIDData = null;
        try {
            playerUUIDData = this.databaseRepository.getPlayerUUID(name).get();
        } catch (Exception e) {
            this.headFetcher.getLogger().log(Level.SEVERE, "Failed to get player uuid", e);
        }

        if (playerUUIDData == null) {
            return connectAndSaveUUID(name);
        }

        if ((System.currentTimeMillis() - playerUUIDData.timeAdded()) > MAX_CACHE_TIME) {
            return connectAndSaveUUID(name);
        }

        this.cacheUUID.put(name, playerUUIDData);

        return playerUUIDData;
    }

    private PlayerUUIDData connectAndSaveUUID(String name) {
        String uuid = connectToGetUUID(name);

        PlayerUUIDData newPlayerUUIDData = new PlayerUUIDData(name, uuid, System.currentTimeMillis());

        this.cacheUUID.put(name, newPlayerUUIDData);

        this.databaseRepository.insertOrUpdatePlayerUUID(newPlayerUUIDData);

        return newPlayerUUIDData;
    }

    private String connectToGetUUID(String name) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(new URI(MOJANG_PROFILES_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(List.of(name))))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());

            List<UUIDDataJson> uuidDataJsonList = GSON.fromJson(response.body(), new TypeToken<List<UUIDDataJson>>() {
            }.getType());
            if (uuidDataJsonList != null && !uuidDataJsonList.isEmpty()) {
                String uuid = uuidDataJsonList.get(0).getId();
                if (uuid != null) {
                    return uuid;
                }
            }
        } catch (Exception e) {
            this.headFetcher.getLogger().log(Level.SEVERE, "Failed to get uuid for player " + name, e);
        }

        return null;
    }

    private UUIDTextureData getSkinProfileByUUID(String uuid) {
        UUIDTextureData tmp = this.cacheTexture.get(uuid);
        if (tmp != null) {
            if ((System.currentTimeMillis() - tmp.timeAdded()) > MAX_CACHE_TIME) {
                return connectAndSaveProfile(uuid);
            }

            return tmp;
        }

        UUIDTextureData uuidTextureData = null;
        try {
            uuidTextureData = this.databaseRepository.getUUIDTexture(uuid).get();
        } catch (Exception e) {
            this.headFetcher.getLogger().log(Level.SEVERE, "Failed to get uuid texture", e);
        }

        if (uuidTextureData == null) {
            return connectAndSaveProfile(uuid);
        }

        if ((System.currentTimeMillis() - uuidTextureData.timeAdded()) > MAX_CACHE_TIME) {
            return connectAndSaveProfile(uuid);
        }

        this.cacheTexture.put(uuid, uuidTextureData);

        return uuidTextureData;
    }

    private UUIDTextureData connectAndSaveProfile(String uuid) {
        String texture = connectToGetSkinProfile(uuid);

        UUIDTextureData newUUIDTextureData = new UUIDTextureData(uuid, texture, System.currentTimeMillis());

        this.cacheTexture.put(uuid, newUUIDTextureData);

        this.databaseRepository.insertOrUpdateUUIDTexture(newUUIDTextureData);

        return newUUIDTextureData;
    }

    private String connectToGetSkinProfile(String uuid) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(new URI(MOJANG_SESSION_URL + uuid.replace("-", "") + "?unsigned=true"))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());

            SkinProfileJson skinProfileJson = GSON.fromJson(response.body(), SkinProfileJson.class);
            if (skinProfileJson != null) {
                List<SkinProfileJson.Property> properties = skinProfileJson.getProperties();
                if (properties != null) {
                    for (SkinProfileJson.Property property : properties) {
                        String name = property.getName();
                        if (name == null) {
                            continue;
                        }
                        if (name.equals("textures")) {
                            return property.getValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            this.headFetcher.getLogger().log(Level.SEVERE, "Failed to get profile for uuid " + uuid, e);
        }

        return null;
    }

}
