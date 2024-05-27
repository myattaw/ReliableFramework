package me.rages.reliableframework.storage.impl;

import me.rages.reliableframework.data.DataObject;
import me.rages.reliableframework.data.User;
import me.rages.reliableframework.storage.SQLStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteStorage<T extends JavaPlugin, D extends DataObject> extends SQLStorage<T, D> {

    public SQLiteStorage(T plugin) {
        super(plugin);
    }

    @Override
    public SQLiteStorage<T, D> connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return this;
        }
        File file = new File(plugin.getDataFolder(), "data.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

        try (Statement stmt = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid TEXT NOT NULL UNIQUE," +
                    "name TEXT NOT NULL)";
            stmt.execute(sql);
        }
        return this;
    }

    @Override
    protected String getTableName(Class<D> clazz) {
        if (clazz.equals(User.class)) {
            return "users";
        }
        throw new IllegalArgumentException("Unknown data object class: " + clazz.getName());
    }

}

