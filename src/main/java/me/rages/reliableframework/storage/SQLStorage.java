package me.rages.reliableframework.storage;

import me.rages.reliableframework.data.Column;
import me.rages.reliableframework.data.DataObject;
import me.rages.reliableframework.data.Id;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public void insert(String tableName, D dataObject) throws SQLException {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (Field field : dataObject.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                columns.append(field.getName()).append(",");
                values.append("?,");
                try {
                    params.add(field.get(dataObject));
                } catch (IllegalAccessException e) {
                    throw new SQLException("Failed to access field value", e);
                }
            }
        }

        columns.deleteCharAt(columns.length() - 1);
        values.deleteCharAt(values.length() - 1);

        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object value : params) {
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
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object value : data.values()) {
                preparedStatement.setObject(index++, value);
            }
            for (Object param : whereParams) {
                preparedStatement.setObject(index++, param);
            }
            preparedStatement.executeUpdate();
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
    public D load(Map.Entry<String, Object> identifier, Class<D> clazz) throws SQLException {
        ResultSet rs = query("SELECT * FROM " + getTableName(clazz) + " WHERE "
                + identifier.getKey() + " = ?", identifier.getValue());
        if (rs.next()) {
            try {
                Constructor<D> constructor = clazz.getConstructor(SQLStorage.class);
                D dataObject = constructor.newInstance(this);
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Column.class)) {
                        field.setAccessible(true);
                        field.set(dataObject, rs.getObject(field.getName()));
                    }
                }

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    dataObject.set(columnName, rs.getObject(columnName));
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
        String idFieldName = null;
        Object idFieldValue = null;

        for (Field field : dataObject.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                idFieldName = field.getName();
                try {
                    idFieldValue = field.get(dataObject);
                } catch (IllegalAccessException e) {
                    throw new SQLException("Failed to access @Id field value", e);
                }
                break;
            }
        }

        if (idFieldName == null || idFieldValue == null) {
            throw new SQLException("No @Id field found in data object");
        }

        // Attempt to update the record
        int rowsAffected = updateAndReturnAffectedRows(
                getTableName((Class<D>) dataObject.getClass()),
                data, idFieldName + " = ?", idFieldValue
        );

        // If no rows were affected, perform an insert so we don't need a create function
        if (rowsAffected == 0) {
            data.put(idFieldName, idFieldValue);  // Ensure ID field is included in the data for insertion
            insert(getTableName((Class<D>) dataObject.getClass()), dataObject);
        }
    }

    // Helper method to perform update and return number of affected rows
    private int updateAndReturnAffectedRows(String tableName, Map<String, Object> data, String whereClause, Object... whereParams) throws SQLException {
        StringBuilder setClause = new StringBuilder();
        for (String column : data.keySet()) {
            setClause.append(column).append(" = ?,");
        }
        setClause.deleteCharAt(setClause.length() - 1);

        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause;
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object value : data.values()) {
                preparedStatement.setObject(index++, value);
            }
            for (Object param : whereParams) {
                preparedStatement.setObject(index++, param);
            }
            return preparedStatement.executeUpdate();
        }
    }


    protected abstract String getTableName(Class<D> clazz);

}