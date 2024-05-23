package me.rages.reliableframework.files;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Represents a configuration file handler.
 *
 * <p>
 * This abstract class provides methods for managing plugin configuration files.
 * </p>
 *
 * @author Michael
 * @since 5/23/2024, Thursday
 */
@Getter
public abstract class ConfigFile {

    private FileConfiguration config;
    private final File configFile;
    private final String fileName;
    private final JavaPlugin plugin;

    /**
     * Constructs a new ConfigFile instance.
     *
     * @param plugin the JavaPlugin instance
     * @param fileName the name of the configuration file
     */
    public ConfigFile(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;

        // Create data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        // Initialize the configFile field
        this.configFile = new File(plugin.getDataFolder(), String.format("%s.yml", fileName.toLowerCase()));

        try {
            // Create the configuration file if it doesn't exist
            this.configFile.createNewFile();
            plugin.getLogger().log(Level.INFO, String.format("Creating new %s configuration file!", fileName));
        } catch (IOException e) {
            // Log error if failed to create configuration file
            plugin.getLogger().log(Level.SEVERE, "Failed to create configuration file!");
        }

        // Load the configuration from the file
        this.config = YamlConfiguration.loadConfiguration(this.configFile);
    }

    /**
     * Initializes the configuration file.
     *
     * <p>
     * This method should be implemented by subclasses to perform any initialization tasks.
     * </p>
     *
     * @return the initialized ConfigFile instance
     */
    public abstract ConfigFile init();

    /**
     * Reloads the configuration from the file.
     */
    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(this.configFile);
    }

    /**
     * Saves the configuration to the file.
     */
    public void save() {
        try {
            this.config.save(this.configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}