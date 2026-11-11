package com.smp.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ItemManager {
    private final SMPCore plugin;
    private final NamespacedKey key;

    public ItemManager(SMPCore plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "ozel_id");
    }

    public Set<String> ids() {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("items");
        return s == null ? Set.of() : s.getKeys(false);
    }

    public ConfigurationSection section(String id) {
        return plugin.getConfig().getConfigurationSection("items." + id);
    }

    private Component color(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s)
                .decoration(TextDecoration.ITALIC, false);
    }

    public ItemStack create(String id) {
        ConfigurationSection s = section(id);
        if (s == null) return null;
        Material mat = Material.matchMaterial(s.getString("material", "STONE"));
        if (mat == null) return null;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(color(s.getString("name", id)));

        List<Component> lore = new ArrayList<>();
        for (String line : s.getStringList("lore")) lore.add(color(line));
        meta.lore(lore);

        ConfigurationSection ench = s.getConfigurationSection("enchants");
        if (ench != null) {
            for (String name : ench.getKeys(false)) {
                Enchantment e = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name.toLowerCase()));
                if (e == null) {
                    plugin.getLogger().warning("Bilinmeyen büyü: " + name + " (" + id + ")");
                    continue;
                }
                meta.addEnchant(e, ench.getInt(name), true); // true = normal sinirin ustune cikabilir
            }
        }
        meta.setUnbreakable(s.getBoolean("unbreakable", false));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    /** Esya ozel bir esyaysa id'sini dondurur, degilse null. */
    public String getId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}
