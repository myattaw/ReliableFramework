package me.rages.reliableframework.storage;

import me.rages.reliableframework.data.DataObject;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public interface Database<D extends DataObject> {

    SQLStorage<?, D> connect() throws SQLException;

    void disconnect() throws SQLException;

    Connection getConnection() throws SQLException;

    void addColumn(String tableName, String columnDefinition) throws SQLException;

    void insert(String tableName, Map<String, Object> data) throws SQLException;

    ResultSet query(String query, Object... params) throws SQLException;

    void update(String tableName, Map<String, Object> data, String whereClause, Object... whereParams) throws SQLException;

    void delete(String tableName, String whereClause, Object... whereParams) throws SQLException;

    void createTable(String tableName, Map<String, String> columns) throws SQLException;

    boolean columnExists(String tableName, String columnName) throws SQLException;

    D load(UUID uuid, Class<D> clazz) throws SQLException;

    void save(D dataObject) throws SQLException;

    D create(UUID uuid, Class<D> clazz) throws SQLException;

}
