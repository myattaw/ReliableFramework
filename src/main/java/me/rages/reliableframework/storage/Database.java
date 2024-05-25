package me.rages.reliableframework.storage;

import java.sql.Connection;
import java.sql.SQLException;

public interface Database {

    Connection connect() throws SQLException;

    void disconnect() throws SQLException;

    Connection getConnection() throws SQLException;

    void setupTables() throws SQLException;

    void addColumn(String tableName, String columnDefinition) throws SQLException;

}
