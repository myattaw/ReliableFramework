package me.rages.reliableframework.data;

import me.rages.reliableframework.data.annotations.Table;
import me.rages.reliableframework.storage.SQLStorage;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * DataObject interface providing default methods for handling
 * data storage and retrieval operations.
 */
public interface DataObject {

    /**
     * Gets the data map that holds key-value pairs.
     *
     * @return a map containing the data.
     */
    Map<String, Object> getData();

    /**
     * Gets the SQL storage instance used for database operations.
     *
     * @return the SQLStorage instance.
     */
    SQLStorage getStorage();

    /**
     * Retrieves a value from the data map and casts it to the specified type.
     *
     * @param key  the key whose associated value is to be returned.
     * @param type the class of the type to which the value should be cast.
     * @param <T>  the type of the value.
     * @return an Optional containing the value if present and of the correct type, otherwise an empty Optional.
     */
    default <T> Optional<T> get(String key, Class<T> type) {
        Object value = getData().get(key);
        return Optional.ofNullable(type.isInstance(value) ? type.cast(value) : null);
    }

    /**
     * Sets a value in the data map and ensures the corresponding column exists in the database.
     *
     * @param key   the key with which the specified value is to be associated.
     * @param value the value to be associated with the specified key.
     * @throws IllegalStateException if there is an error ensuring the column exists or setting the value.
     */
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

    /**
     * Retrieves the table name for the specified class.
     *
     * @param clazz the class whose table name is to be retrieved.
     * @return the table name associated with the specified class.
     * @throws IllegalArgumentException if the class does not have a @Table annotation.
     */
    static String getTableName(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Table.class)) {
            return clazz.getAnnotation(Table.class).name();
        }
        throw new IllegalArgumentException("Class " + clazz.getSimpleName() + " does not have a @Table annotation");
    }

}