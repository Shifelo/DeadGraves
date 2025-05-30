package sbs.jeflo;

import org.bukkit.plugin.java.JavaPlugin;
import sbs.jeflo.listeners.GraveListener;

public class DeadGraves extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("DeadGraves has been enabled!");
        getServer().getPluginManager().registerEvents(new GraveListener(this), this);
    }
    @Override
    public void onDisable() {
        getLogger().info("DeadGraves has been disabled!");
    }
}
