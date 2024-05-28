package me.rages.reliableframework.storage;

import me.rages.reliableframework.data.annotations.Column;
import me.rages.reliableframework.data.DataObject;
import me.rages.reliableframework.data.annotations.Id;
import me.rages.reliableframework.data.annotations.Table;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public abstract class SQLStorage implements Database {

    protected final JavaPlugin plugin;
    protected Connection connection;
    protected Class<? extends DataObject>[] dataObjectClasses;

    @SafeVarargs
    public SQLStorage(JavaPlugin plugin, Class<? extends DataObject>... dataObjectClasses) {
        this.plugin = plugin;
        this.dataObjectClasses = dataObjectClasses;
    }

    @Override
    public abstract SQLStorage connect() throws SQLException;

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
    public void insert(String tableName, DataObject dataObject) throws SQLException {
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
    public <T extends DataObject> T load(Map.Entry<String, Object> identifier, Class<? extends DataObject> clazz) throws SQLException {
        String tableName = getTableName(clazz);
        String sql = "SELECT * FROM " + tableName + " WHERE " + identifier.getKey() + " = ?";
        try (ResultSet rs = query(sql, identifier.getValue())) {
            if (rs.next()) {
                DataObject dataObject = createDataObjectInstance(clazz);
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Column.class)) {
                        field.setAccessible(true);
                        System.out.println(field.getName() + ":" + rs.getObject(field.getName()));
                        field.set(dataObject, rs.getObject(field.getName()));
                    }
                }
                fillDataObjectFromResultSet(dataObject, rs);
                return (T) dataObject;
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
    public void save(DataObject dataObject) throws SQLException {
        Map<String, Object> data = dataObject.getData();
        Map.Entry<String, Object> idField = getIdField(dataObject);

        if (idField == null) {
            throw new SQLException("No @Id field found in data object");
        }

        int rowsAffected = updateAndReturnAffectedRows(
                getTableName(dataObject.getClass()),
                data, idField.getKey() + " = ?", idField.getValue()
        );

        if (rowsAffected == 0) {
            data.put(idField.getKey(), idField.getValue());
            insert(getTableName(dataObject.getClass()), dataObject);
        }
    }

    private <D extends DataObject> Map.Entry<String, Object> getIdField(D dataObject) throws SQLException {
        for (Field field : dataObject.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                try {
                    return Map.entry(field.getName(), field.get(dataObject));
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

    @Override
    public String getTableName(Class<? extends DataObject> clazz) {
        if (clazz.isAnnotationPresent(Table.class)) {
            Table table = clazz.getAnnotation(Table.class);
            return table.name();
        }
        throw new IllegalArgumentException("No @Table annotation found on class: " + clazz.getName());
    }

    public void createTablesForDataObjects(Class<? extends DataObject>... dataObjectClasses) throws SQLException {
        for (Class<? extends DataObject> dataObjectClass : dataObjectClasses) {
            String tableName = getTableName(dataObjectClass);
            Map<String, String> columns = new HashMap<>();
            for (Field field : dataObjectClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class)) {
                    columns.put(field.getName(), getColumnType(field.getType()));
                }
            }
            createTable(tableName, columns);
        }
    }

    private String getColumnType(Class<?> type) {
        if (type == Integer.class || type == Long.class) {
            return "INTEGER";
        } else if (type == String.class) {
            return "TEXT";
        } else if (type == Boolean.class) {
            return "BOOLEAN";
        } else if (type == Double.class || type == Float.class) {
            return "REAL";
        } else if (type == java.util.Date.class) {
            return "INTEGER";
        } else if (type == byte[].class) {
            return "BLOB";
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + type.getName());
        }
    }

}
