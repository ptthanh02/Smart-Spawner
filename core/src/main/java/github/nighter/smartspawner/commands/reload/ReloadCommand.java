package github.nighter.smartspawner.commands.reload;

import github.nighter.smartspawner.spawner.lootgen.SpawnerLootGenerator;
import github.nighter.smartspawner.language.LanguageManager;
import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.config.ConfigManager;
import github.nighter.smartspawner.spawner.properties.SpawnerManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {
    private final SmartSpawner plugin;
    private final SpawnerManager spawnerManager;
    private final ConfigManager configManager;
    private final LanguageManager languageManager;
    private final SpawnerLootGenerator spawnerLootGenerator;

    public ReloadCommand(SmartSpawner plugin) {
        this.plugin = plugin;
        this.spawnerManager = plugin.getSpawnerManager();
        this.spawnerLootGenerator = plugin.getSpawnerLootGenerator();
        this.configManager = plugin.getConfigManager();
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smartspawner.reload")) {
            sender.sendMessage(languageManager.getMessageWithPrefix("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(languageManager.getMessageWithPrefix("command.reload.usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (args.length == 1) {
                reloadAll(sender);
            } else {
                sender.sendMessage(languageManager.getMessageWithPrefix("command.reload.usage"));
            }
            return true;
        }

        return false;
    }

    private void reloadAll(CommandSender sender) {
        try {
            sender.sendMessage(languageManager.getMessageWithPrefix("command.reload.wait"));
            // Reload all configurations
            configManager.reloadConfigs();

            // Reload Hopper feature
            plugin.setUpHopperHandler();

            // Reload loot tables configuration
            spawnerLootGenerator.loadConfigurations();

            // Reload language files
            languageManager.reload();

            // Refresh all holograms for configured changes
            spawnerManager.reloadAllHolograms();

            sender.sendMessage(languageManager.getMessageWithPrefix("command.reload.success"));
            configManager.debug("Plugin reloaded successfully by " + sender.getName());
        } catch (Exception e) {
            sender.sendMessage(languageManager.getMessageWithPrefix("command.reload.error"));
            plugin.getLogger().severe("Error reloading plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("smartspawner.reload")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Collections.singletonList("reload");
        }

        return Collections.emptyList();
    }
}
