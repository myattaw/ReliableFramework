package me.rages.reliableframework.data;

import lombok.*;
import me.rages.reliableframework.data.annotations.Column;
import me.rages.reliableframework.data.annotations.Id;
import me.rages.reliableframework.data.annotations.Table;
import me.rages.reliableframework.storage.SQLStorage;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@Table(name = "users")
public class User implements DataObject {

    @Id
    @Column
    private String uuid;
    @Column
    private String name;

    private final Map<String, Object> data = new HashMap<>();
    private final SQLStorage storage;

    public User(SQLStorage storage) {
        this.storage = storage;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        return Optional.ofNullable(type.cast(data.get(key)));
    }

    @Override
    public void set(String key, Object value) throws SQLException {
        if (!data.containsKey(key)) {
            storage.ensureColumnExists("users", key, value);
        }
        data.put(key, value);
    }

}
