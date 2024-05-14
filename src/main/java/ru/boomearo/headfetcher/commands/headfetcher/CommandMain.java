package ru.boomearo.headfetcher.commands.headfetcher;

import org.bukkit.command.CommandSender;
import ru.boomearo.headfetcher.commands.CommandNodeBukkit;
import ru.boomearo.headfetcher.managers.ConfigManager;

import java.util.Collections;
import java.util.List;

public class CommandMain extends CommandNodeBukkit {

    public CommandMain(ConfigManager configManager) {
        super(null, null, "root", "headfetcher.command", configManager);
    }

    @Override
    public List<String> getDescription(CommandSender sender) {
        return Collections.singletonList(this.configManager.getMessage("command_main"));
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        sendHelp(sender);
    }

}
