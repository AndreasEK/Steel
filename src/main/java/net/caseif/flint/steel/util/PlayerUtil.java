/*
 * New BSD License (BSD-new)
 *
 * Copyright (c) 2015 Maxim Roncacé
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the name of the copyright holder nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.caseif.flint.steel.util;

import net.caseif.flint.steel.SteelMain;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;

/**
 * Utility methods regarding players.
 *
 * @author Max Roncac�
 */
public class PlayerUtil {

    private static final String PLAYER_INVENTORY_PRIMARY_KEY = "primary";
    private static final String PLAYER_INVENTORY_ARMOR_KEY = "armor";
    private static final String PLAYER_INVENTORY_STORAGE_DIR = "inventories";

    /**
     * Pushes the inventory of the given player into persistent storage.
     *
     * @param player The {@link Player} to push the inventory of
     * @throws IllegalStateException If the inventory of the given
     *     {@link Player} is already present in persistent storage
     * @throws IOException If an exception occurs while saving into persistent
     *     storage
     */
    public static void pushInventory(Player player) throws IllegalStateException, IOException {
        PlayerInventory inv = player.getInventory();
        // the file to store the inventory in
        File storage = new File(SteelMain.getPlugin().getDataFolder(),
                PLAYER_INVENTORY_STORAGE_DIR + File.pathSeparatorChar + player.getUniqueId() + ".yml");
        if (storage.exists()) { // verify file isn't already present on disk (meaning it wasn't popped the last time)
            throw new IllegalStateException("Inventory push requested for player " + player.getName() + ", but "
                    + "inventory was already present in persistent storage!");
        }
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection mainInv = yaml.createSection(PLAYER_INVENTORY_PRIMARY_KEY); // section for the main inv
        mainInv.set("capacity", inv.getContents().length);
        for (int i = 0; i < inv.getContents().length; i++) {
            mainInv.set(Integer.toString(i), inv.getContents()[i]);
        }
        ConfigurationSection armorInv = yaml.createSection(PLAYER_INVENTORY_ARMOR_KEY); // section for the armor inv
        armorInv.set("capacity", inv.getArmorContents().length);
        for (int i = 0; i < inv.getArmorContents().length; i++) {
            armorInv.set(Integer.toString(i), inv.getArmorContents()[i]);
        }
        yaml.save(storage); // save to disk
        inv.clear(); // clear the inventory to complete the push to disk
    }

    /**
     * Pops the inventory of the given player from persistent storage.
     *
     * @param player The {@link Player} to pop the inventory of
     * @throws IllegalArgumentException If the inventory of the given
     *     {@link Player} is not present in persistent storage
     * @throws IOException If an exception occurs while loading from persistent
     *     storage
     * @throws InvalidConfigurationException If the stored inventory is invalid
     */
    public static void popInventory(Player player) throws IllegalArgumentException, IOException,
            InvalidConfigurationException {
        // the file to load the inventory from
        File storage = new File(SteelMain.getPlugin().getDataFolder(),
                PLAYER_INVENTORY_STORAGE_DIR + File.pathSeparatorChar + player.getUniqueId() + ".yml");
        if (!storage.exists()) { // verify file is present on disk
            throw new IllegalStateException("Inventory pop requested for player " + player.getName() + ", but "
                    + "inventory was not present in persistent storage!");
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(storage); // load from disk
        if (!yaml.contains(PLAYER_INVENTORY_PRIMARY_KEY)) {
            throw new InvalidConfigurationException("Stored inventory is missing required section \""
                    + PLAYER_INVENTORY_PRIMARY_KEY + "\"");
        }
        ItemStack[] contents;
        ItemStack[] armor = null;
        {
            ConfigurationSection mainInv = yaml.getConfigurationSection(PLAYER_INVENTORY_PRIMARY_KEY);
            if (!mainInv.contains("capacity")) {
                throw new InvalidConfigurationException("Section \"" + PLAYER_INVENTORY_PRIMARY_KEY + "\" in stored "
                        + "inventory is missing required element \"capacity\"");
            }
            int capacity = mainInv.getInt("capacity");
            contents = new ItemStack[capacity];
            for (int i = 0; i < capacity; i++) {
                if (mainInv.contains(Integer.toString(i))) {
                    contents[i] = mainInv.getItemStack(Integer.toString(i));
                }
            }
        }
        {
            if (yaml.contains("armor")) {
                ConfigurationSection armorInv = yaml.getConfigurationSection(PLAYER_INVENTORY_ARMOR_KEY);
                if (!armorInv.contains("capacity")) {
                    throw new InvalidConfigurationException("Section \"" + PLAYER_INVENTORY_PRIMARY_KEY + "\" in "
                            + "stored inventory is missing required element \"capacity\"");
                }
                int capacity = armorInv.getInt("capacity");
                armor = new ItemStack[capacity];
                for (int i = 0; i < capacity; i++) {
                    if (armorInv.contains(Integer.toString(i))) {
                        armor[i] = armorInv.getItemStack(Integer.toString(i));
                    }
                }
            }
        }
        player.getInventory().clear();
        player.getInventory().setContents(contents);
        if (armor != null) {
            player.getInventory().setArmorContents(armor);
        }
        //noinspection ResultOfMethodCallIgnored
        storage.delete();
    }

}