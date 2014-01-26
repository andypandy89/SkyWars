package vc.pvp.skywars.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import vc.pvp.skywars.controllers.PlayerController;
import vc.pvp.skywars.game.GameState;
import vc.pvp.skywars.player.GamePlayer;

public class BlockListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerController.get().get(player);

        if (gamePlayer.isPlaying() && gamePlayer.getGame().getState() == GameState.PLAYING) {
        	gamePlayer.getGame().getScoreboardController().addPlaced();
        } else if (gamePlayer.isPlaying()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerController.get().get(player);

        if (gamePlayer.isPlaying() && gamePlayer.getGame().getState() == GameState.PLAYING) {
        	gamePlayer.getGame().getScoreboardController().addBroken();
        } else if (gamePlayer.isPlaying()) {
            event.setCancelled(true);
        }
    }
}
