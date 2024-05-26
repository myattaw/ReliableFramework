package me.rages.reliableframework.data;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface DataObject {

    Map<String, Object> getData();
    <T> Optional<T> get(String key, Class<T> type);
    void set(String key, Object value) throws SQLException;

}