package dev.veyno.aiFoliaChunkLoader;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;

public final class KeepRegionCommand implements CommandExecutor, TabCompleter {
    private static final String PREFIX = ChatColor.AQUA + "[KeepRegion] " + ChatColor.GRAY;

    private final AiFoliaChunkLoader plugin;
    private final RegionManager manager;

    public KeepRegionCommand(AiFoliaChunkLoader plugin, RegionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "Nur Spieler können diesen Befehl nutzen.");
            return true;
        }
        if (!sender.hasPermission("keepregion.use")) {
            sender.sendMessage(PREFIX + "Keine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "list" -> handleList(player);
            case "remove" -> handleRemove(player, args);
            default -> sendUsage(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PREFIX + "Usage: /keepregion create <radius>");
            return;
        }
        int radius;
        try {
            radius = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(PREFIX + "Radius muss eine ganze Zahl sein.");
            return;
        }
        int maxRadius = plugin.getConfig().getInt("limits.max-radius", 16);
        int maxRadiusAdmin = plugin.getConfig().getInt("limits.max-radius-admin", maxRadius);
        int allowedMax = player.hasPermission("keepregion.admin") ? maxRadiusAdmin : maxRadius;
        if (radius < 0 || radius > allowedMax) {
            player.sendMessage(PREFIX + "Radius muss zwischen 0 und " + allowedMax + " liegen.");
            return;
        }
        int maxRegions = plugin.getConfig().getInt("limits.max-regions-total", 0);
        if (maxRegions > 0 && manager.totalRegions() >= maxRegions && !player.hasPermission("keepregion.admin")) {
            player.sendMessage(PREFIX + "Maximale Anzahl an Regionen erreicht.");
            return;
        }
        int centerX = Math.floorDiv(player.getLocation().getBlockX(), 16);
        int centerZ = Math.floorDiv(player.getLocation().getBlockZ(), 16);
        World world = player.getWorld();
        KeepRegion region = new KeepRegion(UUID.randomUUID(), world.getName(), world.getUID(), centerX, centerZ, radius,
                player.getName(), Instant.now());
        if (manager.hasRegion(region)) {
            player.sendMessage(PREFIX + "Diese Region existiert bereits.");
            return;
        }
        manager.addRegion(region);
        player.sendMessage(PREFIX + "Region erstellt: center=(" + centerX + "," + centerZ + ") r=" + radius
                + " chunks=" + chunkCount(radius));
    }

    private void handleList(Player player) {
        List<KeepRegion> regions = manager.getRegions();
        if (regions.isEmpty()) {
            player.sendMessage(PREFIX + "Keine Regionen gespeichert.");
            return;
        }
        player.sendMessage(PREFIX + "Gespeicherte Regionen (nach Welt gruppiert):");
        World playerWorld = player.getWorld();
        int playerChunkX = Math.floorDiv(player.getLocation().getBlockX(), 16);
        int playerChunkZ = Math.floorDiv(player.getLocation().getBlockZ(), 16);
        var grouped = new java.util.LinkedHashMap<String, List<KeepRegion>>();
        for (KeepRegion region : regions) {
            grouped.computeIfAbsent(region.getWorldName(), key -> new ArrayList<>()).add(region);
        }
        for (var entry : grouped.entrySet()) {
            player.sendMessage(ChatColor.DARK_AQUA + "Welt: " + entry.getKey());
            for (KeepRegion region : entry.getValue()) {
                StringBuilder line = new StringBuilder();
                line.append(ChatColor.YELLOW).append(region.shortId())
                        .append(ChatColor.GRAY).append(" center=(").append(region.getCenterX()).append(",")
                        .append(region.getCenterZ()).append(")")
                        .append(" r=").append(region.getRadius())
                        .append(" chunks=").append(chunkCount(region.getRadius()));
                if (playerWorld.getName().equalsIgnoreCase(region.getWorldName())) {
                    int dist = Math.max(Math.abs(playerChunkX - region.getCenterX()), Math.abs(playerChunkZ - region.getCenterZ()));
                    line.append(" dist=").append(dist).append(" chunks");
                }
                player.sendMessage(line.toString());
            }
        }
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length >= 2) {
            Optional<KeepRegion> removed = manager.removeById(args[1]);
            if (removed.isEmpty()) {
                player.sendMessage(PREFIX + "Keine eindeutige Region mit dieser ID gefunden.");
                return;
            }
            KeepRegion region = removed.get();
            player.sendMessage(PREFIX + "Region entfernt: " + region.shortId() + " center=(" + region.getCenterX() + ","
                    + region.getCenterZ() + ") r=" + region.getRadius());
            return;
        }
        int centerX = Math.floorDiv(player.getLocation().getBlockX(), 16);
        int centerZ = Math.floorDiv(player.getLocation().getBlockZ(), 16);
        List<KeepRegion> removed = manager.removeByCenter(player.getWorld(), centerX, centerZ);
        if (removed.isEmpty()) {
            player.sendMessage(PREFIX + "Keine Region mit diesem Mittelpunkt gefunden.");
            return;
        }
        if (removed.size() == 1) {
            KeepRegion region = removed.getFirst();
            player.sendMessage(PREFIX + "Region entfernt: " + region.shortId() + " center=(" + region.getCenterX() + ","
                    + region.getCenterZ() + ") r=" + region.getRadius());
            return;
        }
        player.sendMessage(PREFIX + removed.size() + " Regionen mit diesem Mittelpunkt entfernt.");
    }

    private void sendUsage(Player player) {
        player.sendMessage(PREFIX + "Usage: /keepregion <create|list|remove>");
    }

    private int chunkCount(int radius) {
        int side = radius * 2 + 1;
        return side * side;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("create", "list", "remove"));
        }
        if (args.length == 2 && "remove".equalsIgnoreCase(args[0])) {
            List<String> ids = new ArrayList<>();
            for (KeepRegion region : manager.getRegions()) {
                ids.add(region.shortId());
            }
            return filter(args[1], ids);
        }
        return Collections.emptyList();
    }

    private List<String> filter(String input, List<String> options) {
        if (input == null || input.isBlank()) {
            return options;
        }
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                results.add(option);
            }
        }
        return results;
    }
}
