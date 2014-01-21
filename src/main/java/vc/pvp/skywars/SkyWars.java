package vc.pvp.skywars;

import com.onarandombox.MultiverseCore.MultiverseCore;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import vc.pvp.skywars.commands.MainCommand;
import vc.pvp.skywars.commands.ScoreCommand;
import vc.pvp.skywars.controllers.*;
import vc.pvp.skywars.database.Database;
import vc.pvp.skywars.listeners.BlockListener;
import vc.pvp.skywars.listeners.EntityListener;
import vc.pvp.skywars.listeners.InventoryListener;
import vc.pvp.skywars.listeners.PlayerListener;
import vc.pvp.skywars.metrics.MetricsLite;
import vc.pvp.skywars.player.GamePlayer;
import vc.pvp.skywars.storage.DataStorage;
import vc.pvp.skywars.storage.SQLStorage;
import vc.pvp.skywars.tasks.SyncTask;
import vc.pvp.skywars.utilities.CraftBukkitUtil;
import vc.pvp.skywars.utilities.FileUtils;
import vc.pvp.skywars.utilities.Messaging;
import vc.pvp.skywars.utilities.StringUtils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import org.bukkit.World;
import vc.pvp.skywars.config.PluginConfig;
import vc.pvp.skywars.utilities.WorldGenerator;

public class SkyWars extends JavaPlugin {

    private static SkyWars instance;
    private static Permission permission;
    private static Economy economy;
    private static Chat chat;
    private Database database;

