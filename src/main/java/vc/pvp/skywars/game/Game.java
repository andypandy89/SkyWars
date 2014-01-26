package vc.pvp.skywars.game;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sk89q.worldedit.CuboidClipboard;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import vc.pvp.skywars.SkyWars;
import vc.pvp.skywars.config.PluginConfig;
import vc.pvp.skywars.controllers.*;
import vc.pvp.skywars.player.GamePlayer;
import vc.pvp.skywars.utilities.CraftBukkitUtil;
import vc.pvp.skywars.utilities.Messaging;
import vc.pvp.skywars.utilities.PlayerUtil;
import vc.pvp.skywars.utilities.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.util.Vector;

public class Game {

    private GameState gameState;
    private Map<Integer, GamePlayer> idPlayerMap = Maps.newLinkedHashMap();
    private Map<GamePlayer, Integer> playerIdMap = Maps.newHashMap();
    private int playerCount = 0;
    private int slots;
    private Map<Integer, Location> spawnPlaces = Maps.newHashMap();
    private int timer;
    private boolean built;
    private GameScoreboardController scoreboardController;

    private CuboidClipboard schematic;
    private World world;
    private int[] islandReference;
    private Vector minLoc;
    private Vector maxLoc;
    private List<Location> chestList = Lists.newArrayList();

    public Game(CuboidClipboard schematic) {
        this.schematic = schematic;
        world = WorldController.get().create(this, schematic);
        slots = spawnPlaces.size();
        gameState = GameState.WAITING;

        for (int iii = 0; iii < slots; iii++) {
            idPlayerMap.put(iii, null);
        }
    }

    public boolean isBuilt() {
        return built;
    }

    public void setBuilt(boolean built) {
        this.built = built;
    }

    public void setTimer(int timer) {
        this.timer = timer;
    }

    public void addChest(Location location) {
        chestList.add(location);
    }

    public void removeChest(Location location) {
        chestList.remove(location);
    }

    public boolean isChest(Location location) {
        return chestList.contains(location);
    }

    public boolean isReady() {
        return slots >= 2;
    }

    public World getWorld() {
        return world;
    }
    
    public GameScoreboardController getScoreboardController() {
    	if (scoreboardController == null) {
    		scoreboardController = new GameScoreboardController(this);
    	}
    	return scoreboardController;
    }

    public void setGridReference(int[] gridReference) {
        islandReference = gridReference;
    }

    public int[] getGridReference() {
        return islandReference;
    }
    
    public void setLocation(int midX, int midZ) {
        int minX = midX + schematic.getOffset().getBlockX() + 1;
        int minZ = midZ + schematic.getOffset().getBlockZ() + 1;
        int maxX = minX + schematic.getWidth() - 2;
        int maxZ = minZ + schematic.getLength() - 2;
        int buffer = PluginConfig.getIslandBuffer();
        minLoc = new Vector(minX - buffer, Integer.MIN_VALUE, minZ - buffer);
        maxLoc = new Vector(maxX + buffer, Integer.MAX_VALUE, maxZ + buffer);
    }
    
    public Vector getMinLoc() {
        return minLoc;
    }
    
    public Vector getMaxLoc() {
        return maxLoc;
    }

