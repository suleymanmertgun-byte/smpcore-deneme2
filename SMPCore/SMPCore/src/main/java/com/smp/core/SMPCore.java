package com.smp.core;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SMPCore extends JavaPlugin implements Listener {
    private TeamManager teams;
    private ItemManager items;
    private MerinyumManager merinyum;
    private MerinyumPopulator populator;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        teams = new TeamManager(this);
        items = new ItemManager(this);
        merinyum = new MerinyumManager(this);
        populator = new MerinyumPopulator(this);

        TeamCommand tc = new TeamCommand(this, teams);
        getCommand("takim").setExecutor(tc);
        getCommand("takim").setTabCompleter(tc);
        getCommand("tc").setExecutor(tc);

        OzelCommand oc = new OzelCommand(items, merinyum);
        getCommand("ozel").setExecutor(oc);
        getCommand("ozel").setTabCompleter(oc);

        getServer().getPluginManager().registerEvents(new TeamListener(this, teams), this);
        getServer().getPluginManager().registerEvents(new ItemListener(items), this);
        getServer().getPluginManager().registerEvents(new MerinyumListener(this, merinyum), this);
        getServer().getPluginManager().registerEvents(new MerinyumToolListener(this, merinyum), this);
        getServer().getPluginManager().registerEvents(this, this);

        // Zaten yuklu olan dunyalara damar uretecini ekle
        for (World w : Bukkit.getWorlds()) addPopulator(w);
        getLogger().info("SMPCore aktif!");
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent e) {
        addPopulator(e.getWorld());
    }

    private void addPopulator(World w) {
        if (w.getEnvironment() == World.Environment.NORMAL && !w.getPopulators().contains(populator)) {
            w.getPopulators().add(populator);
        }
    }

    @Override
    public void onDisable() {
        if (teams != null) teams.save();
        if (merinyum != null) merinyum.unregister();
    }
}
