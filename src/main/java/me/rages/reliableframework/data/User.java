package me.rages.reliableframework.data;

import lombok.Getter;
import me.rages.reliableframework.storage.SQLStorage;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
public class User implements DataObject {

    private final UUID uuid;
    private final String name;
    private final Map<String, Object> data;
    private final SQLStorage<?, ?> storage;

    public User(UUID uuid, SQLStorage<?, ?> storage) {
        this.uuid = uuid;
        this.name = Bukkit.getOfflinePlayer(uuid).getName();
        this.storage = storage;
        this.data = new HashMap<>();
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