    public void onPlayerJoin(GamePlayer gamePlayer) {
        Player player = gamePlayer.getBukkitPlayer();

        int id = getFistEmpty();
        playerCount++;
        idPlayerMap.put(getFistEmpty(), gamePlayer);
        playerIdMap.put(gamePlayer, id);

        sendMessage(new Messaging.MessageFormatter()
                .withPrefix()
                .setVariable("player", player.getDisplayName())
                .setVariable("amount", String.valueOf(getPlayerCount()))
                .setVariable("slots", String.valueOf(slots))
                .format("game.join"));

        if (getMinimumPlayers() - playerCount != 0) {
            sendMessage(new Messaging.MessageFormatter()
                    .withPrefix()
                    .setVariable("amount", String.valueOf(getMinimumPlayers() - playerCount))
                    .format("game.required"));
        }

        PlayerUtil.refreshPlayer(player);

        if (PluginConfig.saveInventory()) {
            gamePlayer.saveCurrentState();
        }

        if (PluginConfig.clearInventory()) {
            PlayerUtil.clearInventory(player);
        }

        if (player.getGameMode() != GameMode.SURVIVAL) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        gamePlayer.setChosenKit(false);
        gamePlayer.setSkipFallDamage(true);
        player.teleport(getSpawn(id).clone().add(0.5, 0.5, 0.5));
        gamePlayer.setGame(this);

        KitController.get().openKitMenu(gamePlayer);

        if (!PluginConfig.buildSchematic()) {
            timer = getTimer();
        }
    }

    public void onPlayerLeave(GamePlayer gamePlayer) {
        onPlayerLeave(gamePlayer, true, true, true);
    }

    public void onPlayerLeave(final GamePlayer gamePlayer, boolean displayText, boolean process, boolean left) {
        Player player = gamePlayer.getBukkitPlayer();

        IconMenuController.get().destroy(player);

        playerCount--;
        if (displayText) {
            if (left && gameState == GameState.PLAYING) {
                int scorePerLeave = PluginConfig.getScorePerLeave(player);
                gamePlayer.addScore(scorePerLeave);
                GlobalScoreboardController.get().updateScore(player);
                sendMessage(new Messaging.MessageFormatter()
                        .withPrefix()
                        .setVariable("player", player.getDisplayName())
                        .setVariable("score", StringUtils.formatScore(scorePerLeave, Messaging.getInstance().getMessage("score.naming")))
                        .format("game.quit.playing"));

            } else {
                sendMessage(new Messaging.MessageFormatter()
                        .withPrefix()
                        .setVariable("player", player.getDisplayName())
                        .setVariable("total", String.valueOf(getPlayerCount()))
                        .setVariable("slots", String.valueOf(slots))
                        .format("game.quit.other"));
            }
        }

        if(gameState == GameState.PLAYING) {
        	getScoreboardController().updateScoreboard();
        	getScoreboardController().removeFromScoreboards(player);
        }

        if (player.isDead()) {
            gamePlayer.setGame(null);
            CraftBukkitUtil.forceRespawn(player);
        } else {
            PlayerUtil.refreshPlayer(player);
            PlayerUtil.clearInventory(player);

            gamePlayer.setGame(null);
            player.teleport(PluginConfig.getLobbySpawn());

            if (PluginConfig.saveInventory()) {
                gamePlayer.restoreState();
            }
        }
        
        player.setFireTicks(0);

        idPlayerMap.put(playerIdMap.remove(gamePlayer), null);
        gamePlayer.setChosenKit(false);
        
        player.setScoreboard(GlobalScoreboardController.get().getScoreboards().get(player.getName()));

        if (process && gameState == GameState.PLAYING && playerCount == 1) {
            onGameEnd(getWinner());
        }
    }
    
