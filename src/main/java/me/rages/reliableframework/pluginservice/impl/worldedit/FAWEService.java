package me.rages.reliableframework.pluginservice.impl.worldedit;

import com.sk89q.worldedit.math.BlockVector3;
import me.rages.reliableframework.pluginservice.PluginService;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public class FAWEService implements PluginService<FAWEService> {

    private WorldEditReader worldEditReader;

    @Override
    public FAWEService setup(JavaPlugin plugin) {
        Plugin foundPlugin = null;
        for (String name : pluginNames()) {
            if (plugin.getServer().getPluginManager().isPluginEnabled(name)) {
                foundPlugin = plugin.getServer().getPluginManager().getPlugin(name);
            }
        }

        // we could not find any usable WorldEdit plugins
        if (foundPlugin != null) {
            // determine if we want to use the latest WorldEdit
            try {
                Class.forName("com.sk89q.worldedit.math.BlockVector3"); // newer WorldEdit versions use this
                this.worldEditReader = new WorldEdit7Reader(); // 1.13 -> 1.20
            } catch (ClassNotFoundException ignored) {
//                this.worldEditReader = new WorldEdit6Reader(); // 1.8 -> 1.12.2
            }
        }
        return this;
    }

    public Map<BlockVector3, BlockData> readSchematic(File file) {
        return worldEditReader.readSchematic(file);
    }

    @Override
    public String[] pluginNames() {
        return new String[]{"WorldEdit", "FastAsyncWorldEdit"};
    }

}
