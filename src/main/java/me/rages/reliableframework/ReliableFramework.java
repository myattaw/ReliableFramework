package me.rages.reliableframework;

import lombok.SneakyThrows;
import me.rages.reliableframework.storage.SQLStorage;
import me.rages.reliableframework.storage.impl.SQLiteStorage;
import me.rages.reliableframework.data.User;
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
        UUID playerUUID = event.getPlayer().getUniqueId();

        // Load the user from the database
        User user = (User) storage.load(playerUUID, User.class);

        // If the user doesn't exist, create a new one
        if (user == null) {
            user = (User) storage.create(playerUUID, User.class);
        }

        // Get the current join count, increment it, and save it back
        int totalJoins = user.get("join_count", Integer.class).orElse(0);
        user.set("join_count", totalJoins + 1);
        storage.save(user);
    }


}
