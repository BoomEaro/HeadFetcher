package ru.boomearo.headfetcher.commands.headfetcher;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import ru.boomearo.headfetcher.commands.CommandNodeBukkit;
import ru.boomearo.headfetcher.managers.ConfigManager;

import java.util.Collections;
import java.util.List;

public class CommandReload extends CommandNodeBukkit {

    public CommandReload(Plugin plugin, CommandNodeBukkit root, ConfigManager configManager) {
        super(plugin, root, "reload", "headfetcher.command.reload", configManager);
    }

    @Override
    public List<String> getDescription(CommandSender commandSender) {
        return Collections.singletonList(this.configManager.getMessage("command_reload"));
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (args.length != 0) {
            sendCurrentHelp(sender);
            return;
        }

        this.configManager.load();

        sender.sendMessage(this.configManager.getMessage("configuration_reloaded"));
    }
}
