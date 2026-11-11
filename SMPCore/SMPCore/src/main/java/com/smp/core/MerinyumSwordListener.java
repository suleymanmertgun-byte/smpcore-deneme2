package com.smp.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Merinyum Kilic:
 *  - her oldurmede can fullenir
 *  - her 5 oldurmede kalici +1 HP (yarim kalp), ust sinir config'te
 */
public class MerinyumSwordListener implements Listener {
    private final SMPCore plugin;
    private final MerinyumManager mm;
    private final NamespacedKey killsKey;
    private final NamespacedKey bonusKey;
    private final NamespacedKey modKey;

    public MerinyumSwordListener(SMPCore plugin, MerinyumManager mm) {
        this.plugin = plugin;
        this.mm = mm;
        this.killsKey = new NamespacedKey(plugin, "merinyum_kills");
        this.bonusKey = new NamespacedKey(plugin, "merinyum_bonus_hp");
        this.modKey = new NamespacedKey(plugin, "merinyum_bonus_hp_mod");
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead instanceof ArmorStand) return;
        Player p = dead.getKiller();
        if (p == null || p.equals(dead)) return;
        if (!mm.is(p.getInventory().getItemInMainHand(), "merinyum_kilic")) return;

        PersistentDataContainer pdc = p.getPersistentDataContainer();
        int kills = pdc.getOrDefault(killsKey, PersistentDataType.INTEGER, 0) + 1;
        pdc.set(killsKey, PersistentDataType.INTEGER, kills);

        int per = Math.max(1, plugin.getConfig().getInt("merinyum.kills-per-bonus", 5));
        if (kills % per == 0) {
            double bonus = pdc.getOrDefault(bonusKey, PersistentDataType.DOUBLE, 0.0);
            double max = plugin.getConfig().getDouble("merinyum.max-bonus-hp", 40.0);
            double add = plugin.getConfig().getDouble("merinyum.bonus-hp", 1.0);
            if (bonus < max) {
                pdc.set(bonusKey, PersistentDataType.DOUBLE, Math.min(max, bonus + add));
                p.sendActionBar(Component.text("+½ kalp kazandın!", NamedTextColor.RED));
            }
        }
        applyBonus(p);

        // Her oldurmede can fullenir
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20.0 : max.getValue());
    }

    /** Kayitli bonus cani oyuncuya uygular (giriste ve yeniden dogunca da). */
    private void applyBonus(Player p) {
        AttributeInstance inst = p.getAttribute(Attribute.MAX_HEALTH);
        if (inst == null) return;
        for (AttributeModifier m : inst.getModifiers().toArray(new AttributeModifier[0]))
            if (m.getKey().equals(modKey)) inst.removeModifier(m);
        double bonus = p.getPersistentDataContainer().getOrDefault(bonusKey, PersistentDataType.DOUBLE, 0.0);
        if (bonus > 0) {
            inst.addModifier(new AttributeModifier(modKey, bonus,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        applyBonus(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> applyBonus(p));
    }
}
