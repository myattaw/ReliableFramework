package me.rages.reliableframework.storage;

import me.rages.reliableframework.data.DataObject;
import me.rages.reliableframework.data.Entity;
import me.rages.reliableframework.data.annotations.Column;
import me.rages.reliableframework.data.annotations.Id;
import me.rages.reliableframework.data.annotations.Table;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Abstract class for SQL storage implementation in a Minecraft plugin.
 * This class provides basic functionalities to connect, disconnect,
 * and interact with an SQL database, as well as managing data objects.
 */
public abstract class SQLStorage implements Database {

    protected final JavaPlugin plugin;
    protected Connection connection;
    protected Class<? extends DataObject>[] dataObjectClasses;

    /**
     * Constructs an SQLStorage instance.
     *
     * @param plugin            the JavaPlugin instance
     * @param dataObjectClasses the data object classes managed by this storage
     */
    @SafeVarargs
    public SQLStorage(JavaPlugin plugin, Class<? extends DataObject>... dataObjectClasses) {
        this.plugin = plugin;
        this.dataObjectClasses = dataObjectClasses;
    }


    /**
     * Connects to the database.
     *
     * @return the SQLStorage instance
     * @throws SQLException if a database access error occurs
     */
    @Override
    public abstract SQLStorage connect() throws SQLException;


    /**
     * Disconnects from the database.
     *
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Gets the current database connection.
     *
     * @return the database connection
     */
    @Override
    public Connection getConnection() {
        return connection;
    }

    /**
     * Adds a new column to a table.
     *
     * @param tableName        the name of the table
     * @param columnDefinition the column definition
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void addColumn(String tableName, String columnDefinition) throws SQLException {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnDefinition;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Checks if a column exists in a table.
     *
     * @param tableName  the name of the table
     * @param columnName the name of the column
     * @return true if the column exists, false otherwise
     * @throws SQLException if a database access error occurs
     */
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

