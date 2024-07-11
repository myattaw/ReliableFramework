package me.rages.reliableframework.pluginservice.impl.worldedit;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.block.data.BlockData;

import java.io.File;
import java.util.Map;

/**
 * @author : Michael
 * @since : 6/23/2022, Thursday
 **/
public interface WorldEditReader {

    /**
     * Reads a schematic from file and adds to list
     * @param file the schematic file
     */
    Map<BlockVector3, BlockData> readSchematic(File file);

}