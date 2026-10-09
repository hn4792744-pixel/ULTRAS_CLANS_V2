package me.uc.hussein.ultrasclans.listener;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.task.TaskType;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class MiningListener implements Listener {

    private final UltrasClansPlugin plugin;
    private final File dataFile;
    private final Set<String> placedBlocks = new HashSet<>();

    public MiningListener(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "placed-blocks.yml");

        plugin.getDataFolder().mkdirs();

        if (dataFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
            placedBlocks.addAll(yaml.getStringList("blocks"));
        }
    }

    private String blockKey(Block block) {
        return block.getWorld().getName() + ":"
                + block.getX() + ":"
                + block.getY() + ":"
                + block.getZ();
    }

    private void savePlacedBlocks() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blocks", new ArrayList<>(placedBlocks));

        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save placed-blocks.yml: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        placedBlocks.add(blockKey(event.getBlockPlaced()));
        savePlacedBlocks();

        plugin.getClanService().getPlayerClan(event.getPlayer().getUniqueId())
                .ifPresent(clan -> plugin.getTaskService()
                        .incrementProgress(clan, TaskType.PLACE_BLOCKS, 1));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();

        if (placedBlocks.remove(blockKey(block))) {
            savePlacedBlocks();
            return;
        }

        plugin.getMiningProgressManager()
                .recordBlockBreak(event.getPlayer(), block.getType().name());

        plugin.getClanService().getPlayerClan(event.getPlayer().getUniqueId())
                .ifPresent(clan -> plugin.getTaskService()
                        .incrementProgress(clan, TaskType.MINE_BLOCKS, 1));
    }
}
