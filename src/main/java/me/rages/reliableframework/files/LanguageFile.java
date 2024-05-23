package me.rages.reliableframework.file;

import org.bukkit.plugin.java.JavaPlugin;

public class LanguageFile extends ConfigFile {

    public LanguageFile(JavaPlugin plugin, String fileName) {
        super(plugin, fileName);
    }

    @Override
    public LanguageFile init() {

        this.save();
        return this;
    }

}