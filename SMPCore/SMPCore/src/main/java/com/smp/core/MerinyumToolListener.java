package com.smp.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.*;

/** Merinyum esyalarin yetenekleri: 3x3 kazma/kurek/capa, agac kesme, kill bonusu, kanat aurasi, maden yavaslatma. */
public class MerinyumToolListener implements Listener {
    private static final double OBSIDIAN_HARDNESS = 50.0;
    private static final Set<Material> TILLABLE = EnumSet.of(
            Material.DIRT, Material.GRASS_BLOCK, Material.DIRT_PATH, Material.COARSE_DIRT);
    private static final BlockFace[] FACES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
            BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    private final SMPCore plugin;
    private final MerinyumManager mm;
    private final Set<UUID> busy = new HashSet<>();
    private final Set<UUID> slowed = new HashSet<>();
    private final NamespacedKey slowKey, bonusKey, killsKey;

    public MerinyumToolListener(SMPCore plugin, MerinyumManager mm) {
        this.plugin = plugin;
        this.mm = mm;
        this.slowKey = new NamespacedKey(plugin, "maden_yavas");
        this.bonusKey = new NamespacedKey(plugin, "kill_bonus_can");
        this.killsKey = new NamespacedKey(plugin, "merinyum_kills");

        // Maden yavaslatmasini, oyuncu madene bakmayi biraklinca kaldir
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID id : new ArrayList<>(slowed)) {
                Player p = Bukkit.getPlayer(id);
                if (p == null) { slowed.remove(id); continue; }
                Block t = p.getTargetBlockExact(6);
                if (t == null || !mm.isOre(t)) removeSlow(p);
            }
        }, 5L, 5L);

        // Kanatli gogusluk: suzulurken sarimsi aura
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Particle.DustOptions opt = new Particle.DustOptions(Color.fromRGB(MerinyumManager.HEX), 1.6f);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.isGliding()) continue;
                if (!mm.is(p.getInventory().getChestplate(), "merinyum_gogusluk_kanat")) continue;
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 14, 0.7, 0.5, 0.7, 0, opt);
            }
        }, 2L, 2L);
    }

    // ------------------------------------------------------------------ maden yavaslatma
    @EventHandler(ignoreCancelled = true)
    public void onDamage(BlockDamageEvent e) {
        Block b = e.getBlock();
        if (!mm.isOre(b)) return;
        Player p = e.getPlayer();
        AttributeInstance a = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (a == null) return;
        for (AttributeModifier m : a.getModifiers()) if (m.getKey().equals(slowKey)) return;

        double target = plugin.getConfig().getDouble("merinyum.ore-hardness", OBSIDIAN_HARDNESS / 2);
        double factor = Math.min(1.0, mm.oreBlock().getHardness() / target);
        a.addModifier(new AttributeModifier(slowKey, factor - 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        slowed.add(p.getUniqueId());
    }

    private void removeSlow(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (a != null) {
            for (AttributeModifier m : new ArrayList<>(a.getModifiers())) {
                if (m.getKey().equals(slowKey)) a.removeModifier(m);
            }
        }
        slowed.remove(p.getUniqueId());
    }

    // ------------------------------------------------------------------ kazma / kurek 3x3, balta agac
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (busy.contains(p.getUniqueId()) || p.isSneaking()) return;
        ItemStack tool = p.getInventory().getItemInMainHand();
        String id = mm.getId(tool);
        if (id == null) return;
        Block b = e.getBlock();
        switch (id) {
            case "merinyum_kazma", "merinyum_kurek" -> area(p, b, tool);
            case "merinyum_balta" -> tree(b, tool);
            default -> { }
        }
    }

    private void area(Player p, Block center, ItemStack tool) {
        RayTraceResult r = p.rayTraceBlocks(6.0);
        BlockFace face = (r != null && center.equals(r.getHitBlock()) && r.getHitBlockFace() != null)
                ? r.getHitBlockFace() : BlockFace.UP;
        int ax = Math.abs(face.getModX()), ay = Math.abs(face.getModY()), az = Math.abs(face.getModZ());

        busy.add(p.getUniqueId());
        try {
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (ax == 1 && dx != 0) continue;
                if (ay == 1 && dy != 0) continue;
                if (az == 1 && dz != 0) continue;
                if (dx == 0 && dy == 0 && dz == 0) continue;
                Block n = center.getRelative(dx, dy, dz);
                if (canArea(n, tool)) p.breakBlock(n);
            }
        } finally {
            busy.remove(p.getUniqueId());
        }
    }

    private boolean canArea(Block n, ItemStack tool) {
        Material t = n.getType();
        if (t.isAir() || n.isLiquid()) return false;
        float h = t.getHardness();
        if (h < 0 || h > 10) return false;           // bedrock, obsidyen, ancient debris vb. 3x3'e girmez
        if (mm.isOre(n)) return false;               // merinyum madeni yavas kirilmali, toplu kirilmaz
        return n.isPreferredTool(tool);
    }

    private boolean isNaturalLeaf(Block b) {
        return Tag.LEAVES.isTagged(b.getType()) && b.getBlockData() instanceof Leaves lv && !lv.isPersistent();
    }

    private void tree(Block start, ItemStack tool) {
        if (!Tag.LOGS.isTagged(start.getType())) return;

        Set<Block> logs = new HashSet<>();
        Deque<Block> q = new ArrayDeque<>();
        logs.add(start);
        q.add(start);
        while (!q.isEmpty() && logs.size() < 300) {
            Block c = q.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                Block n = c.getRelative(dx, dy, dz);
                if (logs.contains(n) || !Tag.LOGS.isTagged(n.getType())) continue;
                logs.add(n);
                q.add(n);
            }
        }

        // Yapraklar: kutuklerden baslayip yaprak zinciri boyunca 6 blok
        Set<Block> leaves = new HashSet<>();
        Map<Block, Integer> dist = new HashMap<>();
        Deque<Block> lq = new ArrayDeque<>();
        for (Block l : logs) { dist.put(l, 0); lq.add(l); }
        while (!lq.isEmpty() && leaves.size() < 800) {
            Block c = lq.poll();
            int d = dist.get(c);
            if (d >= 6) continue;
            for (BlockFace f : FACES) {
                Block n = c.getRelative(f);
                if (dist.containsKey(n) || !isNaturalLeaf(n)) continue;
                dist.put(n, d + 1);
                leaves.add(n);
                lq.add(n);
            }
        }

        // Yapraksiz kutuk yigini = oyuncu yapisi olabilir (ev, kutuk duvar); dokunma
        if (leaves.isEmpty()) return;

        for (Block l : logs) if (!l.equals(start)) l.breakNaturally(tool);
        for (Block l : leaves) l.breakNaturally(tool);
    }

    // ------------------------------------------------------------------ capa 3x3
    @EventHandler
    public void onHoe(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!mm.is(e.getItem(), "merinyum_capa")) return;
        Player p = e.getPlayer();
        Block c = e.getClickedBlock();
        if (c == null || p.isSneaking() || e.getBlockFace() == BlockFace.DOWN) return;
        if (!TILLABLE.contains(c.getType())) return;

        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) continue;   // ortadakini oyun kendisi capalar
            Block n = c.getRelative(dx, 0, dz);
            switch (n.getType()) {
                case DIRT, GRASS_BLOCK, DIRT_PATH -> {
                    if (n.getRelative(BlockFace.UP).getType().isAir()) n.setType(Material.FARMLAND);
                }
                case COARSE_DIRT -> n.setType(Material.DIRT);
                default -> { }
            }
        }
    }

    // ------------------------------------------------------------------ kilic: her kill'de can full, her 5 kill'de +yarim kalp
    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null) return;
        if (!mm.is(p.getInventory().getItemInMainHand(), "merinyum_kilic")) return;

        LivingEntity v = e.getEntity();
        boolean all = plugin.getConfig().getBoolean("merinyum.sword.count-all-mobs", false);
        if (!(all || v instanceof Player || v instanceof Enemy)) return;   // ciftlik hayvani farm'i sayilmaz
        if (v.equals(p)) return;

        var pdc = p.getPersistentDataContainer();
        int kills = pdc.getOrDefault(killsKey, PersistentDataType.INTEGER, 0) + 1;
        pdc.set(killsKey, PersistentDataType.INTEGER, kills);
        applyBonus(p);

        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20.0 : max.getValue());

        int toNext = 5 - (kills % 5);
        p.sendActionBar(Component.text("Merinyum Kılıcı: " + kills + " kill  |  sonraki ½ kalp: " + toNext,
                NamedTextColor.GOLD));
    }

    private void applyBonus(Player p) {
        int kills = p.getPersistentDataContainer().getOrDefault(killsKey, PersistentDataType.INTEGER, 0);
        double cap = plugin.getConfig().getDouble("merinyum.sword.max-bonus-health", 20.0);
        double bonus = Math.min(cap, (kills / 5) * 1.0);   // 5 kill = 1 can = yarim kalp

        AttributeInstance a = p.getAttribute(Attribute.MAX_HEALTH);
        if (a == null) return;
        for (AttributeModifier m : new ArrayList<>(a.getModifiers())) {
            if (m.getKey().equals(bonusKey)) a.removeModifier(m);
        }
        if (bonus > 0) a.addModifier(new AttributeModifier(bonusKey, bonus, AttributeModifier.Operation.ADD_NUMBER));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        applyBonus(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> applyBonus(e.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        removeSlow(e.getPlayer());
    }
}
