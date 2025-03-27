package me.yourname.shrinkingborder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ShrinkingBorder extends JavaPlugin {

    private FileConfiguration config;
    private World world;
    private WorldBorder border;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        world = Bukkit.getWorlds().get(0);
        border = world.getWorldBorder();
        getLogger().info("ShrinkingBorder enabled.");

        startTeleportOutsidePlayersTask();
        // Optional: auto-start shrinking
        // startGradualShrinking();
    }

    @Override
    public void onDisable() {
        getLogger().info("ShrinkingBorder disabled.");
    }

    public void startGradualShrinking() {
        double centerX = config.getDouble("border.centerX");
        double centerZ = config.getDouble("border.centerZ");
        double initialSize = config.getDouble("border.initialSize");
        double finalSize = config.getDouble("border.finalSize");
        long intervalTicks = config.getLong("border.shrinkIntervalTicks");
        double shrinkPerStep = config.getDouble("border.shrinkAmountPerStep");

        border.setCenter(centerX, centerZ);
        border.setSize(initialSize);

        new BukkitRunnable() {
            @Override
            public void run() {
                double currentSize = border.getSize();
                if (currentSize <= finalSize) {
                    getLogger().info("World border has reached final size.");
                    cancel();
                    return;
                }

                double newSize = Math.max(currentSize - shrinkPerStep, finalSize);
                border.setSize(newSize);
                getLogger().info("World border shrunk to: " + newSize);

                if (config.getBoolean("messages.enabled")) {
                    String msg = config.getString("messages.shrinkBroadcast")
                                    .replace("%size%", String.format("%.1f", newSize));
                    Bukkit.broadcastMessage(msg);

                    if (config.getBoolean("sounds.enabled")) {
                        String sound = config.getString("sounds.shrinkSound");
                        float volume = (float) config.getDouble("sounds.volume");
                        float pitch = (float) config.getDouble("sounds.pitch");
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.playSound(player.getLocation(), sound, volume, pitch);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, intervalTicks);
    }

    public void startTeleportOutsidePlayersTask() {
        if (!config.getBoolean("teleportOutsidePlayersInside")) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Location loc = player.getLocation();
                    double borderSize = border.getSize() / 2;
                    double centerX = border.getCenter().getX();
                    double centerZ = border.getCenter().getZ();

                    double dx = loc.getX() - centerX;
                    double dz = loc.getZ() - centerZ;

                    if (Math.abs(dx) > borderSize || Math.abs(dz) > borderSize) {
                        double clampedX = Math.max(Math.min(loc.getX(), centerX + borderSize - 1), centerX - borderSize + 1);
                        double clampedZ = Math.max(Math.min(loc.getZ(), centerZ + borderSize - 1), centerZ - borderSize + 1);
                        Location safeLoc = new Location(loc.getWorld(), clampedX, loc.getY(), clampedZ);
                        player.teleport(safeLoc);
                        player.sendMessage("⚠️ You were outside the world border and have been teleported inside.");
                    }
                }
            }
        }.runTaskTimer(this, 100L, 600L); // Every 30 seconds after 5 sec delay
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shrinkborder")) {
            startGradualShrinking();
            sender.sendMessage("World border gradual shrinking started!");
            return true;
        }
        return false;
    }
}
