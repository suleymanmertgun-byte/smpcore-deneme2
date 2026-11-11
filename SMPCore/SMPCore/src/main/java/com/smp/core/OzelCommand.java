package com.smp.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class OzelCommand implements CommandExecutor, TabCompleter {
    private final ItemManager items;
    private final MerinyumManager mm;

    public OzelCommand(ItemManager items, MerinyumManager mm) {
        this.items = items;
        this.mm = mm;
    }

    private List<String> allIds() {
        List<String> l = new ArrayList<>(MerinyumManager.IDS);
        l.addAll(items.ids());
        return l;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 0 || a[0].equalsIgnoreCase("liste")) {
            s.sendMessage(ChatColor.GOLD + "Özel eşyalar: " + ChatColor.YELLOW + String.join(", ", allIds()));
            s.sendMessage(ChatColor.GRAY + "Kullanım: /ozel ver <esya> [oyuncu] [adet]");
            return true;
        }
        if (a[0].equalsIgnoreCase("ver") && a.length >= 2) {
            Player target;
            if (a.length >= 3) target = Bukkit.getPlayerExact(a[2]);
            else target = s instanceof Player p ? p : null;
            if (target == null) { s.sendMessage(ChatColor.RED + "Oyuncu bulunamadı."); return true; }

            ItemStack item = MerinyumManager.IDS.contains(a[1]) ? mm.create(a[1]) : items.create(a[1]);
            if (item == null) { s.sendMessage(ChatColor.RED + "Böyle bir eşya yok. /ozel liste"); return true; }
            if (a.length >= 4) {
                try { item.setAmount(Math.max(1, Math.min(64, Integer.parseInt(a[3])))); } catch (NumberFormatException ignored) {}
            }

            var left = target.getInventory().addItem(item);
            left.values().forEach(i -> target.getWorld().dropItemNaturally(target.getLocation(), i));
            s.sendMessage(ChatColor.GREEN + a[1] + " verildi: " + target.getName());
            return true;
        }
        s.sendMessage(ChatColor.RED + "Kullanım: /ozel ver <esya> [oyuncu] [adet] | /ozel liste");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> r = new ArrayList<>();
        if (a.length == 1) { r.add("ver"); r.add("liste"); }
        else if (a.length == 2) r.addAll(allIds());
        else if (a.length == 3) Bukkit.getOnlinePlayers().forEach(p -> r.add(p.getName()));
        String last = a[a.length - 1].toLowerCase();
        return r.stream().filter(x -> x.toLowerCase().startsWith(last)).toList();
    }
}
