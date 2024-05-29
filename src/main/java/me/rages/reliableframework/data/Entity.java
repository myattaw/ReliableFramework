package me.rages.reliableframework.data;

import lombok.*;


/**
 * Represents a utility class for creating EntityEntry objects used for searching.
 */
public class Entity {

    /**
     * Creates an EntityEntry object for searching based on the provided column name and value.
     *
     * @param columnName The name of the column.
     * @param value      The value being queried.
     * @return An EntityEntry object representing the search criteria.
     */
    public static EntityEntry of(String columnName, Object value) {
        return new EntityEntry(columnName, value);
    }

    /**
     * Represents an entry containing a column name and its corresponding value.
     */
    @Data
    @AllArgsConstructor
    public static class EntityEntry {
        /**
         * The name of the column.
         */
        private String columnName;

        /**
         * The value associated with the column.
         */
        private Object value;
    }

}