package vc.pvp.skywars.controllers;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import vc.pvp.skywars.SkyWars;
import vc.pvp.skywars.game.Game;
import vc.pvp.skywars.player.GamePlayer;

import com.google.common.collect.Maps;

public class GameScoreboardController {

	private final Game game;

	private Map<String, Scoreboard> scoreboards = Maps.newHashMap();
	private int broken;
	private int placed;

	public GameScoreboardController(Game game) {
		this.game = game;
	}

	public void addBroken() {
		++this.broken;
		this.updateBroken();
	}

	public void addPlaced() {
		++this.placed;
		this.updatePlaced();
	}

	public void registerScoreboard() {
		for (GamePlayer gPlayer : game.getPlayers()) {
			if (gPlayer == null) {
				continue;
			}
			Scoreboard scoreboard = Bukkit.getServer().getScoreboardManager()
					.getNewScoreboard();
			Objective objective = scoreboard.registerNewObjective("info",
					"dummy");
			objective.setDisplayName("\247c " + ChatColor.BOLD + "Stats");
			objective.setDisplaySlot(DisplaySlot.SIDEBAR);

			scoreboards.put(gPlayer.getName(), scoreboard);
		}
	}

	public void removeFromScoreboards(Player player) {
		Scoreboard scoreboard = scoreboards.get(player.getName());
		if (scoreboard == null) {
			return;
		}
		for (Objective objective : scoreboard.getObjectives()) {
			objective.unregister();
		}
		scoreboards.remove(player.getName());
	}

	public void setScoreboard(Player player) {
		player.setScoreboard(scoreboards.get(player.getName()));
	}

	public void unregisterScoreboard() {
		for (Scoreboard board : scoreboards.values()) {
			for (Objective objective : board.getObjectives()) {
				objective.unregister();
			}
			board.clearSlot(DisplaySlot.SIDEBAR);
		}
	}

	private void updateBroken(Player player) {
		Objective objective = (scoreboards.get(player.getName()))
				.getObjective("info");
		objective.getScore(
				Bukkit.getOfflinePlayer(ChatColor.DARK_AQUA + "Broken"))
				.setScore(this.broken);
	}

	private void updateBroken() {
		for (GamePlayer gPlayer : game.getPlayers()) {
			updateBroken(gPlayer.getBukkitPlayer());
		}
	}

	private void updateMoney(Player p) {
		Objective objective = (scoreboards.get(p.getName()))
				.getObjective("info");
		objective.getScore(Bukkit.getOfflinePlayer(ChatColor.GREEN + "Money"))
				.setScore((int) SkyWars.getEconomy().getBalance(p.getName()));
	}

	private void updatePlaced(Player p) {
		Objective objective = (scoreboards.get(p.getName()))
				.getObjective("info");
		objective.getScore(Bukkit.getOfflinePlayer(ChatColor.BLUE + "Placed"))
				.setScore(this.placed);
	}

	private void updatePlaced() {
		for (GamePlayer gPlayer : game.getPlayers()) {
			updatePlaced(gPlayer.getBukkitPlayer());
		}
	}

	private void updateScore(Player p) {
		Objective objective = (scoreboards.get(p.getName()))
				.getObjective("info");
		GamePlayer gp = PlayerController.get().get(p);
		objective.getScore(Bukkit.getOfflinePlayer(ChatColor.GOLD + "Score"))
				.setScore(gp.getScore());
	}

	public void updateScoreboard() {
		for (GamePlayer gPlayer : game.getPlayers()) {
			if (gPlayer == null) {
				continue;
			}
			Player player = gPlayer.getBukkitPlayer();
			Objective objective = ((Scoreboard) this.scoreboards.get(player
					.getName())).getObjective("info");
			updateScore(player);
			updateMoney(player);
			objective.getScore(
					Bukkit.getOfflinePlayer(ChatColor.RED + "Players Left"))
					.setScore(game.getPlayerCount());
			updatePlaced(player);
			updateBroken(player);
		}
	}
}