    public void onPlayerDeath(final GamePlayer gamePlayer, PlayerDeathEvent event, String killerName) {
    	final Player player = gamePlayer.getBukkitPlayer();
        Player killer = player.getKiller();

        int scorePerDeath = PluginConfig.getScorePerDeath(player);
        gamePlayer.addScore(scorePerDeath);
        gamePlayer.setDeaths(gamePlayer.getDeaths() + 1);
        
        EntityType projectile = null;
        
        if (killerName != null) {
        	if (killerName.contains(":")) {
        		String[] split = killerName.split(":");
        		killerName = split[0];
        		
        		for (EntityType type : EntityType.values()) {
        			if (type == null || type.name() == null) {
        				continue;
        			}
        			if (type.name().equals(split[1])) {
        				projectile = type;
        			}
        		}
        	}
        	if (Bukkit.getPlayer(killerName) == null) {
        		killerName = null;
        	}
        }

        if (killer != null || killerName != null) {
            GamePlayer gameKiller = killer == null ? PlayerController.get().get(Bukkit.getPlayer(killerName)) : PlayerController.get().get(killer);

            int scorePerKill = PluginConfig.getScorePerKill(killer);
            gameKiller.addScore(scorePerKill);
            gameKiller.setKills(gameKiller.getKills() + 1);

            String projectileString = "";
            
            if (projectile != null) {
            	switch (projectile) {
            	case ARROW:
            		projectileString = " shooting an arrow";
            	case FIREBALL:
            		projectileString = " casting a fireball";
            	case FISHING_HOOK:
            		projectileString = " fishing";
            	case SNOWBALL:
            		projectileString = " throwing a snowball";
        		default:
            		break;
            	}
            }
            
            sendMessage(new Messaging.MessageFormatter()
                    .withPrefix()
                    .setVariable("player", player.getDisplayName())
                    .setVariable("killer", killer.getDisplayName()+projectileString)
                    .setVariable("player_score", StringUtils.formatScore(scorePerDeath, Messaging.getInstance().getMessage("score.naming")))
                    .setVariable("killer_score", StringUtils.formatScore(scorePerKill, Messaging.getInstance().getMessage("score.naming")))
                    .format("game.kill"));

        } else {
            sendMessage(new Messaging.MessageFormatter()
                    .withPrefix()
                    .setVariable("player", player.getDisplayName())
                    .setVariable("score", StringUtils.formatScore(scorePerDeath, Messaging.getInstance().getMessage("score.naming")))
                    .format("game.death"));
        }

        sendMessage(new Messaging.MessageFormatter()
                .withPrefix()
                .setVariable("remaining", String.valueOf(playerCount - 1))
                .format("game.remaining"));

        for (GamePlayer gp : getPlayers()) {
            if (gp.equals(gamePlayer)) {
                gp.getBukkitPlayer().sendMessage(new Messaging.MessageFormatter().withPrefix().format("game.eliminated.self"));

            } else {
                gp.getBukkitPlayer().sendMessage(new Messaging.MessageFormatter()
                        .withPrefix()
                        .setVariable("player", player.getDisplayName())
                        .format("game.eliminated.others"));
            }
        }

        if (event != null) {
            Location location = player.getLocation().clone();
            World world = location.getWorld();

            for (ItemStack itemStack : event.getDrops()) {
                world.dropItemNaturally(location, itemStack);
            }

            world.spawn(location, ExperienceOrb.class).setExperience(event.getDroppedExp());

            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);

            onPlayerLeave(gamePlayer, false, true, false);

        } else {
            onPlayerLeave(gamePlayer, false, true, false);
        }
    }

    public void onPlayerDeath(final GamePlayer gamePlayer, PlayerDeathEvent event) {
        onPlayerDeath(gamePlayer, event, null);
    }

    public void onGameStart() {
        getScoreboardController().registerScoreboard();
        gameState = GameState.PLAYING;

        for (Map.Entry<Integer, GamePlayer> playerEntry : idPlayerMap.entrySet()) {
            GamePlayer gamePlayer = playerEntry.getValue();

            if (gamePlayer != null) {
                IconMenuController.get().destroy(gamePlayer.getBukkitPlayer());
                getSpawn(playerEntry.getKey()).clone().add(0, -1D, 0).getBlock().setType(Material.AIR);
                gamePlayer.setGamesPlayed(gamePlayer.getGamesPlayed() + 1);
            }
        }
        
        getScoreboardController().updateScoreboard();

        for (GamePlayer gamePlayer : getPlayers()) {
            gamePlayer.getBukkitPlayer().setHealth(20D);
            gamePlayer.getBukkitPlayer().setFoodLevel(20);
            
            getScoreboardController().setScoreboard(gamePlayer.getBukkitPlayer());
            
            gamePlayer.getBukkitPlayer().sendMessage(new Messaging.MessageFormatter().withPrefix().format("game.start"));
        }
    }

    public void onGameEnd() {
        onGameEnd(null);
    }

    public void onGameEnd(GamePlayer gamePlayer) {
        if (gamePlayer != null) {
            Player player = gamePlayer.getBukkitPlayer();
            int score = PluginConfig.getScorePerWin(player);
            gamePlayer.addScore(score);
            GlobalScoreboardController.get().updateScore(player);
            gamePlayer.setGamesWon(gamePlayer.getGamesWon() + 1);
            GlobalScoreboardController.get().updateWins(player);
            Bukkit.broadcastMessage(new Messaging.MessageFormatter()
                    .withPrefix()
                    .setVariable("player", player.getDisplayName())
                    .setVariable("score", String.valueOf( score ))
                    .setVariable("map", SchematicController.get().getName(schematic))
                    .format("game.win" ) );
        }

        GlobalScoreboardController.get().updateActiveGames();

        for (GamePlayer player : getPlayers()) {
            onPlayerLeave(player, false, false, false);
        }

        gameState = GameState.ENDING;
        getScoreboardController().unregisterScoreboard();

        GameController.get().remove(this);
    }

    public void onTick() {
        if (timer <= 0 || (gameState == GameState.WAITING && !hasReachedMinimumPlayers())) {
            return;
        }

        timer--;

        switch (gameState) {
            case WAITING:
                if (timer == 0) {
                    onGameStart();
                } else if (timer % 10 == 0 || timer <= 5) {
                	if (timer <= 5) {
                		for (GamePlayer gamePlayer : idPlayerMap.values()) {
                			if (gamePlayer == null) {
                				continue;
                			}
                			Player player = gamePlayer.getBukkitPlayer();
                			if (player == null) {
                				continue;
                			}
                			player.getWorld().playSound(player.getLocation(), 
                					Sound.SUCCESSFUL_HIT, 10, 1);
                		}
                	}
                    sendMessage(new Messaging.MessageFormatter()
                            .withPrefix()
                            .setVariable("timer", String.valueOf(timer))
                            .format("game.countdown"));
                }
                break;

            default:
                break;
        }
    }

    public GameState getState() {
        return gameState;
    }

    public boolean isFull() {
        return getPlayerCount() == slots;
    }

    public int getMinimumPlayers() {
        FileConfiguration config = SkyWars.get().getConfig();
        String schematicName = SchematicController.get().getName(schematic);

        return config.getInt("schematics." + schematicName + ".min-players", slots);
    }

    private int getTimer() {
        FileConfiguration config = SkyWars.get().getConfig();
        String schematicName = SchematicController.get().getName(schematic);

        return config.getInt("schematics." + schematicName + ".timer", 11);
    }

    public boolean hasReachedMinimumPlayers() {
        return getPlayerCount() >= getMinimumPlayers();
    }

    public void sendMessage(String message) {
        for (GamePlayer gamePlayer : getPlayers()) {
            gamePlayer.getBukkitPlayer().sendMessage(message);
        }
    }

    private GamePlayer getWinner() {
        for (GamePlayer gamePlayer : idPlayerMap.values()) {
            if (gamePlayer == null) {
                continue;
            }

            return gamePlayer;
        }

        return null;
    }

    private int getFistEmpty() {
        for (Map.Entry<Integer, GamePlayer> playerEntry : idPlayerMap.entrySet()) {
            if (playerEntry.getValue() == null) {
                return playerEntry.getKey();
            }
        }

        return -1;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public Collection<GamePlayer> getPlayers() {
        List<GamePlayer> playerList = Lists.newArrayList();

        for (GamePlayer gamePlayer : idPlayerMap.values()) {
            if (gamePlayer != null) {
                playerList.add(gamePlayer);
            }
        }

        return playerList;
    }

    private Location getSpawn(int id) {
        return spawnPlaces.get(id);
    }

    public void addSpawn(int id, Location location) {
        spawnPlaces.put(id, location);
    }
}
