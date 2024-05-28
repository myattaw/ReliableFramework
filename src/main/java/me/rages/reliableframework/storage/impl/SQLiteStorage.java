package me.rages.reliableframework.storage.impl;

import me.rages.reliableframework.data.DataObject;
import me.rages.reliableframework.storage.SQLStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SQLiteStorage is an implementation of SQLStorage that provides
 * functionality for connecting to and interacting with an SQLite database.
 */
public class SQLiteStorage extends SQLStorage {

    /**
     * Constructs a new SQLiteStorage instance.
     *
     * @param plugin            the JavaPlugin instance
     * @param dataObjectClasses the classes of the data objects to be managed
     */
    @SafeVarargs
    public SQLiteStorage(JavaPlugin plugin, Class<? extends DataObject>... dataObjectClasses) {
        super(plugin, dataObjectClasses);
    }

    /**
     * Connects to the SQLite database. If the connection is already open,
     * it will return the current instance. If not, it will establish a new
     * connection to the SQLite database file located in the plugin's data folder.
     * It will also automatically create tables for all specified DataObject classes.
     *
     * @return the SQLiteStorage instance
     * @throws SQLException if a database access error occurs
     */
    @Override
    public final SQLiteStorage connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return this;
        }
        File file = new File(plugin.getDataFolder(), "data.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        // Automatically create tables for all DataObject classes
        if (dataObjectClasses.length > 0) {
            createTablesForDataObjects(this.dataObjectClasses);
        }

        return this;
    }

}
