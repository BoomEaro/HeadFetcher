package ru.boomearo.headfetcher.commands.headfetcher;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import ru.boomearo.headfetcher.api.HeadManager;
import ru.boomearo.headfetcher.commands.CommandNodeBukkit;
import ru.boomearo.headfetcher.managers.ConfigManager;

import java.util.Collections;
import java.util.List;

public class CommandApply extends CommandNodeBukkit {

    private final HeadManager headManager;

    public CommandApply(Plugin plugin, CommandNodeBukkit root, ConfigManager configManager, HeadManager headManager) {
        super(plugin, root, "apply", "headfetcher.command.apply", configManager);
        this.headManager = headManager;
    }

    @Override
    public List<String> getDescription(CommandSender commandSender) {
        return Collections.singletonList(this.configManager.getMessage("command_apply"));
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.configManager.getMessage("only_players"));
            return;
        }

        if (args.length != 1) {
            sendCurrentHelp(sender);
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack itemStack = inventory.getItemInMainHand();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            sender.sendMessage(this.configManager.getMessage("invalid_item"));
            return;
        }

        String name = args[0];

        this.headManager.applyHeadTexture(itemStack, name).whenComplete((newItem, throwable) -> {
            if (throwable != null) {
                return;
            }

            Bukkit.getScheduler().runTask(this.plugin, () -> {
                inventory.setItemInMainHand(newItem);

                sender.sendMessage(this.configManager.getMessage("applied"));
            });
        });

    }
}
