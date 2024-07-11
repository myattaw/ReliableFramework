package me.rages.reliableframework.pluginservice.impl.worldedit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.block.data.BlockData;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WorldEdit7Reader implements WorldEditReader {

    @Override
    public Map<BlockVector3, BlockData> readSchematic(File file) {
        Map<BlockVector3, BlockData> blockDataMap = new HashMap<>();
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format != null) {
            try (Closer closer = Closer.create()) {
                FileInputStream fis = closer.register(new FileInputStream(file));
                BufferedInputStream bis = closer.register(new BufferedInputStream(fis));
                ClipboardReader reader = closer.register(format.getReader(bis));
                Clipboard clipboard = reader.read();
                ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);


                for (BlockVector3 vector3 : clipboardHolder.getClipboard().getRegion()) {

                    BaseBlock baseBlock = clipboard.getFullBlock(vector3);

                    if (baseBlock.getBlockType().getMaterial().isAir()) continue;
                    blockDataMap.put(vector3, BukkitAdapter.adapt(baseBlock));

                }
                return blockDataMap;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

}
