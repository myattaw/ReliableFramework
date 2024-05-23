package me.rages.reliableframework.pluginservice;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents a plugin service.
 *
 * <p>
 * This interface defines methods for initializing the service and providing plugin names.
 * </p>
 *
 * @param <T> the type of service to be setup
 * @author Michael
 * @since 6/17/2022, Friday
 */
public interface PluginService<T> {

    /**
     * Initializes the PluginService.
     *
     * <p>
     * This method should be implemented to initialize the PluginService and return itself for access.
     * </p>
     *
     * @param plugin the JavaPlugin instance
     * @return the initialized PluginService instance
     */
    T setup(JavaPlugin plugin);

    /**
     * Returns possible plugin names.
     *
     * <p>
     * This method should return an array of possible names the plugin may use.
     * </p>
     *
     * @return possible names the plugin may use
     */
    String[] pluginNames();

}