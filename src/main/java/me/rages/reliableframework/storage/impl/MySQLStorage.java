package me.rages.reliableframework.storage.impl;

import me.rages.reliableframework.data.DataObject;
import me.rages.reliableframework.storage.SQLStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class MySQLStorage extends SQLStorage {

    @SafeVarargs
    public MySQLStorage(JavaPlugin plugin, Class<? extends DataObject>... dataObjectClasses) {
        super(plugin, dataObjectClasses);
    }

    @Override
    public MySQLStorage connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return this;
        }

        String address = plugin.getConfig().getString("storage.url.address");
        String database = plugin.getConfig().getString("storage.url.database");
        String username = plugin.getConfig().getString("storage.url.username");
        String password = plugin.getConfig().getString("storage.url.password");

        String url = "jdbc:mysql://" + address + "/" + database + "?useSSL=false&serverTimezone=UTC";
        connection = DriverManager.getConnection(url, username, password);

        if (dataObjectClasses.length > 0) {
            createTablesForDataObjects(this.dataObjectClasses);
        }

        return this;
    }

    @Override
    public boolean columnExists(String tableName, String columnName) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM information_schema.columns WHERE table_name = ? AND column_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        }
        return false;
    }

    @Override
    public String getColumnType(Class<?> type) {
        if (type == Integer.class) {
            return "INT";
        } else if (type == Long.class) {
            return "BIGINT";
        } else if (type == String.class) {
            return "VARCHAR(255)";
        } else if (type == Boolean.class) {
            return "TINYINT(1)";
        } else if (type == Double.class) {
            return "DOUBLE";
        } else if (type == Float.class) {
            return "FLOAT";
        } else if (type == java.util.Date.class) {
            return "DATETIME";
        } else if (type == byte[].class) {
            return "BLOB";
        } else if (type == UUID.class) {
            return "CHAR(36)"; // UUIDs stored as CHAR(36)
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + type.getName());
        }
    }

}
