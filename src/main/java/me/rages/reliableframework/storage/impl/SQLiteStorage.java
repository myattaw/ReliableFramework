package me.rages.reliableframework.storage.impl;

import me.rages.reliableframework.data.DataObject;
import me.rages.reliableframework.storage.SQLStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SQLiteStorage is an implementation of SQLStorage that provides
 * functionality for connecting to and interacting with an SQLite database.
 */
public class SQLiteStorage extends SQLStorage {

    @SafeVarargs
    public SQLiteStorage(JavaPlugin plugin, Class<? extends DataObject>... dataObjectClasses) {
        super(plugin, dataObjectClasses);
    }

    @Override
    public SQLiteStorage connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return this;
        }
        File file = new File(plugin.getDataFolder(), "data.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

        if (dataObjectClasses.length > 0) {
            createTablesForDataObjects(this.dataObjectClasses);
        }

        return this;
    }

    @Override
    public boolean columnExists(String tableName, String columnName) throws SQLException {
        String query = "PRAGMA table_info(" + tableName + ")";
        try (PreparedStatement ps = connection.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (rs.getString("name").equalsIgnoreCase(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
