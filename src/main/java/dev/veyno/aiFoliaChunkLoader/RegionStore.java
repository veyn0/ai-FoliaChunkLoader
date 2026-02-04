package dev.veyno.aiFoliaChunkLoader;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class RegionStore {
    private static final String REGIONS_KEY = "regions";

    private final JavaPlugin plugin;
    private final File regionsFile;

    public RegionStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.regionsFile = new File(plugin.getDataFolder(), "regions.yml");
    }

    public List<KeepRegion> load() {
        ensureFile();
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(regionsFile);
        List<KeepRegion> regions = new ArrayList<>();
        List<?> rawList = configuration.getList(REGIONS_KEY);
        if (rawList == null) {
            return regions;
        }
        for (int i = 0; i < rawList.size(); i++) {
            Object entry = rawList.get(i);
            if (!(entry instanceof Map<?, ?> map)) {
                plugin.getLogger().warning("Invalid region entry at index " + i + " in regions.yml");
                continue;
            }
            try {
                Object idRaw = map.get("id");
                UUID id = UUID.fromString(idRaw == null ? "\"\"" : idRaw.toString());
                String worldName = valueOf(map.get("world"));
                if (worldName == null || worldName.isEmpty()) {
                    plugin.getLogger().warning("Region entry missing world at index " + i);
                    continue;
                }
                UUID worldUuid = null;
                String worldUuidRaw = valueOf(map.get("worldUuid"));
                if (worldUuidRaw != null && !worldUuidRaw.isEmpty()) {
                    worldUuid = UUID.fromString(worldUuidRaw);
                }
                int centerX = intValue(map.get("centerX"));
                int centerZ = intValue(map.get("centerZ"));
                int radius = intValue(map.get("radius"));
                String createdBy = valueOf(map.get("createdBy"));
                Instant createdAt = null;
                String createdAtRaw = valueOf(map.get("createdAt"));
                if (createdAtRaw != null && !createdAtRaw.isEmpty()) {
                    createdAt = Instant.parse(createdAtRaw);
                }
                regions.add(new KeepRegion(id, worldName, worldUuid, centerX, centerZ, radius, createdBy, createdAt));
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to load region entry at index " + i, ex);
            }
        }
        return regions;
    }

    public void save(List<KeepRegion> regions) {
        ensureFile();
        YamlConfiguration configuration = new YamlConfiguration();
        List<Map<String, Object>> entries = new ArrayList<>();
        for (KeepRegion region : regions) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", region.getId().toString());
            entry.put("world", region.getWorldName());
            if (region.getWorldUuid() != null) {
                entry.put("worldUuid", region.getWorldUuid().toString());
            }
            entry.put("centerX", region.getCenterX());
            entry.put("centerZ", region.getCenterZ());
            entry.put("radius", region.getRadius());
            entry.put("createdBy", region.getCreatedBy());
            if (region.getCreatedAt() != null) {
                entry.put("createdAt", region.getCreatedAt().toString());
            }
            entries.add(entry);
        }
        configuration.set(REGIONS_KEY, entries);
        try {
            configuration.save(regionsFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save regions.yml", ex);
        }
    }

    private void ensureFile() {
        if (!regionsFile.exists()) {
            if (!regionsFile.getParentFile().exists() && !regionsFile.getParentFile().mkdirs()) {
                plugin.getLogger().warning("Failed to create plugin data folder");
            }
            try {
                if (!regionsFile.createNewFile()) {
                    plugin.getLogger().warning("Failed to create regions.yml");
                }
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create regions.yml", ex);
            }
        }
    }

    private String valueOf(Object raw) {
        if (raw == null) {
            return null;
        }
        return raw.toString();
    }

    private int intValue(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw != null) {
            try {
                return Integer.parseInt(raw.toString());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
