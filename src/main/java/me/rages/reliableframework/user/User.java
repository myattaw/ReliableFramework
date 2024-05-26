package me.rages.reliableframework.user;

import lombok.Getter;
import me.rages.reliableframework.storage.SQLStorage;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
public class User {

    private final UUID uuid;
    private final String name;
    private final Map<String, Object> data;
    private final SQLStorage<?> storage;

    public User(UUID uuid, String name, SQLStorage<?> storage) {
        this.uuid = uuid;
        this.name = name;
        this.storage = storage;
        this.data = new HashMap<>();
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }

    public void set(String key, Object value) throws SQLException {
        if (!data.containsKey(key)) {
            storage.ensureColumnExists("users", key, value);
        }
        data.put(key, value);
    }

}
