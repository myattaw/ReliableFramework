package me.rages.reliableframework.storage.impl;

import me.rages.reliableframework.storage.SQLStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteStorage<T extends JavaPlugin> extends SQLStorage<T> {

    public SQLiteStorage(T plugin) {
        super(plugin);
    }

    @Override
    public Connection connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return connection;
        }
        final File file = new File(plugin.getDataFolder(), "data.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        return connection;
    }

    @Override
    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void setupTables() throws SQLException {
        // Implement table setup logic here
    }

    @Override
    public void addColumn(String tableName, String columnDefinition) throws SQLException {
        // Implement adding column logic here
    }

}
