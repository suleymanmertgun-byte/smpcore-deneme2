package com.smp.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Merinyum: resin (#FC7812) ile altin (#FDF55F) arasi renk => #FCB638
 * Tum esyalar kirilmaz, statlar Netherite'in 2 kati.
 * Uretim: 4 Netherite Kulce + 4 Merinyum Kulcesi + 1 Elmas (ortada) = Saf Merinyum Kulcesi
 * Sonra Demirci Masasi: Netherite Upgrade Sablonu + Netherite esya + Saf Merinyum Kulcesi.
 */
public class MerinyumManager {
    public static final int HEX = 0xFCB638;
    public static final TextColor COLOR = TextColor.color(HEX);
    public static final double X = 2.0;

    public static final List<String> IDS = List.of(
            "merinyum_ham", "merinyum_kulce", "merinyum_saf",
            "merinyum_kilic", "merinyum_kazma", "merinyum_balta", "merinyum_kurek", "merinyum_capa",
            "merinyum_kask", "merinyum_gogusluk", "merinyum_gogusluk_kanat", "merinyum_pantolon", "merinyum_bot");

    private final SMPCore plugin;
    private final NamespacedKey idKey;
    public final NamespacedKey kanatKey;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();

    public MerinyumManager(SMPCore plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "merinyum_id");
        this.kanatKey = new NamespacedKey(plugin, "gogusluk_kanat_tarif");
        registerRecipes();
    }

    public void unregister() {
        for (NamespacedKey k : recipeKeys) Bukkit.removeRecipe(k);
    }

    // ---------- yardimcilar ----------
    public String getId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public boolean isMerinyum(ItemStack item) {
        return getId(item) != null;
    }

    public boolean is(ItemStack item, String id) {
        return id.equals(getId(item));
    }

    public boolean isOurs(Recipe r) {
        return r instanceof Keyed k && k.getKey().getNamespace().equals(plugin.getName().toLowerCase(Locale.ROOT));
    }

    public Material oreBlock() {
        Material m = Material.matchMaterial(plugin.getConfig().getString("merinyum.ore-block", "NETHER_GOLD_ORE"));
        return m == null ? Material.NETHER_GOLD_ORE : m;
    }

    public boolean isOre(Block b) {
        return b.getType() == oreBlock() && b.getWorld().getEnvironment() == World.Environment.NORMAL;
    }

    /** "from" esyasindaki buyuleri "to" esyasina ekler (yuksek olan kalir). */
    public void copyEnchants(ItemStack from, ItemStack to) {
        if (from == null || to == null) return;
        for (Map.Entry<Enchantment, Integer> en : from.getEnchantments().entrySet()) {
            if (to.getEnchantmentLevel(en.getKey()) < en.getValue()) to.addUnsafeEnchantment(en.getKey(), en.getValue());
        }
    }

    private Component gray(String s) {
        return Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack make(Material mat, String id, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text(name, COLOR).decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(gray(s));
        m.lore(l);
        m.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        m.setEnchantmentGlintOverride(true);
        it.setItemMeta(m);
        return it;
    }

    private AttributeModifier mod(String name, double amount, EquipmentSlotGroup slot) {
        return new AttributeModifier(new NamespacedKey(plugin, name), amount,
                AttributeModifier.Operation.ADD_NUMBER, slot);
    }

    private List<String> gearLore(String... extra) {
        List<String> l = new ArrayList<>(List.of("Merinyum'dan dövülmüş.", "Netherite'ten 2 kat güçlü.", "Kırılmaz."));
        l.addAll(Arrays.asList(extra));
        return l;
    }

    /** Netherite alet x2: hasar, saldiri hizi ve kazma hizi. */
    private ItemStack tool(Material mat, String id, String name, double netheriteDamage, double netheriteSpeed,
                           Tag<Material> mineable, String... extra) {
        ItemStack it = make(mat, id, name, gearLore(extra));
        ItemMeta m = it.getItemMeta();
        m.setUnbreakable(true);
        m.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                mod(id + "_dmg", netheriteDamage * X - 1.0, EquipmentSlotGroup.MAINHAND));
        m.addAttributeModifier(Attribute.ATTACK_SPEED,
                mod(id + "_spd", netheriteSpeed * X - 4.0, EquipmentSlotGroup.MAINHAND));
        if (mineable != null) {
            ToolComponent tc = m.getTool();
            tc.setDefaultMiningSpeed(1.0f);
            tc.addRule(mineable, (float) (9.0 * X), true); // Netherite kazma hizi 9.0
            m.setTool(tc);
        }
        it.setItemMeta(m);
        return it;
    }

    /** Boyali deri zirh (renk tam #FCB638), statlar Netherite x2, kirilmaz. */
    private ItemStack armor(Material mat, String id, String name, double armorPts, EquipmentSlotGroup slot,
                            boolean glider, String... extra) {
        ItemStack it = make(mat, id, name, gearLore(extra));
        LeatherArmorMeta m = (LeatherArmorMeta) it.getItemMeta();
        m.setColor(Color.fromRGB(HEX));
        m.setUnbreakable(true);
        m.addItemFlags(ItemFlag.HIDE_DYE);
        m.addAttributeModifier(Attribute.ARMOR, mod(id + "_arm", armorPts * X, slot));
        m.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, mod(id + "_tgh", 3.0 * X, slot));
        m.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, mod(id + "_kb", 0.1 * X, slot));
        if (glider) m.setGlider(true);
        it.setItemMeta(m);
        return it;
    }

    public ItemStack create(String id) {
        return switch (id) {
            case "merinyum_ham" -> make(Material.RAW_GOLD, id, "Ham Merinyum",
                    List.of("Y -30 ve altında bulunur.", "Sadece Netherite kazma ile çıkar.", "Fırında külçeye dönüşür."));
            case "merinyum_kulce" -> make(Material.GOLD_INGOT, id, "Merinyum Külçesi",
                    List.of("Saf Merinyum için gerekli."));
            case "merinyum_saf" -> make(Material.COPPER_INGOT, id, "Saf Merinyum Külçesi",
                    List.of("Demirci Masası'nda Netherite eşyayı", "Merinyum'a yükseltir."));

            case "merinyum_kilic" -> tool(Material.NETHERITE_SWORD, id, "Merinyum Kılıcı", 8.0, 1.6, null,
                    "Her öldürmede canın fullenir.", "Her 5 öldürmede +½ kalp.");
            case "merinyum_kazma" -> tool(Material.NETHERITE_PICKAXE, id, "Merinyum Kazması", 6.0, 1.2,
                    Tag.MINEABLE_PICKAXE, "3x3 alan kazar.", "Shift: tek blok.");
            case "merinyum_balta" -> tool(Material.NETHERITE_AXE, id, "Merinyum Baltası", 10.0, 1.0,
                    Tag.MINEABLE_AXE, "Ağacı yapraklarıyla birlikte keser.", "Shift: tek blok.");
            case "merinyum_kurek" -> tool(Material.NETHERITE_SHOVEL, id, "Merinyum Küreği", 6.5, 1.0,
                    Tag.MINEABLE_SHOVEL, "3x3 alan kazar.", "Shift: tek blok.");
            case "merinyum_capa" -> tool(Material.NETHERITE_HOE, id, "Merinyum Çapası", 1.0, 4.0,
                    Tag.MINEABLE_HOE, "3x3 alanı çapalar.", "Shift: tek blok.");

            case "merinyum_kask" -> armor(Material.LEATHER_HELMET, id, "Merinyum Miğferi", 3, EquipmentSlotGroup.HEAD, false);
            case "merinyum_gogusluk" -> armor(Material.LEATHER_CHESTPLATE, id, "Merinyum Göğüslüğü", 8, EquipmentSlotGroup.CHEST, false,
                    "Üretim masasında Elytra ile birleştirilebilir.");
            case "merinyum_gogusluk_kanat" -> armor(Material.LEATHER_CHESTPLATE, id, "Merinyum Kanatlı Göğüslük", 8, EquipmentSlotGroup.CHEST, true,
                    "Elytra gibi süzülür.", "Süzülürken sarımsı aura bırakır.");
            case "merinyum_pantolon" -> armor(Material.LEATHER_LEGGINGS, id, "Merinyum Pantolonu", 6, EquipmentSlotGroup.LEGS, false);
            case "merinyum_bot" -> armor(Material.LEATHER_BOOTS, id, "Merinyum Botları", 3, EquipmentSlotGroup.FEET, false);
            default -> null;
        };
    }

    // ---------- tarifler ----------
    private void registerRecipes() {
        // Saf Merinyum Kulcesi: 4 Netherite Kulce (kose) + 4 Merinyum Kulcesi (kenar) + 1 Elmas (orta)
        NamespacedKey sk = new NamespacedKey(plugin, "merinyum_saf_tarif");
        ShapedRecipe saf = new ShapedRecipe(sk, create("merinyum_saf"));
        saf.shape("NKN", "KDK", "NKN");
        saf.setIngredient('N', Material.NETHERITE_INGOT);
        saf.setIngredient('K', new RecipeChoice.ExactChoice(create("merinyum_kulce")));
        saf.setIngredient('D', Material.DIAMOND);
        Bukkit.addRecipe(saf);
        recipeKeys.add(sk);

        // Demirci masasi: sablon + netherite esya + saf merinyum
        smithing("merinyum_kilic", Material.NETHERITE_SWORD);
        smithing("merinyum_kazma", Material.NETHERITE_PICKAXE);
        smithing("merinyum_balta", Material.NETHERITE_AXE);
        smithing("merinyum_kurek", Material.NETHERITE_SHOVEL);
        smithing("merinyum_capa", Material.NETHERITE_HOE);
        smithing("merinyum_kask", Material.NETHERITE_HELMET);
        smithing("merinyum_gogusluk", Material.NETHERITE_CHESTPLATE);
        smithing("merinyum_pantolon", Material.NETHERITE_LEGGINGS);
        smithing("merinyum_bot", Material.NETHERITE_BOOTS);

        // Merinyum Gogusluk + Elytra (uretim masasi). Dogrulama ve buyu aktarimi MerinyumListener'da.
        ShapelessRecipe kanat = new ShapelessRecipe(kanatKey, create("merinyum_gogusluk_kanat"));
        kanat.addIngredient(Material.LEATHER_CHESTPLATE);
        kanat.addIngredient(Material.ELYTRA);
        Bukkit.addRecipe(kanat);
        recipeKeys.add(kanatKey);
    }

    private void smithing(String id, Material base) {
        NamespacedKey k = new NamespacedKey(plugin, id + "_basma");
        SmithingTransformRecipe r = new SmithingTransformRecipe(k, create(id),
                new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new RecipeChoice.MaterialChoice(base),
                new RecipeChoice.ExactChoice(create("merinyum_saf")));
        Bukkit.addRecipe(r);
        recipeKeys.add(k);
    }
}
