package sbs.jeflo;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    private final JavaPlugin plugin;
    private final Map<String, MessageEntry> messages = new HashMap<>();

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.isConfigurationSection("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                boolean enabled = config.getBoolean("messages." + key + ".enabled", true);
                String text = config.getString("messages." + key + ".text", "");
                messages.put(key, new MessageEntry(enabled, ChatColor.translateAlternateColorCodes('&', text)));
            }
        }
    }

    public void reload() {
        messages.clear();
        loadMessages();
    }

    public void send(String key, org.bukkit.command.CommandSender player) {
        MessageEntry entry = messages.get(key);
        if (entry != null && entry.enabled && !entry.text.isEmpty()) {
            player.sendMessage(entry.text);
        }
    }

    public boolean isEnabled(String key) {
        MessageEntry entry = messages.get(key);
        return entry != null && entry.enabled;
    }

    private static class MessageEntry {
        boolean enabled;
        String text;
        MessageEntry(boolean enabled, String text) {
            this.enabled = enabled;
            this.text = text;
        }
    }
}
