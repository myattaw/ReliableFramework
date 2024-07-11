package me.rages.reliableframework;

import com.sk89q.worldedit.math.BlockVector3;
import lombok.SneakyThrows;
import me.rages.reliableframework.data.Entity;
import me.rages.reliableframework.data.ReliableUser;
import me.rages.reliableframework.pluginservice.ServiceManager;
import me.rages.reliableframework.pluginservice.impl.worldedit.FAWEService;
import me.rages.reliableframework.storage.SQLStorage;
import me.rages.reliableframework.storage.impl.MySQLStorage;
import me.rages.reliableframework.storage.impl.SQLiteStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ReliableFramework extends JavaPlugin implements Listener {

    private SQLStorage storage;

    @Override
    @SneakyThrows
    public void onEnable() {
        saveDefaultConfig();
        //TODO: Read configuration file later to determine what storage system to use.
        // Possibly remove 'Class<? extends DataObject>... dataObjectClasses' and use reflections.

        ServiceManager serviceManager = ServiceManager.createServiceManager(this)
                .registerService(new FAWEService());

        FAWEService faweService = serviceManager.getService(FAWEService.class);

        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            File file = new File(getDataFolder(), "test.schem");

            Runtime runtime = Runtime.getRuntime();

            // Garbage collection to get more accurate memory usage
            runtime.gc();

            long startMemory = runtime.totalMemory() - runtime.freeMemory();
            long startTime = System.currentTimeMillis();

            Map<BlockVector3, BlockData> result = faweService.readSchematic(file);

            long endTime = System.currentTimeMillis();
            long endMemory = runtime.totalMemory() - runtime.freeMemory();
            long duration = endTime - startTime;
            long memoryUsed = endMemory - startMemory;

            System.out.println("Number of elements: " + result.size());
            System.out.println("Method execution time: " + duration + " ms");
            System.out.println("Memory used: " + memoryUsed + " bytes");
        }, 20L * 5);

        String dbType = getConfig().getString("storage.type", "SQLite");
        if (dbType.equals("SQLite")) {
            this.storage = new SQLiteStorage(this, ReliableUser.class).connect();
        } else if (dbType.equals("MySQL")) {
            this.storage = new MySQLStorage(this, ReliableUser.class).connect();
        }
        getServer().getPluginManager().registerEvents(this, this);
    }


    @Override
    public void onDisable() {
        try {
            storage.disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @EventHandler
    public void onPluginLoad(PluginEnableEvent event) {
        if (event.getPlugin().equals(this)) {
            storage.loadAll(ReliableUser.class)
                    .thenComposeAsync(users -> {
                        Bukkit.getConsoleSender().sendMessage(
                                ChatColor.RED + "Loaded Framework Users: " + users.size()
                        );
                        return CompletableFuture.completedFuture(users);
                    })
                    .exceptionally(ex -> {
                        Bukkit.getConsoleSender().sendMessage(
                                ChatColor.RED + "Failed to load users: " + ex.getMessage()
                        );
                        ex.printStackTrace();
                        return null;
                    });
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Load the user from the database asynchronously
        storage.load(Entity.of("uuid", player.getUniqueId()), ReliableUser.class)
                .thenApplyAsync(user -> {
                    if (user == null) {
                        // Create a new user if it doesn't exist
                        user = new ReliableUser(storage);
                        user.setUuid(player.getUniqueId());
                        user.setName(player.getName());
                        Bukkit.getConsoleSender().sendMessage(
                                ChatColor.YELLOW +
                                        String.format("Created the user: [%s, %s]",
                                        user.getName(), user.getUuid())
                        );
                    } else {
                        Bukkit.getConsoleSender().sendMessage(
                                ChatColor.GOLD +
                                        String.format("Loaded the user: [%s, %s]",
                                        user.getName(), user.getUuid())
                        );
                    }
                    return user;
                })
                .thenComposeAsync(user -> {
                    // Get the current join count, increment it, and save it back
                    int totalJoins = user.get("join_count", Integer.class).orElse(0) + 1;
                    user.set("join_count", totalJoins);

                    Bukkit.getConsoleSender().sendMessage(
                            ChatColor.BLUE +
                                    String.format("%s has joined the server %d times.",
                                    user.getName(), totalJoins)
                    );

                    // Save the user asynchronously
                    return storage.save(user).thenApplyAsync(result -> {
                        // Additional actions after saving, if necessary
                        Bukkit.getConsoleSender().sendMessage(
                                ChatColor.GREEN +
                                        String.format("User [%s, %s] saved successfully.",
                                        user.getName(), user.getUuid())
                        );
                        return result;
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

}
