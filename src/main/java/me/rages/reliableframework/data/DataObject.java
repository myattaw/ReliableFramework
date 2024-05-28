package me.rages.reliableframework.data;

import java.util.Map;
import java.util.Optional;

public interface DataObject {

    Map<String, Object> getData();

    <T> Optional<T> get(String key, Class<T> type);

    void set(String key, Object value);

}