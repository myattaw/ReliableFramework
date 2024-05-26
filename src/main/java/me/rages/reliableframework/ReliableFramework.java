package me.rages.reliableframework;

import lombok.SneakyThrows;
import me.rages.reliableframework.storage.SQLStorage;
import me.rages.reliableframework.storage.impl.SQLiteStorage;
import me.rages.reliableframework.data.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.*;

public class ReliableFramework extends JavaPlugin implements Listener {

    private SQLStorage storage;

    @Override
    @SneakyThrows
    public void onEnable() {
        saveDefaultConfig();
        this.storage = new SQLiteStorage<>(this).connect();
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
    public void onJoin(PlayerJoinEvent event) throws SQLException {
        Player player = event.getPlayer();

        // Load the user from the database
        User user = (User) storage.load(Map.entry("uuid", player.getUniqueId()), User.class);

        // If the user doesn't exist, create a new one
        if (user == null) {
            user = new User(storage);
            user.setUuid(player.getUniqueId().toString());
            user.setName(player.getName());
            System.out.println(String.format("Created the user: [%s, %s]", user.getName(), user.getUuid()));
        } else {
            System.out.println(String.format("Loaded the user: [%s, %s]", user.getName(), user.getUuid()));
        }

        // Get the current join count, increment it, and save it back
        int totalJoins = user.get("join_count", Integer.class).orElse(0) + 1;
        user.set("join_count", totalJoins);
        System.out.println(String.format("%s has joined the server %d times.", user.getName(), totalJoins));
        storage.save(user);
    }


}
