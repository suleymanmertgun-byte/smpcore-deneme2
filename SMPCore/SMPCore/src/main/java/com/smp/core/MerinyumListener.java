package com.smp.core;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmithingInventory;

import java.util.concurrent.ThreadLocalRandom;

public class MerinyumListener implements Listener {
    private final MerinyumManager mm;

    public MerinyumListener(SMPCore plugin, MerinyumManager mm) {
        this.mm = mm;
    }

    // Cevheri kir: Ham Merinyum duser (Servet destekli). Sadece Netherite / Merinyum kazma (ikisi de netherite tabanli).
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!mm.isOre(b)) return;

        e.setDropItems(false);
        ItemStack tool = e.getPlayer().getInventory().getItemInMainHand();
        if (tool.getType() != Material.NETHERITE_PICKAXE) return;

        int fortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        int amount = fortune > 0 ? Math.max(1, ThreadLocalRandom.current().nextInt(fortune + 2)) : 1;
        ItemStack drop = mm.create("merinyum_ham");
        drop.setAmount(amount);
        b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), drop);
        e.setExpToDrop(ThreadLocalRandom.current().nextInt(3, 8));
    }

    // Nether'den getirilen cevheri koyup hile yapmayi engelle
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() == mm.oreBlock()
                && e.getBlockPlaced().getWorld().getEnvironment() == World.Environment.NORMAL
                && !e.getPlayer().hasPermission("ozel.admin")) {
            e.setCancelled(true);
        }
    }

    // Ham Merinyum -> Merinyum Kulcesi (altin olmasin)
    @EventHandler
    public void onCook(BlockCookEvent e) {
        ItemStack src = e.getSource();
        if (mm.is(src, "merinyum_ham")) {
            e.setResult(mm.create("merinyum_kulce"));
        } else if (mm.isMerinyum(src)) {
            e.setCancelled(true);
        }
    }

    // Uretim masasi: Gogusluk + Elytra birlesimi, ve merinyum esyalarin vanilla tariflerde kullanilmasini engelle
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        CraftingInventory inv = e.getInventory();
        Recipe rec = e.getRecipe();
        if (rec == null) return;

        if (rec instanceof Keyed k && k.getKey().equals(mm.kanatKey)) {
            ItemStack chest = null, ely = null;
            for (ItemStack i : inv.getMatrix()) {
                if (i == null) continue;
                if (i.getType() == Material.ELYTRA) ely = i;
                else if (mm.is(i, "merinyum_gogusluk")) chest = i;
            }
            if (chest == null || ely == null) { inv.setResult(null); return; }
            ItemStack res = mm.create("merinyum_gogusluk_kanat");
            mm.copyEnchants(chest, res);
            mm.copyEnchants(ely, res);
            inv.setResult(res);
            return;
        }

        if (mm.isOurs(rec)) return;
        for (ItemStack i : inv.getMatrix()) {
            if (mm.isMerinyum(i)) { inv.setResult(null); return; }
        }
    }

    // Demirci masasi: buyuleri aktar, yanlis kullanimlari engelle
    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent e) {
        SmithingInventory inv = e.getInventory();
        Recipe rec = inv.getRecipe();
        if (rec == null) return;
        ItemStack base = inv.getInputEquipment();

        if (mm.isOurs(rec)) {
            if (mm.isMerinyum(base)) { e.setResult(null); return; }
            ItemStack res = e.getResult();
            if (res != null && base != null) {
                mm.copyEnchants(base, res);
                e.setResult(res);
            }
            return;
        }
        if (mm.isMerinyum(base) || mm.isMerinyum(inv.getInputMineral()) || mm.isMerinyum(inv.getInputTemplate())) {
            e.setResult(null);
        }
    }

    // Yere dusen merinyum esyalari lavda/ateste/patlamada yok olmasin
    @EventHandler
    public void onItemDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Item item && mm.isMerinyum(item.getItemStack())
                && e.getCause() != EntityDamageEvent.DamageCause.VOID) {
            e.setCancelled(true);
        }
    }
}