    /**
     * Inserts a data object into a table.
     *
     * @param tableName  the name of the table
     * @param dataObject the data object to insert
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void insert(String tableName, DataObject dataObject) throws SQLException {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<Object> params = new ArrayList<>();

        // Insert fields annotated with @Column
        for (Field field : dataObject.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                field.setAccessible(true);
                columns.append(column.name()).append(",");
                values.append("?,");
                try {
                    Object value = field.get(dataObject);
                    if (value instanceof UUID) {
                        value = value.toString();  // Convert UUID to string representation
                    }
                    params.add(value);
                } catch (IllegalAccessException e) {
                    throw new SQLException("Failed to access field value", e);
                }
            }
        }

        // Insert extra data values
        for (Map.Entry<String, Object> data : dataObject.getData().entrySet()) {
            columns.append(data.getKey()).append(",");
            values.append("?,");
            Object value = data.getValue();
            if (value instanceof UUID) {
                value = value.toString();  // Convert UUID to string representation
            }
            params.add(value);
        }

        // Remove the last comma from columns and values
        if (columns.length() > 0 && values.length() > 0) {
            columns.deleteCharAt(columns.length() - 1);
            values.deleteCharAt(values.length() - 1);
        }

        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object value : params) {
                preparedStatement.setObject(index++, value);
            }
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Executes a query on the database.
     *
     * @param query  the query string
     * @param params the query parameters
     * @return the result set of the query
     * @throws SQLException if a database access error occurs
     */
    @Override
    public ResultSet query(String query, Object... params) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps.executeQuery();
    }

    /**
     * Updates data in a table.
     *
     * @param tableName   the name of the table
     * @param data        the data to update
     * @param whereClause the where clause to specify which rows to update
     * @param whereParams the parameters for the where clause
     * @throws SQLException if a database access error occurs
     */
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

    /**
     * Deletes data from a table.
     *
     * @param tableName   the name of the table
     * @param whereClause the where clause to specify which rows to delete
     * @param whereParams the parameters for the where clause
     * @throws SQLException if a database access error occurs
     */
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


    /**
     * Creates a table with the specified columns.
     *
     * @param tableName the name of the table
     * @param columns   a map of column names and their data types
     * @throws SQLException if a database access error occurs
     */
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

    /**
     * Ensures a column exists in a table. If it doesn't, it adds the column.
     *
     * @param tableName  the name of the table
     * @param columnName the name of the column
     * @param value      the sample value to determine the column type
     * @throws SQLException if a database access error occurs
     */
    public void ensureColumnExists(String tableName, String columnName, Object value) throws SQLException {
        if (!columnExists(tableName, columnName)) {
            String columnType = getColumnType(value.getClass());
            addColumn(tableName, columnName + " " + columnType);
        }
    }

    /**
     * Loads a data object from the database asynchronously.
     *
     * @param entry      the identifier to query the data object
     * @param clazz      the class of the data object
     * @param <T>        the type of the data object
     * @return a CompletableFuture of the data object
     */
    @Override
    public <T extends DataObject> CompletableFuture<T> load(
            Entity.EntityEntry entry,
            Class<T> clazz
    ) {
        return CompletableFuture.supplyAsync(() -> {
            String tableName = getTableName(clazz);
            String sql = "SELECT * FROM " + tableName + " WHERE " + entry.getColumnName() + " = ?";
            try (ResultSet rs = query(sql, entry.getValue())) {
                if (rs.next()) {
                    T dataObject = createDataObjectInstance(clazz);
                    for (Field field : clazz.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Column.class)) {
                            Column column = field.getAnnotation(Column.class);
                            Object value = rs.getObject(column.name());
                            if (field.getType() == UUID.class && value instanceof String) {
                                value = UUID.fromString((String) value);
                            } else if (field.getType() == Boolean.class) {
                                value = (Integer) value != 0;
                            }
                            field.setAccessible(true);
                            field.set(dataObject, value);
                        }
                    }
                    fillDataObjectFromResultSet(dataObject, rs);
                    return dataObject;
                }
            } catch (SQLException | ReflectiveOperationException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Loads all data objects from the database asynchronously.
     *
     * @param clazz the class of the data objects
     * @param <T>   the type of the data objects
     * @return a CompletableFuture of a list of data objects
     */
    @Override
    public <T extends DataObject> CompletableFuture<List<T>> loadAll(Class<T> clazz) {
        return CompletableFuture.supplyAsync(() -> {
            List<T> dataObjects = new ArrayList<>();
            String tableName = getTableName(clazz);
            String sql = "SELECT * FROM " + tableName;
            try (ResultSet rs = query(sql)) {
                while (rs.next()) {
                    T dataObject = createDataObjectInstance(clazz);
                    for (Field field : clazz.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Column.class)) {
                            Column column = field.getAnnotation(Column.class);
                            Object value = rs.getObject(column.name());
                            if (field.getType() == UUID.class && value instanceof String) {
                                value = UUID.fromString((String) value);
                            } else if (field.getType() == Boolean.class) {
                                value = (Integer) value != 0;
                            }
                            field.setAccessible(true);
                            field.set(dataObject, value);
                        }
                    }
                    fillDataObjectFromResultSet(dataObject, rs);
                    dataObjects.add(dataObject);
                }
            } catch (SQLException | ReflectiveOperationException e) {
                e.printStackTrace();
            }
            return dataObjects;
        });
    }

    /**
     * Saves a data object to the database asynchronously.
     *
     * @param dataObject the data object to save
     * @return a CompletableFuture representing the save operation
     */
    public CompletableFuture<Void> save(DataObject dataObject) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> data = dataObject.getData();
                Entity.EntityEntry entry = getIdField(dataObject);

                if (entry == null) {
                    throw new SQLException("No @Id field found in data object");
                }

                // Update the data
                int rowsAffected = 0;
                if (!data.isEmpty()) {
                    rowsAffected = updateAndReturnAffectedRows(
                            getTableName(dataObject.getClass()),
                            data, entry.getColumnName() + " = ?", entry.getValue()
                    );
                }

                // If no rows were affected, insert new data
                if (rowsAffected == 0) {
                    insert(getTableName(dataObject.getClass()), dataObject);
                }

            } catch (SQLException e) {
                throw new RuntimeException("Failed to save data object", e);
            }
        });
    }

    /**
     * Creates an instance of a data object.
     *
     * @param clazz the class of the data object
     * @param <D>   the type of the data object
     * @return the data object instance
     * @throws ReflectiveOperationException if an error occurs during instantiation
     */
    private <D extends DataObject> D createDataObjectInstance(Class<D> clazz) throws ReflectiveOperationException {
        Constructor<D> constructor = clazz.getConstructor(SQLStorage.class);
        return constructor.newInstance(this);
    }

    /**
     * Fills a data object with data from a result set.
     *
     * @param dataObject the data object to fill
     * @param rs         the result set
     * @throws SQLException if a database access error occurs
     */
    private void fillDataObjectFromResultSet(DataObject dataObject, ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            dataObject.set(columnName, rs.getObject(columnName));
        }
    }

    /**
     * Gets the ID field from a data object.
     *
     * @param dataObject the data object
     * @param <D>        the type of the data object
     * @return the ID field as a map entry
     * @throws SQLException if a database access error occurs
     */
    private <D extends DataObject> Entity.EntityEntry getIdField(D dataObject) throws SQLException {
        for (Field field : dataObject.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                try {
                    return Entity.of(field.getName(), field.get(dataObject));
                } catch (IllegalAccessException e) {
                    throw new SQLException("Failed to access @Id field value", e);
                }
            }
        }
        return null;
    }

    /**
     * Updates data and returns the number of affected rows.
     *
     * @param tableName   the name of the table
     * @param data        the data to update
     * @param whereClause the where clause to specify which rows to update
     * @param whereParams the parameters for the where clause
     * @return the number of affected rows
     * @throws SQLException if a database access error occurs
     */
    private int updateAndReturnAffectedRows(String tableName, Map<String, Object> data, String whereClause, Object... whereParams) throws SQLException {
        String setClause = data.keySet().stream().map(key -> key + " = ?").collect(Collectors.joining(", "));
        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            for (Object value : data.values()) {
                if (value instanceof UUID) {
                    value = value.toString();  // Convert UUID to string representation
                }
                ps.setObject(index++, value);
            }
            for (Object param : whereParams) {
                ps.setObject(index++, param);
            }
            return ps.executeUpdate();
        }
    }

    /**
     * Gets the table name for a data object class.
     *
     * @param clazz the class of the data object
     * @return the table name
     * @throws IllegalArgumentException if the class does not have a @Table annotation
     */
    @Override
    public String getTableName(Class<? extends DataObject> clazz) {
        if (clazz.isAnnotationPresent(Table.class)) {
            Table table = clazz.getAnnotation(Table.class);
            return table.name();
        }
        throw new IllegalArgumentException("No @Table annotation found on class: " + clazz.getName());
    }


    /**
     * Creates tables for the specified data object classes.
     *
     * @param dataObjectClasses the data object classes
     * @throws SQLException if a database access error occurs
     */
    @SafeVarargs
    public final void createTablesForDataObjects(Class<? extends DataObject>... dataObjectClasses) throws SQLException {
        for (Class<? extends DataObject> dataObjectClass : dataObjectClasses) {
            String tableName = getTableName(dataObjectClass);
            Map<String, String> columns = new HashMap<>();
            for (Field field : dataObjectClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    columns.put(column.name(), getColumnType(field.getType()));
                }
            }
            createTable(tableName, columns);
        }
    }

    /**
     * Gets the SQL column type for a Java class.
     *
     * @param type the Java class
     * @return the SQL column type
     * @throws IllegalArgumentException if the data type is unsupported
     */
    public abstract String getColumnType(Class<?> type);

}