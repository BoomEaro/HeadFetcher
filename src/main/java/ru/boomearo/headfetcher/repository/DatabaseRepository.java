package ru.boomearo.headfetcher.repository;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bukkit.plugin.Plugin;
import org.sqlite.JDBC;
import ru.boomearo.headfetcher.PlayerUUIDData;
import ru.boomearo.headfetcher.UUIDTextureData;

import java.io.File;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@RequiredArgsConstructor
public class DatabaseRepository {

    private static final String CON_STR = "jdbc:sqlite:[path]database.db";

    private final Plugin plugin;

    private Connection connection;
    private ExecutorService executor;

    @SneakyThrows
    public void load() {
        if (this.connection != null) {
            return;
        }

        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdir();
        }

        DriverManager.registerDriver(new JDBC());

        this.executor = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder()
                .setNameFormat("HeadFetcher-SQL")
                .setPriority(3)
                .build());

        this.connection = DriverManager.getConnection(CON_STR.replace("[path]", this.plugin.getDataFolder() + File.separator));

        createPlayerUUIDTable();
        createUUIDTextureTable();
    }

    private void createPlayerUUIDTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_uuid ("
                + " name VARCHAR(255) PRIMARY KEY NOT NULL,"
                + " uuid VARCHAR(255),"
                + " time_added BIGINT NOT NULL DEFAULT 0"
                + ");";

        try (Statement stmt = this.connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to create player_uuid table", e);
        }
    }

    private void createUUIDTextureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS uuid_texture ("
                + " uuid VARCHAR(255) PRIMARY KEY NOT NULL,"
                + " texture VARCHAR(32767),"
                + " time_added BIGINT NOT NULL DEFAULT 0"
                + ");";

        try (Statement stmt = this.connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to create uuid_texture table", e);
        }
    }

    public void insertOrUpdatePlayerUUID(PlayerUUIDData playerUUIDData) {
        if (this.executor == null) {
            return;
        }

        this.executor.execute(() -> {
            if (this.connection == null) {
                return;
            }

            String sql = "REPLACE INTO player_uuid (name, uuid, time_added)" +
                    "VALUES(?, ?, ?)";

            try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
                statement.setString(1, playerUUIDData.name());
                statement.setString(2, playerUUIDData.uuid());
                statement.setLong(3, playerUUIDData.timeAdded());
                statement.execute();
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to insert player uuid " + playerUUIDData, e);
            }
        });
    }

    public CompletableFuture<PlayerUUIDData> getPlayerUUID(String name) {
        if (this.executor == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            if (this.connection == null) {
                return null;
            }

            try (PreparedStatement statement = this.connection.prepareStatement("SELECT name, uuid, time_added FROM player_uuid WHERE name = ? LIMIT 1")) {
                statement.setString(1, name);

                ResultSet resSet = statement.executeQuery();

                if (resSet.next()) {
                    return new PlayerUUIDData(resSet.getString("name"), resSet.getString("uuid"), resSet.getLong("time_added"));
                }
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to get player uuid " + name, e);
            }

            return null;
        }, this.executor);
    }

    public void insertOrUpdateUUIDTexture(UUIDTextureData uuidTextureData) {
        if (this.executor == null) {
            return;
        }

        this.executor.execute(() -> {
            if (this.connection == null) {
                return;
            }

            String sql = "REPLACE INTO uuid_texture (uuid, texture, time_added)" +
                    "VALUES(?, ?, ?)";

            try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
                statement.setString(1, uuidTextureData.uuid());
                statement.setString(2, uuidTextureData.texture());
                statement.setLong(3, uuidTextureData.timeAdded());
                statement.execute();
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to insert uuid texture " + uuidTextureData, e);
            }
        });
    }

    public CompletableFuture<UUIDTextureData> getUUIDTexture(String uuid) {
        if (this.executor == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            if (this.connection == null) {
                return null;
            }

            try (PreparedStatement statement = this.connection.prepareStatement("SELECT uuid, texture, time_added FROM uuid_texture WHERE uuid = ? LIMIT 1")) {
                statement.setString(1, uuid);

                ResultSet resSet = statement.executeQuery();

                if (resSet.next()) {
                    return new UUIDTextureData(resSet.getString("uuid"), resSet.getString("texture"), resSet.getLong("time_added"));
                }
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to get uuid texture " + uuid, e);
            }

            return null;
        }, this.executor);
    }

    @SneakyThrows
    public void unload() {
        if (this.connection == null) {
            return;
        }

        this.executor.shutdown();
        this.executor.awaitTermination(15, TimeUnit.SECONDS);
        this.executor = null;

        this.connection.close();
        this.connection = null;
    }
}
