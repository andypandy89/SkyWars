package vc.pvp.skywars.controllers;

import com.google.common.collect.Maps;

import vc.pvp.skywars.SkyWars;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class StatisticsController {

    private static StatisticsController statisticsController;
    private Map<String, Integer> topList = Maps.newLinkedHashMap();
	private List<TopThreeStatue> topThreeStatueList;

    public StatisticsController() {

    }

    public void setTopList(Map<String, Integer> topList) {
        this.topList = topList;
    }

    public void update() {

    }

    public class StatisticsWall {

        private int minX;
        private int minY;
        private int minZ;

        private int maxX;
        private int maxY;
        private int maxZ;

    }

    public class TopThreeStatue {

        public void update() {

        }

    }

    public static StatisticsController get() {
        if (statisticsController == null && SkyWars.getDB() != null) {
            statisticsController = new StatisticsController();
        }

        return statisticsController;
    }
}
