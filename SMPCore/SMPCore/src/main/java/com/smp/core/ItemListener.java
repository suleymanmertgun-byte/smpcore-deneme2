package com.smp.core;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

public class ItemListener implements Listener {
    private final ItemManager items;

    public ItemListener(ItemManager items) {
        this.items = items;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;

        String id = items.getId(p.getInventory().getItemInMainHand());
        if (id == null) return;
        ConfigurationSection s = items.section(id);
        if (s == null) return;
        ConfigurationSection ab = s.getConfigurationSection("abilities");
        if (ab == null) return;

        // Can calma
        double ls = ab.getDouble("lifesteal", 0);
        if (ls > 0) {
            AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
            double maxHp = max == null ? 20 : max.getValue();
            p.setHealth(Math.min(maxHp, p.getHealth() + e.getFinalDamage() * ls));
        }

        // Yakma
        int fire = ab.getInt("fire", 0);
        if (fire > 0) victim.setFireTicks(fire * 20);

        // Yildirim
        double chance = ab.getDouble("lightning-chance", 0);
        if (chance > 0 && ThreadLocalRandom.current().nextDouble() < chance) {
            Location loc = victim.getLocation();
            victim.getWorld().strikeLightningEffect(loc);
            victim.setNoDamageTicks(0);
            victim.damage(ab.getDouble("lightning-damage", 6), p);
        }
    }
}
