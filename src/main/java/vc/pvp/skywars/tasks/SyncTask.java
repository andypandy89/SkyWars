package vc.pvp.skywars.tasks;

import vc.pvp.skywars.controllers.GameController;
import vc.pvp.skywars.game.Game;

@SuppressWarnings("unused")
public class SyncTask implements Runnable {

	private int tickCounter;

    @Override
    public void run() {
        for (Game game : GameController.get().getAll()) {
            game.onTick();
        }

        // @TODO
        /*if (tickCounter++ == 10) {
            if (SkyWars.getDB() != null) {
                SkyWars.getDB().checkConnection();
            }
            tickCounter = 0;
        }*/
    }
}
