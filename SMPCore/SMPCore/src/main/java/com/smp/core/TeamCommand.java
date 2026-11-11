package com.smp.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabCompleter {
    private final SMPCore plugin;
    private final TeamManager tm;
    private static final String P = ChatColor.GOLD + "[Takım] " + ChatColor.GRAY;

    public TeamCommand(SMPCore plugin, TeamManager tm) {
        this.plugin = plugin;
        this.tm = tm;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!(s instanceof Player p)) {
            s.sendMessage("Bu komut sadece oyunculara özel.");
            return true;
        }

        // /tc <mesaj>
        if (cmd.getName().equalsIgnoreCase("tc")) {
            Team t = tm.getTeam(p.getUniqueId());
            if (t == null) { p.sendMessage(P + "Bir takımda değilsin."); return true; }
            if (a.length == 0) { p.sendMessage(P + "Kullanım: /tc <mesaj>"); return true; }
            String msg = ChatColor.AQUA + "[TC] " + ChatColor.WHITE + p.getName() + ChatColor.GRAY + ": " + ChatColor.WHITE + String.join(" ", a);
            broadcast(t, msg);
            return true;
        }

        if (a.length == 0) { help(p); return true; }
        UUID id = p.getUniqueId();
        Team mine = tm.getTeam(id);

        switch (a[0].toLowerCase()) {
            case "olustur", "create" -> {
                if (mine != null) { p.sendMessage(P + "Zaten bir takımdasın. Önce /takim ayril"); return true; }
                if (a.length < 2) { p.sendMessage(P + "Kullanım: /takim olustur <isim>"); return true; }
                String name = a[1];
                if (!name.matches("[A-Za-z0-9_]{2,12}")) { p.sendMessage(P + "İsim 2-12 karakter, sadece harf/rakam/_ olmalı."); return true; }
                if (tm.getByName(name) != null) { p.sendMessage(P + "Bu isimde bir takım zaten var."); return true; }
                tm.create(name, id);
                Bukkit.broadcastMessage(P + ChatColor.YELLOW + p.getName() + ChatColor.GRAY + " yeni bir takım kurdu: " + ChatColor.GOLD + name);
            }
            case "davet", "invite" -> {
                if (mine == null) { p.sendMessage(P + "Bir takımda değilsin."); return true; }
                if (!mine.owner.equals(id)) { p.sendMessage(P + "Sadece lider davet edebilir."); return true; }
                if (a.length < 2) { p.sendMessage(P + "Kullanım: /takim davet <oyuncu>"); return true; }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) { p.sendMessage(P + "Oyuncu çevrimiçi değil."); return true; }
                if (tm.getTeam(t.getUniqueId()) != null) { p.sendMessage(P + "Bu oyuncu zaten bir takımda."); return true; }
                if (mine.members.size() >= plugin.getConfig().getInt("team.max-members", 10)) { p.sendMessage(P + "Takım dolu."); return true; }
                tm.invite(t.getUniqueId(), mine);
                p.sendMessage(P + t.getName() + " oyuncusuna davet gönderildi.");
                t.sendMessage(P + ChatColor.YELLOW + p.getName() + ChatColor.GRAY + " seni " + ChatColor.GOLD + mine.name + ChatColor.GRAY + " takımına davet etti. Kabul: " + ChatColor.GREEN + "/takim kabul");
            }
            case "kabul", "accept" -> {
                if (mine != null) { p.sendMessage(P + "Zaten bir takımdasın."); return true; }
                Team t = tm.popInvite(id);
                if (t == null) { p.sendMessage(P + "Bekleyen davetin yok."); return true; }
                tm.join(t, id);
                broadcast(t, P + ChatColor.YELLOW + p.getName() + ChatColor.GRAY + " takıma katıldı!");
            }
            case "ayril", "leave" -> {
                if (mine == null) { p.sendMessage(P + "Bir takımda değilsin."); return true; }
                tm.remove(mine, id);
                p.sendMessage(P + "Takımdan ayrıldın.");
                broadcast(mine, P + ChatColor.YELLOW + p.getName() + ChatColor.GRAY + " takımdan ayrıldı.");
            }
            case "at", "kick" -> {
                if (mine == null || !mine.owner.equals(id)) { p.sendMessage(P + "Sadece lider oyuncu atabilir."); return true; }
                if (a.length < 2) { p.sendMessage(P + "Kullanım: /takim at <oyuncu>"); return true; }
                OfflinePlayer t = Bukkit.getOfflinePlayer(a[1]);
                if (!mine.members.contains(t.getUniqueId()) || t.getUniqueId().equals(id)) { p.sendMessage(P + "Bu oyuncu takımında değil."); return true; }
                tm.remove(mine, t.getUniqueId());
                broadcast(mine, P + ChatColor.YELLOW + a[1] + ChatColor.GRAY + " takımdan atıldı.");
                if (t.isOnline()) t.getPlayer().sendMessage(P + "Takımdan atıldın.");
            }
            case "devret" -> {
                if (mine == null || !mine.owner.equals(id)) { p.sendMessage(P + "Sadece lider devredebilir."); return true; }
                if (a.length < 2) { p.sendMessage(P + "Kullanım: /takim devret <oyuncu>"); return true; }
                OfflinePlayer t = Bukkit.getOfflinePlayer(a[1]);
                if (!mine.members.contains(t.getUniqueId())) { p.sendMessage(P + "Bu oyuncu takımında değil."); return true; }
                mine.owner = t.getUniqueId();
                tm.save();
                broadcast(mine, P + ChatColor.YELLOW + a[1] + ChatColor.GRAY + " artık takım lideri.");
            }
            case "dagit", "disband" -> {
                if (mine == null || !mine.owner.equals(id)) { p.sendMessage(P + "Sadece lider takımı dağıtabilir."); return true; }
                broadcast(mine, P + "Takım dağıtıldı.");
                tm.disband(mine);
            }
            case "bilgi", "info" -> {
                Team t = a.length >= 2 ? tm.getByName(a[1]) : mine;
                if (t == null) { p.sendMessage(P + "Takım bulunamadı."); return true; }
                p.sendMessage(ChatColor.GOLD + "=== " + t.name + " ===");
                p.sendMessage(ChatColor.GRAY + "Lider: " + ChatColor.YELLOW + name(t.owner));
                p.sendMessage(ChatColor.GRAY + "Üyeler (" + t.members.size() + "): " + ChatColor.WHITE +
                        t.members.stream().map(this::name).collect(Collectors.joining(", ")));
            }
            case "liste", "list" -> {
                if (tm.all().isEmpty()) { p.sendMessage(P + "Henüz takım yok."); return true; }
                p.sendMessage(ChatColor.GOLD + "=== Takımlar ===");
                for (Team t : tm.all())
                    p.sendMessage(ChatColor.YELLOW + t.name + ChatColor.GRAY + " - " + t.members.size() + " üye (lider: " + name(t.owner) + ")");
            }
            default -> help(p);
        }
        return true;
    }

    private void help(Player p) {
        p.sendMessage(ChatColor.GOLD + "=== Takım Komutları ===");
        p.sendMessage(ChatColor.YELLOW + "/takim olustur <isim>" + ChatColor.GRAY + " - takım kur");
        p.sendMessage(ChatColor.YELLOW + "/takim davet <oyuncu>" + ChatColor.GRAY + " - davet et");
        p.sendMessage(ChatColor.YELLOW + "/takim kabul" + ChatColor.GRAY + " - daveti kabul et");
        p.sendMessage(ChatColor.YELLOW + "/takim ayril" + ChatColor.GRAY + " - takımdan ayrıl");
        p.sendMessage(ChatColor.YELLOW + "/takim at <oyuncu>" + ChatColor.GRAY + " - oyuncu at (lider)");
        p.sendMessage(ChatColor.YELLOW + "/takim devret <oyuncu>" + ChatColor.GRAY + " - liderliği devret");
        p.sendMessage(ChatColor.YELLOW + "/takim dagit" + ChatColor.GRAY + " - takımı dağıt (lider)");
        p.sendMessage(ChatColor.YELLOW + "/takim bilgi [takım]" + ChatColor.GRAY + " - bilgi");
        p.sendMessage(ChatColor.YELLOW + "/takim liste" + ChatColor.GRAY + " - tüm takımlar");
        p.sendMessage(ChatColor.YELLOW + "/tc <mesaj>" + ChatColor.GRAY + " - takım sohbeti");
    }

    private void broadcast(Team t, String msg) {
        for (UUID id : t.members) {
            Player pl = Bukkit.getPlayer(id);
            if (pl != null) pl.sendMessage(msg);
        }
    }

    private String name(UUID id) {
        String n = Bukkit.getOfflinePlayer(id).getName();
        return n == null ? "?" : n;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1)
            return filter(List.of("olustur", "davet", "kabul", "ayril", "at", "devret", "dagit", "bilgi", "liste"), a[0]);
        if (a.length == 2 && List.of("davet", "at", "devret").contains(a[0].toLowerCase()))
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), a[1]);
        if (a.length == 2 && a[0].equalsIgnoreCase("bilgi"))
            return filter(tm.all().stream().map(t -> t.name).toList(), a[1]);
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream().filter(x -> x.toLowerCase().startsWith(prefix.toLowerCase())).toList();
    }
}
