package me.rages.reliableframework.data;

import lombok.*;
import me.rages.reliableframework.storage.SQLStorage;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class User implements DataObject {

    @Id
    @Column
    private String uuid;
    @Column
    private String name;
    private final Map<String, Object> data = new HashMap<>();
    private final SQLStorage<?, ?> storage;

    public User(SQLStorage<?, ?> storage) {
        this.storage = storage;
    }


    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }

    @Override
    public void set(String key, Object value) throws SQLException {
        if (!data.containsKey(key)) {
            storage.ensureColumnExists("users", key, value);
        }
        data.put(key, value);
    }

}
