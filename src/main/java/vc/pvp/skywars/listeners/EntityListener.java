package vc.pvp.skywars.listeners;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.google.common.collect.Maps;

import vc.pvp.skywars.SkyWars;
import vc.pvp.skywars.controllers.PlayerController;
import vc.pvp.skywars.game.Game;
import vc.pvp.skywars.game.GameState;
import vc.pvp.skywars.player.GamePlayer;

public class EntityListener implements Listener {
	
	private final Map<String, String> hits = Maps.newHashMap();

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player player = (Player) event.getEntity();
        GamePlayer gamePlayer = PlayerController.get().get(player);

        if (!gamePlayer.isPlaying()) {
            return;
        }

        Game game = gamePlayer.getGame();

        if (game.getState() == GameState.WAITING) {
            event.setCancelled(true);
        } else if (event.getCause() == EntityDamageEvent.DamageCause.FALL && gamePlayer.shouldSkipFallDamage()) {
            gamePlayer.setSkipFallDamage(false);
            event.setCancelled(true);
        } else if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            player.setFallDistance(0F);
            event.setCancelled(true);
            gamePlayer.getGame().onPlayerDeath(gamePlayer, null);
        }
        
        if (event instanceof EntityDamageByEntityEvent) {
        	EntityDamageByEntityEvent eEvent = (EntityDamageByEntityEvent) event;
        	if (eEvent.getDamager() instanceof Projectile) {
        		LivingEntity entity = ((Projectile)eEvent.getDamager()).getShooter();
        		if (entity.getType() == EntityType.PLAYER) {
            		hits.put(gamePlayer.getName(), ((Player) entity).getName() + ":" + eEvent.getDamager().getType().name());
        		}
        	} else if (eEvent.getDamager() instanceof Player) {
        		hits.put(gamePlayer.getName(), ((Player) eEvent.getDamager()).getName());
        	}
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        Player player = event.getEntity();
        final GamePlayer gamePlayer = PlayerController.get().get(player);

        if (!gamePlayer.isPlaying()) {
            return;
        }

        if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            Bukkit.getScheduler().runTaskLater(SkyWars.get(), new Runnable() {
                @Override
                public void run() {
                    gamePlayer.getGame().onPlayerDeath(gamePlayer, event, hits.get(gamePlayer.getName()));
                    hits.remove(gamePlayer.getName());
                }
            }, 1L);
        } else {
            gamePlayer.getGame().onPlayerDeath(gamePlayer, event, hits.get(gamePlayer.getName()));
            hits.remove(gamePlayer.getName());
        }
    }
}
