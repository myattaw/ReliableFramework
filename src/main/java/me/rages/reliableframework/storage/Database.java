package me.rages.reliableframework.storage;

import me.rages.reliableframework.data.DataObject;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for database operations in a Minecraft plugin.
 * This interface defines the standard methods for connecting,
 * disconnecting, and performing CRUD operations on the database.
 */
public interface Database {

    /**
     * Connects to the database.
     *
     * @return the SQLStorage instance
     * @throws SQLException if a database access error occurs
     */
    SQLStorage connect() throws SQLException;

    /**
     * Disconnects from the database.
     *
     * @throws SQLException if a database access error occurs
     */
    void disconnect() throws SQLException;

    /**
     * Gets the current database connection.
     *
     * @return the database connection
     * @throws SQLException if a database access error occurs
     */
    Connection getConnection() throws SQLException;

    /**
     * Adds a new column to a table.
     *
     * @param tableName        the name of the table
     * @param columnDefinition the column definition
     * @throws SQLException if a database access error occurs
     */
    void addColumn(String tableName, String columnDefinition) throws SQLException;

    /**
     * Inserts a data object into a table.
     *
     * @param tableName  the name of the table
     * @param dataObject the data object to insert
     * @throws SQLException if a database access error occurs
     */
    void insert(String tableName, DataObject dataObject) throws SQLException;

    /**
     * Executes a query on the database.
     *
     * @param query  the query string
     * @param params the query parameters
     * @return the result set of the query
     * @throws SQLException if a database access error occurs
     */
    ResultSet query(String query, Object... params) throws SQLException;

    /**
     * Updates data in a table.
     *
     * @param tableName   the name of the table
     * @param data        the data to update
     * @param whereClause the where clause to specify which rows to update
     * @param whereParams the parameters for the where clause
     * @throws SQLException if a database access error occurs
     */
    void update(String tableName, Map<String, Object> data, String whereClause, Object... whereParams) throws SQLException;

    /**
     * Deletes data from a table.
     *
     * @param tableName   the name of the table
     * @param whereClause the where clause to specify which rows to delete
     * @param whereParams the parameters for the where clause
     * @throws SQLException if a database access error occurs
     */
    void delete(String tableName, String whereClause, Object... whereParams) throws SQLException;

    /**
     * Creates a table with the specified columns.
     *
     * @param tableName the name of the table
     * @param columns   a map of column names and their data types
     * @throws SQLException if a database access error occurs
     */
    void createTable(String tableName, Map<String, String> columns) throws SQLException;

    /**
     * Checks if a column exists in a table.
     *
     * @param tableName  the name of the table
     * @param columnName the name of the column
     * @return true if the column exists, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean columnExists(String tableName, String columnName) throws SQLException;

    /**
     * Loads a data object from the database asynchronously.
     *
     * @param identifier the identifier to query the data object
     * @param clazz      the class of the data object
     * @param <T>        the type of the data object
     * @return a CompletableFuture of the data object
     */
    <T extends DataObject> CompletableFuture<T> load(
            Map.Entry<String, Object> identifier,
            Class<T> clazz
    );

    /**
     * Saves a data object to the database asynchronously.
     *
     * @param dataObject the data object to save
     * @return a CompletableFuture representing the save operation
     * @throws SQLException if a database access error occurs
     */
    CompletableFuture<Void> save(DataObject dataObject) throws SQLException;

    /**
     * Gets the table name for a data object class.
     *
     * @param clazz the class of the data object
     * @return the table name
     */
    String getTableName(Class<? extends DataObject> clazz);
}