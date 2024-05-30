package me.rages.reliableframework.data;

import me.rages.reliableframework.data.annotations.Table;
import me.rages.reliableframework.storage.SQLStorage;
import me.rages.reliableframework.storage.impl.MySQLStorage;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

public interface DataObject {

    Map<String, Object> getData();

    SQLStorage getStorage();

    default <T> Optional<T> get(String key, Class<T> type) {
        Object value = getData().get(key);
        return Optional.ofNullable(type.isInstance(value) ? type.cast(value) : null);
    }

    default void set(String key, Object value) {
        try {
            if (!getData().containsKey(key)) {
                getStorage().ensureColumnExists(getTableName(getClass()), key, value);
            }
            getData().put(key, value);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to set value for key: " + key, e);
        }
    }

    static String getTableName(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Table.class)) {
            return clazz.getAnnotation(Table.class).name();
        }
        throw new IllegalArgumentException("Class " + clazz.getSimpleName() + " does not have a @Table annotation");
    }

}