package ru.boomearo.headfetcher.managers;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class ConfigManager {

    private final Plugin plugin;

    private Map<String, String> messages = new HashMap<>();

    public void load() {
        File configFile = new File(this.plugin.getDataFolder() + File.separator + "config.yml");
        if (!configFile.exists()) {
            this.plugin.getLogger().info("Configuration is not found, creating a new one...");
            this.plugin.saveDefaultConfig();
        }

        this.plugin.reloadConfig();

        FileConfiguration configuration = this.plugin.getConfig();

        Map<String, String> tmp = new HashMap<>();
        ConfigurationSection messagesSection = configuration.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                String message = messagesSection.getString(key);
                String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
                tmp.put(key, coloredMessage);
            }
        }
        this.messages = tmp;
    }

    public String getMessage(String key) {
        String message = this.messages.get(key);
        if (message == null) {
            return "<invalid message translate key '" + key + "'>";
        }

        return message;
    }

}
