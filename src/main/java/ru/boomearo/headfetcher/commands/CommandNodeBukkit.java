package ru.boomearo.headfetcher.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import ru.boomearo.headfetcher.managers.ConfigManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public abstract class CommandNodeBukkit extends CommandNode<CommandSender, String> {

    protected final Plugin plugin;
    protected final String permission;
    protected ConfigManager configManager;

    public CommandNodeBukkit(Plugin plugin, CommandNodeBukkit root, String name, List<String> aliases, String permission, ConfigManager configManager) {
        super(root, name, aliases);
        this.plugin = plugin;
        this.permission = permission;
        this.configManager = configManager;
    }

    public CommandNodeBukkit(Plugin plugin, CommandNodeBukkit root, String name, String permission, ConfigManager configManager) {
        this(plugin, root, name, Collections.emptyList(), permission, configManager);
    }

    public CommandNodeBukkit(Plugin plugin, CommandNodeBukkit root, String name, List<String> aliases, ConfigManager configManager) {
        this(plugin, root, name, aliases, null, configManager);
    }

    public CommandNodeBukkit(Plugin plugin, CommandNodeBukkit root, String name, ConfigManager configManager) {
        this(plugin, root, name, Collections.emptyList(), configManager);
    }

    @Override
    public void onExecuteException(CommandSender sender, String[] args, Exception e) {
        this.plugin.getLogger().log(Level.SEVERE, "Failed command execute", e);
    }

    @Override
    public Collection<String> onSuggestException(CommandSender sender, String[] args, Exception e) {
        this.plugin.getLogger().log(Level.SEVERE, "Failed tab complete execute", e);
        return Collections.emptyList();
    }

    @Override
    public void onPermissionFailedExecute(CommandSender sender, String[] args) {
        sender.sendMessage(this.configManager.getMessage("do_not_have_permissions"));
    }

    @Override
    public Collection<String> onPermissionFailedSuggest(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        if (this.permission == null) {
            return true;
        }
        return sender.hasPermission(this.permission);
    }

    public void sendCurrentHelp(CommandSender sender) {
        List<String> descs = getDescription(sender);
        if (descs != null) {
            for (String text : descs) {
                sender.sendMessage(text);
            }
        }
    }

    public void sendHelp(CommandSender sender) {
        for (String text : getDescriptionListFromRoot(sender)) {
            sender.sendMessage(text);
        }
    }
}
