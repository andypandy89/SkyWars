package vc.pvp.skywars.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import vc.pvp.skywars.SkyWars;
import vc.pvp.skywars.config.PluginConfig;
import vc.pvp.skywars.controllers.GameController;
import vc.pvp.skywars.controllers.PlayerController;
import vc.pvp.skywars.controllers.SchematicController;
import vc.pvp.skywars.controllers.GlobalScoreboardController;
import vc.pvp.skywars.game.Game;
import vc.pvp.skywars.game.GameState;
import vc.pvp.skywars.player.GamePlayer;
import vc.pvp.skywars.utilities.Messaging;
import vc.pvp.skywars.utilities.StringUtils;

import java.util.Iterator;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerController.get().register(event.getPlayer());
        GlobalScoreboardController.get().addPlayerToScoreboard(event.getPlayer());
        GlobalScoreboardController.get().joinScoreboard(event.getPlayer());
        GlobalScoreboardController.get().updatePlayers();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerController.get().get(player);

        if (gamePlayer.isPlaying()) {
            gamePlayer.getGame().onPlayerLeave(gamePlayer);
        }

        GlobalScoreboardController.get().updatePlayers();
        gamePlayer.save();
        PlayerController.get().unregister(player);
        (GlobalScoreboardController.get().getScoreboards().get(event.getPlayer().getName())).getObjective("skywars").unregister();
        GlobalScoreboardController.get().getScoreboards().remove(player.getName());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        final GamePlayer gamePlayer = PlayerController.get().get(player);

        if (gamePlayer.isPlaying()) {
            event.setRespawnLocation(PluginConfig.getLobbySpawn());

            if (PluginConfig.saveInventory()) {
                Bukkit.getScheduler().runTaskLater(SkyWars.get(), new Runnable() {
                    @Override
                    public void run() {
                        gamePlayer.restoreState();
                    }
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerController.get().get(player);

        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock().getType() == Material.STONE_PLATE) {
            if (player.getWorld() != PluginConfig.getLobbySpawn().getWorld()) {
                return;
            }
            if (PluginConfig.getLobbyRadius() != 0) {
                if (player.getLocation().distance(PluginConfig.getLobbySpawn()) > PluginConfig.getLobbyRadius()) {
                    return;
                }
            }
            if (!gamePlayer.isPlaying()) {
                if (SchematicController.get().size() == 0) {
                    player.sendMessage(new Messaging.MessageFormatter().format("error.no-schematics"));
                    return;
                }

                Game game = GameController.get().findEmpty();
                game.onPlayerJoin(gamePlayer);
            }

            return;
        }

        if (gamePlayer.isPlaying() && gamePlayer.getGame().getState() != GameState.PLAYING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerController.get().get(player);

        if (PluginConfig.chatHandledByOtherPlugin()) {
            event.setFormat(event.getFormat().replace("[score]", String.valueOf(gamePlayer.getScore())));

            if (gamePlayer.isPlaying()) {
                for (Iterator<Player> iterator = event.getRecipients().iterator(); iterator.hasNext();) {
                    GamePlayer gp = PlayerController.get().get(iterator.next());

                    if (!gp.isPlaying() || !gp.getGame().equals(gamePlayer.getGame())) {
                        iterator.remove();
                    }
                }

            } else {
                for (Iterator<Player> iterator = event.getRecipients().iterator(); iterator.hasNext();) {
                    GamePlayer gp = PlayerController.get().get(iterator.next());

                    if (gp.isPlaying()) {
                        iterator.remove();
                    }
                }
            }

            return;
        }
        
        String prefix = null;
        if (SkyWars.getChat() != null) {
            prefix = SkyWars.getChat().getPlayerPrefix(player);
        }
        if (prefix == null) {
            prefix = "";
        }
        String message = new Messaging.MessageFormatter()
                .setVariable("score", StringUtils.formatScore(gamePlayer.getScore()))
                .setVariable("player", player.getDisplayName())
                .setVariable("message", Messaging.stripColor(event.getMessage()))
                .setVariable("prefix", prefix)
                .format("chat.local");
        event.setCancelled(true);

        if (gamePlayer.isPlaying()) {
            gamePlayer.getGame().sendMessage(message);

        } else {
            for (GamePlayer gp : PlayerController.get().getAll()) {
                if (!gp.isPlaying()) {
                    gp.getBukkitPlayer().sendMessage(message);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerController.get().get(player);

        if (gamePlayer.isPlaying()) {
            String command = event.getMessage().split(" ")[0].toLowerCase();

            if (!command.equals("/sw") && !PluginConfig.isCommandWhitelisted(command)) {
                event.setCancelled(true);
                player.sendMessage( new Messaging.MessageFormatter().withPrefix().format("error.cmd-disabled"));
            }
        }
    }
}
