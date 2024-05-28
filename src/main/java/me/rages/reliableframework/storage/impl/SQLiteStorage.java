package me.rages.reliableframework.storage.impl;

import me.rages.reliableframework.data.DataObject;
import me.rages.reliableframework.data.User;
import me.rages.reliableframework.storage.SQLStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteStorage extends SQLStorage {

    @SafeVarargs
    public SQLiteStorage(JavaPlugin plugin, Class<? extends DataObject>... dataObjectClasses) {
        super(plugin, dataObjectClasses);
    }

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
