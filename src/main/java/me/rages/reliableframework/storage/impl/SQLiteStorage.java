package me.rages.reliableframework.storage.impl;

import me.rages.reliableframework.storage.SQLStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteStorage<T extends JavaPlugin> extends SQLStorage<T> {

    public SQLiteStorage(T plugin) {
        super(plugin);
    }

    @Override
    public SQLiteStorage<? extends JavaPlugin> connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return null;
        }
        File file = new File(plugin.getDataFolder(), "data.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

        try (Statement statement = connection.createStatement()) {
            // Example table creation, modify as needed
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid TEXT NOT NULL UNIQUE," +
                    "name TEXT NOT NULL)";
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
