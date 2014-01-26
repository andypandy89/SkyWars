package vc.pvp.skywars.controllers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vc.pvp.skywars.SkyWars;
import vc.pvp.skywars.game.GameState;
import vc.pvp.skywars.player.GamePlayer;
import vc.pvp.skywars.utilities.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class KitController {

    private static final String PERMISSION_NODE = "skywars.kit.";
    private static final int INVENTORY_SLOTS_PER_ROW = 9;
    private static final int MAX_INVENTORY_SIZE = 54;
    private static final String KIT_MENU_NAME = new Messaging.MessageFormatter().format("kit.window-title");

    private static KitController instance;
    private final Map<String, Kit> kitMap = Maps.newHashMap();

    public KitController() {
        load();
    }

    public void load() {
        kitMap.clear();
        File dataDirectory = SkyWars.get().getDataFolder();
        File kitsDirectory = new File(dataDirectory, "kits");

        if (!kitsDirectory.exists()) {
            if (!kitsDirectory.mkdirs())  {
                return;
            }


            FileUtils.saveResource(SkyWars.get(), "example.yml", new File(kitsDirectory, "Example.yml"), false);
        }

        File[] kits = kitsDirectory.listFiles();
        if (kits == null) {
            return;
        }

        for (File kit : kits) {
            if (!kit.getName().endsWith(".yml")) {
                continue;
            }

            String name = kit.getName().replace(".yml", "");

            if (!name.isEmpty() && !kitMap.containsKey(name)) {
                kitMap.put(name, new Kit(name, YamlConfiguration.loadConfiguration(kit)));
            }
        }

        LogUtils.log(Level.INFO, getClass(), "Registered %d kits ...", kitMap.size());
    }

    public boolean hasPermission(Player player, Kit kit) {
        return player.isOp() || player.hasPermission(PERMISSION_NODE + kit.getName().toLowerCase());
    }

    public boolean isPurchaseAble(Kit kit) {
        return kit.getPoints() > 0 || kit.getMoney() > 0;
    }

    public boolean canPurchase(GamePlayer gamePlayer, Kit kit) {
    	if (kit.getPoints() > 0) {
    		if (gamePlayer.getScore() < kit.getPoints()) {
    			return false;
    		}
    	}
    	if (kit.getMoney() > 0) {
    		if (SkyWars.getEconomy() != null && !SkyWars.getEconomy().has(gamePlayer.getName(), kit.getMoney())) {
    			return false;
    		}
    	}
    	return isPurchaseAble(kit);
    }

    public void populateInventory(Inventory inventory, Kit kit) {
        for (ItemStack itemStack : kit.getItems()) {
            inventory.addItem(itemStack);
        }
    }

    public Kit getByName(String name) {
        return kitMap.get(name);
    }

    public void openKitMenu(final GamePlayer gamePlayer) {
        List<Kit> availableKits = Lists.newArrayList(kitMap.values());

        int rowCount = INVENTORY_SLOTS_PER_ROW;
        while (rowCount < availableKits.size() && rowCount < MAX_INVENTORY_SIZE) {
            rowCount += INVENTORY_SLOTS_PER_ROW;
        }

        IconMenuController.get().create(gamePlayer.getBukkitPlayer(), KIT_MENU_NAME, rowCount, new IconMenu.OptionClickEventHandler() {
            @Override
            public void onOptionClick(IconMenu.OptionClickEvent event) {
                if (!gamePlayer.isPlaying()) {
                    event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("error.not-in-game"));
                    return;
                }

                if (gamePlayer.getGame().getState() != GameState.WAITING) {
                    event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("error.can-not-pick-kit"));
                    return;
                }

                Kit kit = KitController.get().getByName(ChatColor.stripColor(event.getName()));
                if (kit == null) {
                    return;
                }

                if (isPurchaseAble(kit)) {
                    if (!canPurchase(gamePlayer, kit)) {
                        event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("error.not-enough-score"));
                        return;
                    }

                    gamePlayer.setScore(gamePlayer.getScore() - kit.getPoints());
                    if (SkyWars.getEconomy() != null) {
                    	SkyWars.getEconomy().withdrawPlayer(gamePlayer.getName(), kit.getMoney());
                    }

                } else if (!hasPermission(event.getPlayer(), kit)) {
                    event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("error.no-permission-kit"));
                    return;
                }

                event.setWillClose(true);
                event.setWillDestroy(true);

                populateInventory(event.getPlayer().getInventory(), kit);
                gamePlayer.setChosenKit(true);

                event.getPlayer().sendMessage(new Messaging.MessageFormatter().format("success.enjoy-kit"));
            }
        });

        for (int iii = 0; iii < availableKits.size(); iii ++) {
            if (iii >= MAX_INVENTORY_SIZE) {
                break;
            }

            Kit kit = availableKits.get(iii);
            List<String> loreList = Lists.newLinkedList();
            boolean canPurchase = false;

            if (isPurchaseAble(kit)) {
                loreList.add("\247r\2476Point Price\247f: \247" + (gamePlayer.getScore() >= kit.getPoints() ? 'a' : 'c') + kit.getPoints());
                if (SkyWars.getEconomy() != null) {
                	loreList.add("\247r\2476Money Price\247f: \247" + (SkyWars.getEconomy().has(gamePlayer.getName(), kit.getMoney()) ? 'a' : 'c') + SkyWars.getEconomy().format(kit.getMoney()));
                }
                loreList.add(" ");

                if (canPurchase(gamePlayer, kit)) {
                    canPurchase = true;
                }

            } else if (!hasPermission(gamePlayer.getBukkitPlayer(), kit)) {
                loreList.add(new Messaging.MessageFormatter().format("kit.lores.no-permission"));
                loreList.add(" ");

            } else {
                canPurchase = true;
            }

            loreList.addAll(kit.getLores());

            IconMenuController.get().setOption(
                    gamePlayer.getBukkitPlayer(),
                    iii,
                    kit.getIcon(),
                    "\247r\247" + (canPurchase ? 'a' : 'c') + kit.getName(),
                    loreList.toArray(new String[loreList.size()]));
        }

        IconMenuController.get().show(gamePlayer.getBukkitPlayer());
    }

    public class Kit {

        private String name;
        private int money;
        private int points;
        private List<ItemStack> items = Lists.newArrayList();

        private ItemStack icon;
        private List<String> lores;

        public Kit(String name, FileConfiguration storage) {
            this.name = name;

            for (String item : storage.getStringList("items")) {
                ItemStack itemStack = ItemUtils.parseItem(item.split(" "));

                if (itemStack != null) {
                    items.add(itemStack);
                } else {
                    LogUtils.log(Level.WARNING, getClass(), "Invalid item in kit: " + item);    
                }
            }

            money = storage.getInt("money", 0);
            points = storage.getInt("points", 0);

            String icon = storage.getString("icon.material", "STONE");
            short data = (short) storage.getInt("icon.data", 0);
            ItemStack stack = ItemUtils.parseItem(icon + ":" + data);
            if (stack == null) {
                stack = new ItemStack(Material.STONE, 1, data);
            }
            this.icon = stack;

            lores = Lists.newLinkedList();
            if (storage.contains("details")) {
                for (String string : storage.getStringList("details")) {
                    lores.add("\247r" + ChatColor.translateAlternateColorCodes('&', string));
                }
            }

            lores.add("\247r\247eContents\247f:");
            for (ItemStack itemStack : items) {
                lores.add("\247r\247c" + ItemUtils.itemName(itemStack));
            }
        }

        public Collection<ItemStack> getItems() {
            return items;
        }

        public String getName() {
            return name;
        }

        public int getMoney() {
            return money;
        }

        public int getPoints() {
            return points;
        }

        public ItemStack getIcon() {
            return icon;
        }

        public List<String> getLores() {
            return lores;
        }
    }

    public static KitController get() {
        if (instance == null) {
            instance = new KitController();
        }

        return instance;
    }
}
