package ru.boomearo.headfetcher;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import ru.boomearo.headfetcher.api.HeadManager;
import ru.boomearo.headfetcher.commands.headfetcher.CommandHeadFetcherExecutor;
import ru.boomearo.headfetcher.managers.ConfigManager;
import ru.boomearo.headfetcher.managers.HeadManagerImpl;
import ru.boomearo.headfetcher.repository.DatabaseRepository;

public class HeadFetcher extends JavaPlugin {

    @Getter
    private static HeadFetcher instance = null;

    @Getter
    private ConfigManager configManager;
    private DatabaseRepository databaseRepository;
    private HeadManagerImpl headManager;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.databaseRepository = new DatabaseRepository(this);
        this.databaseRepository.load();

        this.headManager = new HeadManagerImpl(this, this.databaseRepository);
        this.headManager.load();

        this.getCommand("headfetcher").setExecutor(new CommandHeadFetcherExecutor(
                this,
                this.configManager,
                this.headManager
        ));

        this.getLogger().info("Plugin successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (this.databaseRepository != null) {
            this.databaseRepository.unload();
        }

        if (this.headManager != null) {
            this.headManager.unload();
        }

        this.getLogger().info("Plugin successfully disabled!");
    }

    public HeadManager getHeadManager() {
        return this.headManager;
    }
}
