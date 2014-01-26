package vc.pvp.skywars.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vc.pvp.skywars.controllers.PlayerController;
import vc.pvp.skywars.player.GamePlayer;
import vc.pvp.skywars.utilities.Messaging;

public class ScoreCommand implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String labal,
			String[] args) {
		if (cmd.getName().equalsIgnoreCase("score")) {
			Player player = (Player) sender;
			GamePlayer gamePlayer = PlayerController.get().get(player);
			sender.sendMessage(new Messaging.MessageFormatter().setVariable(
					"value", String.valueOf(gamePlayer.getScore())).format(
					"success.score"));
			return true;
		} else {
			return false;
		}
	}
}