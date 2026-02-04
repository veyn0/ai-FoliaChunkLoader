package dev.veyno.aiFoliaChunkLoader;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class RegionManager {
    private final JavaPlugin plugin;
    private final RegionStore store;
    private final List<KeepRegion> regions = new ArrayList<>();

    public RegionManager(JavaPlugin plugin, RegionStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void loadRegions() {
        regions.clear();
        regions.addAll(store.load());
    }

    public List<KeepRegion> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    public boolean hasRegion(KeepRegion candidate) {
        return regions.stream().anyMatch(region -> region.sameKey(candidate));
    }

    public void addRegion(KeepRegion region) {
        regions.add(region);
        store.save(regions);
        applyTickets(region, true);
    }

    public List<KeepRegion> removeByCenter(World world, int centerX, int centerZ) {
        List<KeepRegion> removed = new ArrayList<>();
        regions.removeIf(region -> {
            if (!worldMatches(region, world)) {
                return false;
            }
            if (region.getCenterX() == centerX && region.getCenterZ() == centerZ) {
                removed.add(region);
                return true;
            }
            return false;
        });
        if (!removed.isEmpty()) {
            store.save(regions);
            removed.forEach(region -> applyTickets(region, false));
        }
        return removed;
    }

    public Optional<KeepRegion> removeById(String idInput) {
        if (idInput == null || idInput.isBlank()) {
            return Optional.empty();
        }
        KeepRegion match = null;
        for (KeepRegion region : regions) {
            String id = region.getId().toString();
            String normalized = idInput.toLowerCase();
            if (id.equalsIgnoreCase(idInput) || id.toLowerCase().startsWith(normalized)) {
                if (match != null) {
                    return Optional.empty();
                }
                match = region;
            }
        }
        if (match == null) {
            return Optional.empty();
        }
        regions.remove(match);
        store.save(regions);
        applyTickets(match, false);
        return Optional.of(match);
    }

    public int totalRegions() {
        return regions.size();
    }

    public void applyAllTickets() {
        for (KeepRegion region : regions) {
            applyTickets(region, true);
        }
    }

    public void removeAllTickets() {
        for (KeepRegion region : regions) {
            applyTickets(region, false);
        }
    }

    private void applyTickets(KeepRegion region, boolean add) {
        World world = resolveWorld(region);
        if (world == null) {
            plugin.getLogger().warning("World not found for region " + region.getId() + " (" + region.getWorldName() + ")");
            return;
        }
        int radius = region.getRadius();
        for (int x = region.getCenterX() - radius; x <= region.getCenterX() + radius; x++) {
            for (int z = region.getCenterZ() - radius; z <= region.getCenterZ() + radius; z++) {
                int chunkX = x;
                int chunkZ = z;
                runOnChunkRegion(world, chunkX, chunkZ, () -> {
                    try {
                        if (add) {
                            world.getChunkAt(chunkX, chunkZ).addPluginChunkTicket(plugin);
                        } else {
                            world.getChunkAt(chunkX, chunkZ).removePluginChunkTicket(plugin);
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().log(Level.WARNING, "Failed to " + (add ? "add" : "remove") + " ticket for chunk "
                                + chunkX + "," + chunkZ + " in world " + world.getName(), ex);
                    }
                });
            }
        }
    }

    private void runOnChunkRegion(World world, int chunkX, int chunkZ, Runnable action) {
        Bukkit.getServer().getRegionScheduler().execute(plugin, world, chunkX, chunkZ, action);
    }

    private World resolveWorld(KeepRegion region) {
        UUID worldUuid = region.getWorldUuid();
        if (worldUuid != null) {
            World world = Bukkit.getWorld(worldUuid);
            if (world != null) {
                return world;
            }
        }
        return Bukkit.getWorld(region.getWorldName());
    }

    private boolean worldMatches(KeepRegion region, World world) {
        if (region.getWorldUuid() != null) {
            return region.getWorldUuid().equals(world.getUID());
        }
        return region.getWorldName().equalsIgnoreCase(world.getName());
    }
}
