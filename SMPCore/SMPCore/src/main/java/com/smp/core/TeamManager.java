package com.smp.core;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TeamManager {
    private final SMPCore plugin;
    private final File file;
    private final Map<String, Team> teams = new LinkedHashMap<>();   // kucuk harf isim -> takim
    private final Map<UUID, String> byPlayer = new HashMap<>();
    private final Map<UUID, String> invites = new HashMap<>();        // davetli -> takim anahtari

    public TeamManager(SMPCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "teams.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("teams");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            String name = sec.getString(key + ".name", key);
            UUID owner = UUID.fromString(sec.getString(key + ".owner"));
            Team t = new Team(name, owner);
            for (String m : sec.getStringList(key + ".members")) {
                UUID id = UUID.fromString(m);
                t.members.add(id);
            }
            teams.put(key, t);
            for (UUID id : t.members) byPlayer.put(id, key);
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Team> e : teams.entrySet()) {
            Team t = e.getValue();
            String p = "teams." + e.getKey();
            y.set(p + ".name", t.name);
            y.set(p + ".owner", t.owner.toString());
            List<String> list = new ArrayList<>();
            for (UUID id : t.members) list.add(id.toString());
            y.set(p + ".members", list);
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("teams.yml kaydedilemedi: " + ex.getMessage());
        }
    }

    public Team getTeam(UUID player) {
        String k = byPlayer.get(player);
        return k == null ? null : teams.get(k);
    }

    public Team getByName(String name) {
        return teams.get(name.toLowerCase());
    }

    public Collection<Team> all() {
        return teams.values();
    }

    public boolean sameTeam(UUID a, UUID b) {
        String ta = byPlayer.get(a);
        return ta != null && ta.equals(byPlayer.get(b));
    }

    public Team create(String name, UUID owner) {
        Team t = new Team(name, owner);
        teams.put(name.toLowerCase(), t);
        byPlayer.put(owner, name.toLowerCase());
        save();
        return t;
    }

    public void invite(UUID target, Team t) {
        invites.put(target, t.name.toLowerCase());
    }

    public Team popInvite(UUID target) {
        String k = invites.remove(target);
        return k == null ? null : teams.get(k);
    }

    public void join(Team t, UUID player) {
        t.members.add(player);
        byPlayer.put(player, t.name.toLowerCase());
        save();
    }

    /** Oyuncuyu takimdan cikarir. Lider cikarsa liderlik devredilir, kimse kalmazsa takim silinir. */
    public void remove(Team t, UUID player) {
        t.members.remove(player);
        byPlayer.remove(player);
        if (t.members.isEmpty()) {
            teams.remove(t.name.toLowerCase());
        } else if (t.owner.equals(player)) {
            t.owner = t.members.iterator().next();
        }
        save();
    }

    public void disband(Team t) {
        for (UUID id : t.members) byPlayer.remove(id);
        teams.remove(t.name.toLowerCase());
        save();
    }
}
