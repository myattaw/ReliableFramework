package me.rages.reliableframework;

import lombok.SneakyThrows;
import me.rages.reliableframework.data.User;
import me.rages.reliableframework.storage.SQLStorage;
import me.rages.reliableframework.storage.impl.SQLiteStorage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Map;

public class ReliableFramework extends JavaPlugin implements Listener {

    private SQLStorage storage;

    @Override
    @SneakyThrows
    public void onEnable() {
        saveDefaultConfig();
        //TODO: Read configuration file later to determine what storage system to use.
        // Possibly remove 'Class<? extends DataObject>... dataObjectClasses' and use reflections.
        this.storage = new SQLiteStorage(this, User.class).connect();
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
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load the user from the database asynchronously
        storage.load(Map.entry("uuid", player.getUniqueId()), User.class)
                .thenApplyAsync(user -> {
                    if (user == null) {
                        // Create a new user if it doesn't exist
                        user = new User(storage);
                        user.setUuid(player.getUniqueId().toString());
                        user.setName(player.getName());
                        System.out.println(String.format("Created the user: [%s, %s]", user.getName(), user.getUuid()));
                    } else {
                        System.out.println(String.format("Loaded the user: [%s, %s]", user.getName(), user.getUuid()));
                    }
                    return user;
                }).thenComposeAsync(user -> {
                    // Get the current join count, increment it, and save it back
                    int totalJoins = user.get("join_count", Integer.class).orElse(0) + 1;
                    user.set("join_count", totalJoins);
                    System.out.println(String.format("%s has joined the server %d times.", user.getName(), totalJoins));

                    // Save the user asynchronously
                    return storage.save(user).thenApplyAsync(result -> {
                        // Additional actions after saving, if necessary
                        System.out.println(String.format("User [%s, %s] saved successfully.", user.getName(), user.getUuid()));
                        return result;
                    });
                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

}
