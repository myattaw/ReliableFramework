package me.rages.reliableframework.pluginservice;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages services for a JavaPlugin.
 *
 * <p>
 * This class allows for registration and retrieval of PluginService instances.
 * </p>
 *
 * @author Michael
 * @since 6/17/2022, Friday
 */
public class ServiceManager {

    private JavaPlugin plugin;
    private Map<Class<?>, PluginService<?>> classMap = new HashMap<>();

    /**
     * Constructs a new ServiceManager instance.
     *
     * @param plugin the JavaPlugin instance
     */
    private ServiceManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a new instance of ServiceManager.
     *
     * @param plugin the JavaPlugin instance
     * @return a new instance of ServiceManager
     */
    public static ServiceManager createServiceManager(JavaPlugin plugin) {
        return new ServiceManager(plugin);
    }

    /**
     * Registers a PluginService.
     *
     * @param pluginService the PluginService to register
     * @return this ServiceManager instance
     */
    public ServiceManager registerService(PluginService pluginService) {
        Arrays.stream(pluginService.pluginNames())
                .filter(name -> plugin.getServer().getPluginManager().getPlugin(name) != null)
                .forEach(name -> {
                    plugin.getLogger().log(Level.INFO, "Successfully hooked into " + name);
                    classMap.put(pluginService.getClass(), (PluginService<?>) pluginService.setup(plugin));
                });
        return this;
    }

    /**
     * Retrieves the PluginService instance associated with the specified class.
     *
     * @param <T>          the type of PluginService
     * @param serviceClass the class of the PluginService
     * @return the PluginService instance, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends PluginService<?>> T getService(Class<?> serviceClass) {
        return (T) classMap.get(serviceClass);
    }

}
