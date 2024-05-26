package me.rages.reliableframework.storage;

import me.rages.reliableframework.data.DataObject;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class SQLStorage<T extends JavaPlugin, D extends DataObject> implements Database<D> {
    protected final T plugin;
    protected Connection connection;

    public SQLStorage(T plugin) {
        this.plugin = plugin;
    }

    @Override
    public abstract SQLStorage<T, D> connect() throws SQLException;

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
    public void addColumn(String tableName, String columnDefinition) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnDefinition;
            stmt.execute(sql);
        }
    }

    @Override
    public boolean columnExists(String tableName, String columnName) throws SQLException {
        String query = "PRAGMA table_info(" + tableName + ")";
        try (ResultSet rs = query(query)) {
            while (rs.next()) {
                if (rs.getString("name").equalsIgnoreCase(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void insert(String tableName, Map<String, Object> data) throws SQLException {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (String column : data.keySet()) {
            columns.append(column).append(",");
            values.append("?,");
        }
        columns.deleteCharAt(columns.length() - 1);
        values.deleteCharAt(values.length() - 1);

        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object value : data.values()) {
                preparedStatement.setObject(index++, value);
            }
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public ResultSet query(String query, Object... params) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            preparedStatement.setObject(i + 1, params[i]);
        }
        return preparedStatement.executeQuery();
    }

    @Override
    public void update(String tableName, Map<String, Object> data, String whereClause, Object... whereParams) throws SQLException {
        StringBuilder setClause = new StringBuilder();
        for (String column : data.keySet()) {
            setClause.append(column).append(" = ?,");
        }
        setClause.deleteCharAt(setClause.length() - 1);

        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object value : data.values()) {
                pstmt.setObject(index++, value);
            }
            for (Object param : whereParams) {
                pstmt.setObject(index++, param);
            }
            pstmt.executeUpdate();
        }
    }

    @Override
    public void delete(String tableName, String whereClause, Object... whereParams) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE " + whereClause;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < whereParams.length; i++) {
                pstmt.setObject(i + 1, whereParams[i]);
            }
            pstmt.executeUpdate();
        }
    }

    @Override
    public void createTable(String tableName, Map<String, String> columns) throws SQLException {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
        for (Map.Entry<String, String> column : columns.entrySet()) {
            sql.append(column.getKey()).append(" ").append(column.getValue()).append(",");
        }
        sql.deleteCharAt(sql.length() - 1);  // Remove the trailing comma
        sql.append(")");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql.toString());
        }
    }

    public void ensureColumnExists(String tableName, String columnName, Object value) throws SQLException {
        if (!columnExists(tableName, columnName)) {
            String columnType = getColumnType(value);
            addColumn(tableName, columnName + " " + columnType);
        }
    }

    private String getColumnType(Object value) {
        if (value instanceof Integer) {
            return "INTEGER DEFAULT 0";
        } else if (value instanceof Long) {
            return "INTEGER DEFAULT 0"; // SQLite uses INTEGER for both int and long
        } else if (value instanceof String) {
            return "TEXT";
        } else if (value instanceof Boolean) {
            return "BOOLEAN DEFAULT FALSE";
        } else if (value instanceof Double || value instanceof Float) {
            return "REAL";
        } else if (value instanceof java.util.Date) {
            return "INTEGER DEFAULT 0"; // Store dates as INTEGER (epoch time) for SQLite compatibility
        } else if (value instanceof byte[]) {
            return "BLOB";
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + value.getClass().getName());
        }
    }

    @Override
    public D load(UUID uuid, Class<D> clazz) throws SQLException {
        ResultSet rs = query("SELECT * FROM " + getTableName(clazz) + " WHERE uuid = ?", uuid.toString());
        if (rs.next()) {
            try {
                Constructor<D> constructor = clazz.getConstructor(UUID.class, SQLStorage.class);
                D dataObject = constructor.newInstance(uuid, this);
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    if (!columnName.equals("uuid")) {
                        dataObject.set(columnName, rs.getObject(columnName));
                    }
                }
                return dataObject;
            } catch (Exception e) {
                throw new SQLException("Failed to load data object", e);
            }
        }
        return null;
    }

    @Override
    public void save(D dataObject) throws SQLException {
        Map<String, Object> data = dataObject.getData();
        data.put("uuid", dataObject.getUuid().toString());
        update(
                getTableName((Class<D>) dataObject.getClass()),
                data,
                "uuid = ?", dataObject.getUuid().toString()
        );
    }

    @Override
    public D create(UUID uuid, Class<D> clazz) throws SQLException {
        try {
            Constructor<D> constructor = clazz.getConstructor(UUID.class, SQLStorage.class);
            D dataObject = constructor.newInstance(uuid, this);
            Map<String, Object> data = new HashMap<>();
            data.put("uuid", uuid.toString());
            insert(getTableName(clazz), data);
            return dataObject;
        } catch (Exception e) {
            throw new SQLException("Failed to create data object", e);
        }
    }

    protected abstract String getTableName(Class<D> clazz);

}