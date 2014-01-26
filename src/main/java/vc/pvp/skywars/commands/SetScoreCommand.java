package vc.pvp.skywars.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vc.pvp.skywars.commands.CommandDescription;
import vc.pvp.skywars.commands.CommandPermissions;
import vc.pvp.skywars.controllers.PlayerController;
import vc.pvp.skywars.controllers.GlobalScoreboardController;
import vc.pvp.skywars.player.GamePlayer;
import vc.pvp.skywars.utilities.Messaging;

@CommandDescription("Set the score of a player.")
@CommandPermissions({"skywars.command.setscore"})
public class SetScoreCommand implements CommandExecutor {

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if(args.length <= 2) {
         sender.sendMessage(new Messaging.MessageFormatter()
         .setVariable("example", "/sw set player score")
         .format("error.not-enough-arguments"));
         return true;
      } else if(!(Bukkit.getServer().getPlayer(args[1]) instanceof Player)) {
          sender.sendMessage(new Messaging.MessageFormatter().format("error.no-valid-player"));
         return true;
      } else {
         if(isInteger(args[2])) {
            GamePlayer gp = PlayerController.get().get(Bukkit.getServer().getPlayer(args[1]));
            gp.setScore(Integer.parseInt(args[2]));
				GlobalScoreboardController.get().updateScore(gp.getBukkitPlayer());
				sender.sendMessage(new Messaging.MessageFormatter()
						.setVariable("player", args[1])
						.setVariable("value", args[2])
						.format("success.score-set"));
         } else {
            sender.sendMessage(new Messaging.MessageFormatter().format("error.no-valid-score"));
         }

         return true;
      }
   }

   private boolean isInteger(String integer) {
      try {
         Integer.parseInt(integer);
         return true;
      } catch (NumberFormatException var3) {
         return false;
      }
   }
}