package vc.pvp.skywars.controllers;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import com.google.common.collect.Maps;

import vc.pvp.skywars.SkyWars;
import vc.pvp.skywars.game.Game;
import vc.pvp.skywars.game.GameState;
import vc.pvp.skywars.player.GamePlayer;

public class GlobalScoreboardController {
	static GlobalScoreboardController instance;

	private Map<String, Scoreboard> scoreboards = Maps.newHashMap();

	public static GlobalScoreboardController get() {
		if (instance == null) {
			instance = new GlobalScoreboardController();
			for (Player player : Bukkit.getOnlinePlayers()) {
				instance.addPlayerToScoreboard(player);
			}
			instance.updateScoreboard();
		}
		return instance;
	}

	public void addPlayerToScoreboard(Player p) {
		Scoreboard scoreboard = Bukkit.getServer().getScoreboardManager()
				.getNewScoreboard();
		Objective objective = scoreboard.registerNewObjective("skywars",
				"dummy");
		objective.setDisplayName("\247c\247lSkyWars");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		this.scoreboards.put(p.getName(), scoreboard);
	}

	public Map<String, Scoreboard> getScoreboards() {
		return this.scoreboards;
	}

	public void joinScoreboard(Player p) {
		updateActiveGames();
		updateWins(p);
		updateScore(p);
		updateMoney(p);
		p.setScoreboard(getScoreboards().get(p.getName()));
	}

	public void shutdown() {
		for (Scoreboard scoreboard : scoreboards.values()) {
			for (Objective objective : scoreboard.getObjectives()) {
				objective.unregister();
			}
			scoreboard.clearSlot(DisplaySlot.SIDEBAR);
		}
	}

	public void updateActiveGames() {
		int games = 0;
		for (Game game : GameController.get().getAll()) {
			if (game.getState().equals(GameState.PLAYING)) {
				++games;
			}
		}

		Player[] var5;
		int var4 = (var5 = Bukkit.getServer().getOnlinePlayers()).length;

		for (int var8 = 0; var8 < var4; ++var8) {
			Player var7 = var5[var8];
			Objective objective = ((Scoreboard) this.scoreboards.get(var7
					.getName())).getObjective("skywars");
			if (objective == null) {
				return;
			}

			objective.getScore(
					Bukkit.getOfflinePlayer(ChatColor.DARK_AQUA
							+ "Active Games")).setScore(games);
		}

	}

	private void updateMoney(Player p) {
		Objective objective = (scoreboards.get(p.getName()))
				.getObjective("skywars");
		if (objective != null && SkyWars.getEconomy() != null) {
			objective.getScore(
					Bukkit.getOfflinePlayer(ChatColor.GREEN + "Money"))
					.setScore(
							(int) SkyWars.getEconomy().getBalance(p.getName()));
		}
	}

	public void updatePlayers() {
		Player[] var4;
		int var3 = (var4 = Bukkit.getServer().getOnlinePlayers()).length;

		for (int var2 = 0; var2 < var3; ++var2) {
			Player p = var4[var2];
			Objective objective = ((Scoreboard) this.scoreboards.get(p
					.getName())).getObjective("skywars");
			if (objective == null) {
				return;
			}

			objective.getScore(
					Bukkit.getOfflinePlayer(ChatColor.AQUA + "Online"))
					.setScore(Bukkit.getOnlinePlayers().length);
		}

	}

	public void updateScore(Player p) {
		Objective objective = ((Scoreboard) this.scoreboards.get(p.getName()))
				.getObjective("skywars");
		if (objective != null) {
			GamePlayer gp = PlayerController.get().get(p);
			objective.getScore(
					Bukkit.getOfflinePlayer(ChatColor.GOLD + "Score"))
					.setScore(gp.getScore());
		}
	}

	public void updateScoreboard() {
		updateActiveGames();
		updatePlayers();
		for (Player p : Bukkit.getOnlinePlayers()) {
			GamePlayer gp = PlayerController.get().get(p);
			updateWins(p);
			updateScore(p);
			updateMoney(p);
			if (gp.isPlaying()
					&& gp.getGame().getState().equals(GameState.PLAYING)) {
				return;
			}

			p.setScoreboard((Scoreboard) this.scoreboards.get(p.getName()));
		}

	}

	public void updateWins(Player p) {
		Objective objective = ((Scoreboard) this.scoreboards.get(p.getName()))
				.getObjective("skywars");
		if (objective != null) {
			GamePlayer gp = PlayerController.get().get(p);
			objective.getScore(
					Bukkit.getOfflinePlayer(ChatColor.LIGHT_PURPLE + "Wins"))
					.setScore(gp.getGamesWon());
		}
	}
}
