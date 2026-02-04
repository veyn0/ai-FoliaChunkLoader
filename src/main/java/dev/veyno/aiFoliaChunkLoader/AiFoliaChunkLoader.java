package dev.veyno.aiFoliaChunkLoader;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AiFoliaChunkLoader extends JavaPlugin {
    private RegionManager regionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        RegionStore store = new RegionStore(this);
        regionManager = new RegionManager(this, store);
        regionManager.loadRegions();
        regionManager.applyAllTickets();

        PluginCommand command = getCommand("keepregion");
        if (command != null) {
            KeepRegionCommand handler = new KeepRegionCommand(this, regionManager);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        } else {
            getLogger().warning("Command keepregion not defined in plugin.yml");
        }
    }

    @Override
    public void onDisable() {
        if (regionManager != null) {
            regionManager.removeAllTickets();
        }
    }
}
