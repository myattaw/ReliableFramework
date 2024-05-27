package me.rages.reliableframework.storage;

import me.rages.reliableframework.data.Column;
import me.rages.reliableframework.data.DataObject;
import me.rages.reliableframework.data.Id;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

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
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnDefinition;
        try (Statement stmt = connection.createStatement()) {
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
        Map<String, Object> data = dataObject.getData();
        String columns = String.join(",", data.keySet());
        String values = String.join(",", Collections.nCopies(data.size(), "?"));

        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object value : data.values()) {
                ps.setObject(index++, value);
            }
            ps.executeUpdate();
        }
    }

    @Override
    public ResultSet query(String query, Object... params) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps.executeQuery();
    }

    @Override
    public void update(String tableName, Map<String, Object> data, String whereClause, Object... whereParams) throws SQLException {
        String setClause = String.join(" = ?, ", data.keySet()) + " = ?";
        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object value : data.values()) {
                ps.setObject(index++, value);
            }
            for (Object param : whereParams) {
                ps.setObject(index++, param);
            }
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String tableName, String whereClause, Object... whereParams) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE " + whereClause;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < whereParams.length; i++) {
                ps.setObject(i + 1, whereParams[i]);
            }
            ps.executeUpdate();
        }
    }

    @Override
    public void createTable(String tableName, Map<String, String> columns) throws SQLException {
        String columnDefinitions = columns.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnDefinitions + ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void ensureColumnExists(String tableName, String columnName, Object value) throws SQLException {
        if (!columnExists(tableName, columnName)) {
            String columnType = getColumnType(value);
            addColumn(tableName, columnName + " " + columnType);
        }
    }

    private String getColumnType(Object value) {
        if (value instanceof Integer || value instanceof Long) {
            return "INTEGER DEFAULT 0";
        } else if (value instanceof String) {
            return "TEXT";
        } else if (value instanceof Boolean) {
            return "BOOLEAN DEFAULT FALSE";
        } else if (value instanceof Double || value instanceof Float) {
            return "REAL";
        } else if (value instanceof java.util.Date) {
            return "INTEGER DEFAULT 0";
        } else if (value instanceof byte[]) {
            return "BLOB";
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + value.getClass().getName());
        }
    }

    @Override
    public D load(Map.Entry<String, Object> identifier, Class<D> clazz) throws SQLException {
        String tableName = getTableName(clazz);
        String sql = "SELECT * FROM " + tableName + " WHERE " + identifier.getKey() + " = ?";
        try (ResultSet rs = query(sql, identifier.getValue())) {
            if (rs.next()) {
                D dataObject = createDataObjectInstance(clazz);
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Column.class)) {
                        field.setAccessible(true);
                        field.set(dataObject, rs.getObject(field.getName()));
                    }
                }
                fillDataObjectFromResultSet(dataObject, rs);
                return dataObject;
            }
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Failed to load data object", e);
        }
        return null;
    }

    private <D extends DataObject> D createDataObjectInstance(Class<D> clazz) throws ReflectiveOperationException {
        Constructor<D> constructor = clazz.getConstructor(SQLStorage.class);
        return constructor.newInstance(this);
    }

    private void fillDataObjectFromResultSet(DataObject dataObject, ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            dataObject.set(columnName, rs.getObject(columnName));
        }
    }

    @Override
    public void save(D dataObject) throws SQLException {
        Map<String, Object> data = dataObject.getData();
        Map.Entry<String, Object> idField = getIdField(dataObject);

        if (idField == null) {
            throw new SQLException("No @Id field found in data object");
        }

        int rowsAffected = updateAndReturnAffectedRows(
                getTableName((Class<D>) dataObject.getClass()),
                data, idField.getKey() + " = ?", idField.getValue()
        );

        if (rowsAffected == 0) {
            data.put(idField.getKey(), idField.getValue());
            insert(getTableName((Class<D>) dataObject.getClass()), dataObject);
        }
    }

    private <D extends DataObject> Map.Entry<String, Object> getIdField(D dataObject) throws SQLException {
        for (Field field : dataObject.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                try {
                    return new AbstractMap.SimpleEntry<>(field.getName(), field.get(dataObject));
                } catch (IllegalAccessException e) {
                    throw new SQLException("Failed to access @Id field value", e);
                }
            }
        }
        return null;
    }

    private int updateAndReturnAffectedRows(String tableName, Map<String, Object> data, String whereClause, Object... whereParams) throws SQLException {
        String setClause = data.keySet().stream().map(key -> key + " = ?").collect(Collectors.joining(", "));
        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object value : data.values()) {
                ps.setObject(index++, value);
            }
            for (Object param : whereParams) {
                ps.setObject(index++, param);
            }
            return ps.executeUpdate();
        }
    }

    protected abstract String getTableName(Class<D> clazz);

}