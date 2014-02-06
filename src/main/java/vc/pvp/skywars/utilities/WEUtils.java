package vc.pvp.skywars.utilities;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import vc.pvp.skywars.SkyWars;
import vc.pvp.skywars.build.BlockBuilder;
import vc.pvp.skywars.build.BlockBuilderEntry;
import vc.pvp.skywars.config.PluginConfig;
import vc.pvp.skywars.game.Game;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WEUtils {

    public static boolean pasteSchematic(Location origin, CuboidClipboard schematic) {
        EditSession editSession = new EditSession(new BukkitWorld(origin.getWorld()), Integer.MAX_VALUE);
        editSession.setFastMode(true);

        try {
            schematic.paste(editSession, new Vector(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()), PluginConfig.ignoreAir());
        } catch (MaxChangedBlocksException ignored) {
            return false;
        }

        return true;
    }

    public static void buildSchematic(final Game game, final Location origin, final CuboidClipboard schematic) {
        Bukkit.getScheduler().runTaskAsynchronously(SkyWars.get(), new Runnable() {
            @SuppressWarnings("deprecation")
			@Override
            public void run() {
                Vector pasteLocation = new Vector(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());

                Vector currentPoint;
                BaseBlock currentBlock;
                List<BlockBuilderEntry> blockQueue = Lists.newLinkedList();
                List<BlockBuilderEntry> delayedQueue = Lists.newLinkedList();

                for (int xxx = 0; xxx < schematic.getSize().getBlockX(); ++xxx) {
                    for (int yyy = 0; yyy < schematic.getSize().getBlockY(); ++yyy) {
                        for (int zzz = 0; zzz < schematic.getSize().getBlockZ(); ++zzz) {
                            currentPoint = new Vector(xxx, yyy, zzz);
                            currentBlock = schematic.getPoint(currentPoint);

                            if (currentBlock.isAir()) {
                                continue;
                            }

                            currentPoint = currentPoint.add(pasteLocation).add(schematic.getOffset());

                            switch (currentBlock.getType()) {
                                // Add blocks with (possible) tile entities on the end, might decrease client-side lag.
                                case 8: // Water
                                case 9: // Stationary Water
                                case 10: // Lava
                                case 11: // Stationary Lava
                                case 63: // Sign post
                                case 68: // Wall sign
                                case 54: // Chest
                                case 23: // Dispenser
                                case 61: // Furnace
                                case 117: // Brewing stance
                                case 154: // Hopper
                                case 158: // Dropper
                                case 138: // Beacon
                                case 52: // Mob spawner
                                case 25: // Note block
                                case 84: // Jukebox
                                case 116: // Enchantment Table
                                case 119: // End portal
                                case 120: // End portal frame
                                case 114: // Mob head
                                case 137: // Command block
                                    delayedQueue.add(new BlockBuilderEntry(currentPoint, currentBlock));
                                    break;

                                default:
                                    blockQueue.add(new BlockBuilderEntry(currentPoint, currentBlock));
                            }
                        }
                    }
                }

                Collections.sort(blockQueue, new Comparator<BlockBuilderEntry>() {
                    @Override
                    public int compare(BlockBuilderEntry o1, BlockBuilderEntry o2) {
                    	return Integer.valueOf(o1.getLocation().getBlockY()).compareTo(Integer.valueOf(o2.getLocation().getBlockY()));
                    }
                });

                int blockCount = blockQueue.size() + delayedQueue.size();
                int ticksRequired = blockCount / PluginConfig.blocksPerTick();
                int time = (int) (ticksRequired * (PluginConfig.buildInterval() * 50L));
                game.setTimer((int) (time / 1000L));

                EditSession editSession = new EditSession(new BukkitWorld(origin.getWorld()), Integer.MAX_VALUE);
                editSession.setFastMode(true);

                BlockBuilder blockBuilder = new BlockBuilder(editSession, blockQueue, delayedQueue, PluginConfig.blocksPerTick(), new BlockBuilder.BuildFinishedHandler() {
                    @Override
                    public void onBuildFinish() {
                        game.setBuilt(true);

                        if (game.isFull()) {
                            game.onGameStart();
                        } else {
                            game.setTimer(1);
                        }
                    }
                });
                blockBuilder.start(40L, PluginConfig.buildInterval());
            }
        });
    }
}
