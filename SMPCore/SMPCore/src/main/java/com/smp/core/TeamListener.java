package com.smp.core;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class TeamListener implements Listener {
    private final SMPCore plugin;
    private final TeamManager tm;

    public TeamListener(SMPCore plugin, TeamManager tm) {
        this.plugin = plugin;
        this.tm = tm;
    }

    // Takim arkadaslari birbirine vuramaz (config'ten kapatilabilir)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (plugin.getConfig().getBoolean("team.friendly-fire", false)) return;
        if (!(e.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        if (e.getDamager() instanceof Player p) attacker = p;
        else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player p) attacker = p;
        if (attacker == null || attacker.equals(victim)) return;
        if (tm.sameTeam(attacker.getUniqueId(), victim.getUniqueId())) e.setCancelled(true);
    }

    // Sohbette takim etiketi
    @EventHandler
    public void onChat(AsyncChatEvent e) {
        e.renderer((source, name, message, viewer) -> {
            Team t = tm.getTeam(source.getUniqueId());
            Component prefix = t == null ? Component.empty()
                    : Component.text("[" + t.name + "] ", NamedTextColor.GOLD);
            return prefix.append(name).append(Component.text(": ", NamedTextColor.GRAY)).append(message);
        });
    }
}
