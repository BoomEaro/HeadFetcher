package ru.boomearo.headfetcher.commands.headfetcher;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import ru.boomearo.headfetcher.api.HeadManager;
import ru.boomearo.headfetcher.commands.CommandNodeBukkit;
import ru.boomearo.headfetcher.managers.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class CommandHeadFetcherExecutor implements CommandExecutor, TabCompleter {

    private final CommandNodeBukkit node;

    public CommandHeadFetcherExecutor(Plugin plugin, ConfigManager configManager, HeadManager headManager) {
        CommandMain root = new CommandMain(configManager);
        root.addNode(new CommandReload(plugin, root, configManager));
        root.addNode(new CommandApply(plugin, root, configManager, headManager));

        this.node = root;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.node.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>(this.node.suggest(sender, args));
    }
}
