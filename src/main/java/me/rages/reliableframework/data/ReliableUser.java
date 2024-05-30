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
import java.util.UUID;

@Getter
@Setter
@Table(name = "users")
public class ReliableUser implements DataObject {

    @Id
    @Column(name = "uuid")
    private UUID uuid;
    @Column(name = "username")
    private String name;

    private final Map<String, Object> data = new HashMap<>();
    private final SQLStorage storage;

    public ReliableUser(SQLStorage storage) {
        this.storage = storage;
    }

}
