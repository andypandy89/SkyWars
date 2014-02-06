package vc.pvp.skywars.utilities;

import com.earth2me.essentials.Enchantments;
import com.flobi.WhatIsIt.WhatIsIt;
import com.sk89q.commandbook.util.ItemUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.blocks.ItemType;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.ess3.api.IEssentials;
import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

public class ItemUtils {
    
    public static ItemStack parseItem(String input) {
        String[] item = {input};
        return parseItem(item);
    }

    /**
     * 
     * Parses the args and returns an ItemStack using various APIs 
     * 
     * @param args Format: Name:Durability Amount Enchantment1:Level,Enchantment2:Level
     * @return The ItemStack or null if not found
     */
    @SuppressWarnings("deprecation")
	public static ItemStack parseItem(String[] args) {
        if (args.length < 1) {
            return null;
        }
        String[] type = args[0].split(":", 2);
        String typeInput = type[0];
        short durability = 0;

        // Find material. Vault and Essentials will also attempt to determine durability.
        Material mat = Material.matchMaterial(typeInput);
        if (mat == null) {
            if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
                IEssentials ess = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");
                try {
                    ItemStack essStack = ess.getItemDb().get(typeInput);
                    mat = essStack.getType();
                    durability = essStack.getDurability();
                } catch (Exception ignored) {
                }
            }
        }
        if (mat == null) {
            if (Bukkit.getServer().getPluginManager().getPlugin("WorldEdit") != null) {
                ItemType itemType = ItemType.lookup(typeInput);
                if (itemType != null) {
                    mat = Material.getMaterial(itemType.name());
                }
            }
        }
        if (mat == null) {
            if (Bukkit.getServer().getPluginManager().getPlugin("Vault") != null) {
                ItemInfo itemInfo = Items.itemByString(typeInput);
                if (itemInfo != null) {
                    mat = itemInfo.material;
                    durability = itemInfo.subTypeId;
                }
            }
        }
        if (mat == null) {
            return null;
        }
        ItemStack itemStack = new ItemStack(mat);

        // If durability is specified using a colon and it hasn't been found
        if (type.length > 1 && type[1].length() > 0 && durability == 0) {
            String durabilityInput = type[1];
            try {
                durability = Short.parseShort(durabilityInput);
            } catch (NumberFormatException ignored) {
            }
            if (durability == 0) {
                if (Bukkit.getServer().getPluginManager().getPlugin("CommandBook") != null) {
                    try {
                        durability = (short) ItemUtil.matchItemData(mat.getId(), durabilityInput);
                    } catch (CommandException ignored) {
                    }
                }
            }
        }

        // Set durability
        itemStack.setDurability(durability);

        // Set amount
        int stackSize = 1;
        int maxSize = itemStack.getMaxStackSize();
        if (args.length > 1 && args[1].length() > 0) {
            try {
                stackSize = Integer.parseInt(args[1]);
                if (stackSize > maxSize) {
                    stackSize = maxSize;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        itemStack.setAmount(stackSize);

        // Process enchantments
        if (args.length > 2) {
            for (String ench : args[2].split(",")) {
                String[] enchantment = ench.split(":", 2);
                Enchantment enchType = Enchantment.getByName(enchantment[0]);
                if (enchType == null) {
                    if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
                        enchType = Enchantments.getByName(enchantment[0]);
                    }
                }
                if (enchType == null) {
                    final String enchSearchString = enchantment[0].toLowerCase().replaceAll("[_\\-]", "");
                    for (Enchantment possibleEnch : Enchantment.values()) {
                        if (possibleEnch.getName().toLowerCase().replaceAll("[_\\-]", "").equals(enchSearchString)) {
                            enchType = possibleEnch;
                            break;
                        }
                    }
                }

                // Verify that the Enchantment can be used on ItemStack
                if (enchType != null) {
                    Map<Enchantment, Integer> existingEnchantments = itemStack.getEnchantments();
                    boolean enchant = true;
                    if (enchType.canEnchantItem(itemStack)) {
                        for (Enchantment existingEnchantment : existingEnchantments.keySet()) {
                            if (enchType.conflictsWith(existingEnchantment)) {
                                enchant = false;
                                break;
                            }
                        }
                    } else {
                        enchant = false;
                    }
                    if (!enchant) {
                        continue;
                    }

                    // Enchantment level
                    int enchLevel = enchType.getStartLevel();
                    if (enchantment.length > 1 && enchantment[1].length() > 0) {
                        try {
                            enchLevel = Integer.parseInt(enchantment[1]);
                        } catch (NumberFormatException ignored) {
                        }
                        if (enchLevel < enchType.getStartLevel()) {
                            enchLevel = enchType.getStartLevel();
                        } else if (enchLevel > enchType.getMaxLevel()) {
                            enchLevel = enchType.getMaxLevel();
                        }
                    }
                    itemStack.addEnchantment(enchType, enchLevel);
                }
            }
        }

        return itemStack;
    }

    public static ItemStack name(ItemStack itemStack, String name, String... lores) {
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (!name.isEmpty()) {
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }

        if (lores.length > 0) {
            List<String> loreList = new ArrayList<String>(lores.length);

            for (String lore : lores) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', lore));
            }

            itemMeta.setLore(loreList);
        }

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
    
    /**
     * 
     * Gets a friendly name for a given ItemStack
     * 
     * @param stack The item stack
     * @return Friendly name
     */
    @SuppressWarnings("deprecation")
	public static String itemName(ItemStack stack) {
        String name = null;
        if (Bukkit.getPluginManager().getPlugin("WhatIsIt") != null) {
            name = WhatIsIt.itemName(stack);
        }
        if (name == null) {
            if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
                IEssentials ess = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");
                name = ess.getItemDb().name(stack);
            }
        }
        if (name == null) {
            if (Bukkit.getServer().getPluginManager().getPlugin("Vault") != null) {
                name = Items.itemByName(stack.toString()).getName();
            }
        }
        if (name == null) {
            if (Bukkit.getServer().getPluginManager().getPlugin("WorldEdit") != null) {
                name = ItemType.fromID(stack.getTypeId()).getName();
            }
        }
        if (name == null) {
            name = stack.getType().toString().toLowerCase();
        }
        return name;
    }
}
