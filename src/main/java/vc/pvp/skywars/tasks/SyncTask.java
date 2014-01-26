package vc.pvp.skywars.tasks;

import vc.pvp.skywars.controllers.GameController;
import vc.pvp.skywars.game.Game;

public class SyncTask implements Runnable {
    @Override
    public void run() {
        for (Game game : GameController.get().getAll()) {
            game.onTick();
        }
    }
}