    @Override
    public void onEnable() {
        instance = this;

        deleteIslandWorlds();

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        PluginConfig.migrateConfig();
        reloadConfig();

        new Messaging(this);

        getCommand("skywars").setExecutor(new MainCommand());
        getCommand("score").setExecutor(new ScoreCommand());
        getCommand("global").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (!(sender instanceof Player)) {
                    return false;
                }

                if (args.length == 0) {
                    sender.sendMessage("\247cUsage: /" + label + " <message>");
                    return true;
                }

                StringBuilder messageBuilder = new StringBuilder();
                for (String arg : args) {
                    messageBuilder.append(arg);
                    messageBuilder.append(" ");
                }

                GamePlayer gamePlayer = PlayerController.get().get((Player) sender);
                String score = StringUtils.formatScore(gamePlayer.getScore());
            
                String prefix = null;
                if (SkyWars.getChat() != null) {
                    prefix = SkyWars.getChat().getPlayerPrefix(gamePlayer.getBukkitPlayer());
                }
                if (prefix == null) {
                    prefix = "";
                }
                Bukkit.broadcastMessage(new Messaging.MessageFormatter()
                        .setVariable("player", gamePlayer.getBukkitPlayer().getDisplayName())
                        .setVariable("score", score)
                        .setVariable("message", Messaging.stripColor(messageBuilder.toString()))
                        .setVariable("prefix", prefix)
                        .format("chat.global"));

                return true;
            }
        });

        try {
            DataStorage.DataStorageType dataStorageType = DataStorage.DataStorageType.valueOf(getConfig().getString("data-storage", "FILE"));
            if (dataStorageType == DataStorage.DataStorageType.SQL && !setupDatabase()) {
                getLogger().log(Level.INFO, "Couldn't setup database, now using file storage.");
                DataStorage.setInstance(DataStorage.DataStorageType.FILE);

            } else {
                DataStorage.setInstance(dataStorageType);
            }

        } catch (NullPointerException npe) {
            DataStorage.setInstance(DataStorage.DataStorageType.FILE);
        }

        setupPermission();
        setupEconomy();
        setupChat();

        SchematicController.get();
        WorldController.get();
        GameController.get();
        PlayerController.get();
        ChestController.get();
        KitController.get();
        IconMenuController.get();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
        	PlayerController.get().register(player);
        }
        
        GlobalScoreboardController.get();

        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }

        /*if (getDB() != null) {
            StatisticsController.get();
            new StatisticsUpdater();
        }*/

        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);
        Bukkit.getPluginManager().registerEvents(new EntityListener(), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new SyncTask(), 20L, 20L);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);

        GameController.get().shutdown();
        PlayerController.get().shutdown();
    	GlobalScoreboardController.get().shutdown();

        if (DataStorage.get() instanceof SQLStorage && !CraftBukkitUtil.isRunning()) {
            SQLStorage sqlStorage = (SQLStorage) DataStorage.get();
            while (!sqlStorage.saveProcessor.isEmpty());
            long currentTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - currentTime < 1000L);
            sqlStorage.saveProcessor.stop();
        }

        if (database != null) {
            database.close();
        }

        deleteIslandWorlds();
    }

    private void deleteIslandWorlds() {
        // Worlds
        File workingDirectory = new File(".");
        File[] contents = workingDirectory.listFiles();

        if (contents != null) {
            for (File file : contents) {
                if (!file.isDirectory() || !file.getName().matches("island-\\d+")) {
                    continue;
                }
                World world = this.getServer().getWorld(file.getName());
                Boolean result = false;
                if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null) {
                    MultiverseCore multiVerse = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
                    if (world != null) {
                        result = multiVerse.getMVWorldManager().deleteWorld(file.getName());
                    } else {
                        result = multiVerse.getMVWorldManager().removeWorldFromConfig(file.getName());
                    }
                }
                if (!result) {
                    if (world != null) {
                        result = this.getServer().unloadWorld(world, true);
                        if (result == true) {
                            this.getLogger().log(Level.INFO, "World ''{0}'' was unloaded from memory.", file.getName());
                        } else {
                            this.getLogger().log(Level.SEVERE, "World ''{0}'' could not be unloaded.", file.getName());
                        }
                    }
                    result = FileUtils.deleteFolder(file);
                    if (result == true) {
                        this.getLogger().log(Level.INFO, "World ''{0}'' was deleted.", file.getName());
                    } else {
                        this.getLogger().log(Level.SEVERE, "World ''{0}'' was NOT deleted.", file.getName());
                        this.getLogger().log(Level.SEVERE, "Are you sure the folder {0} exists?", file.getName());
                        this.getLogger().log(Level.SEVERE, "Please check your file permissions on ''{0}''", file.getName());
                    }
                }
            }
        }

        // WorldGuard
        workingDirectory = new File("./plugins/WorldGuard/worlds/");
        contents = workingDirectory.listFiles();

        if (contents != null) {
            for (File file : contents) {
                if (!file.isDirectory() || !file.getName().matches("island-\\d+")) {
                    continue;
                }

                FileUtils.deleteFolder(file);
            }
        }
    }

    private boolean setupDatabase() {
        try {
            database = new Database(getConfig().getConfigurationSection("database"));

        } catch (ClassNotFoundException exception) {
            getLogger().log(Level.SEVERE, String.format("Unable to register JDCB driver: %s", exception.getMessage()));
            return false;

        } catch (SQLException exception) {
            getLogger().log(Level.SEVERE, String.format("Unable to connect to SQL server: %s", exception.getMessage()));
            return false;
        }

        try {
            database.createTables();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, String.format("An exception was thrown while attempting to create tables: %s", exception.getMessage()));
            return false;
        }

        return true;
    }

    private void setupPermission() {
        RegisteredServiceProvider<Permission> chatProvider = getServer().getServicesManager().getRegistration(Permission.class);
        if (chatProvider != null) {
            permission = chatProvider.getProvider();
        }
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> chatProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (chatProvider != null) {
            economy = chatProvider.getProvider();
        }
    }

    private void setupChat() {
        RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(Chat.class);
        if (chatProvider != null) {
            chat = chatProvider.getProvider();
        }
    }

    public static SkyWars get() {
        return instance;
    }

    public static Permission getPermission() {
        return permission;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public static Chat getChat() {
        return chat;
    }

    public static Database getDB() {
        return instance.database;
    }
    
    @Override
    public WorldGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new WorldGenerator();
    }
}
