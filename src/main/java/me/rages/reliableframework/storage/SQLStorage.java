package me.rages.reliableframework.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;

public abstract class SQLStorage<T extends JavaPlugin> implements Database {

    protected final T plugin;
    protected Connection connection;

    public SQLStorage(T plugin) {
        this.plugin = plugin;
    }

}