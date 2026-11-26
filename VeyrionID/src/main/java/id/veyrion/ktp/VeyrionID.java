package id.veyrion.ktp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public final class VeyrionID extends JavaPlugin implements Listener, TabExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private NamespacedKey ownerKey;
    private File dataFile;
    private YamlConfiguration data;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ownerKey = new NamespacedKey(this, "owner");
        dataFile = new File(getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);
        var cmd = getCommand("ki");
        if (cmd != null) { cmd.setExecutor(this); cmd.setTabCompleter(this); }
        getServer().getPluginManager().registerEvents(this, this);
    }

    // ---------- Pesan & kartu ----------

    private Component msg(String body) {
        return MM.deserialize(getConfig().getString("prefix", "") + "<white>" + body);
    }

    private List<Component> cardLines(UUID id) {
        String p = "players." + id + ".";
        TagResolver r = TagResolver.resolver(
                Placeholder.unparsed("nama", data.getString(p + "nama", "-")),
                Placeholder.unparsed("umur", data.getString(p + "umur", "-")),
                Placeholder.unparsed("lahir", data.getString(p + "lahir", "-")),
                Placeholder.unparsed("ras", data.getString(p + "ras", "-")),
                Placeholder.unparsed("terbit", data.getString(p + "terbit", "-")));
        String[] t = {
                "<#7a6a45>══════════════════════",
                "<gradient:#b8892b:#f1d98a><bold>KARTU IDENTITAS VEYRION</bold></gradient>",
                "<#7a6a45>══════════════════════",
                "<#b8892b>Nama   <gray>: <white><nama>",
                "<#b8892b>Umur   <gray>: <white><umur> tahun",
                "<#b8892b>Lahir  <gray>: <white><lahir>",
                "<#b8892b>Ras    <gray>: <#f1d98a><ras>",
                "<#b8892b>Terbit <gray>: <white><terbit>",
                "<#7a6a45>══════════════════════"};
        List<Component> out = new ArrayList<>();
        for (String line : t) out.add(MM.deserialize(line, r));
        return out;
    }

    private void showCard(Player viewer, UUID owner) {
        if (!data.contains("players." + owner)) {
            viewer.sendMessage(msg("Kartu ini belum terdaftar."));
            return;
        }
        cardLines(owner).forEach(viewer::sendMessage);
    }

    private ItemStack makeCard(UUID owner) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize("<gradient:#b8892b:#f1d98a><bold>Kartu Identitas Veyrion</bold></gradient>")
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (Component c : cardLines(owner)) lore.add(c.decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(MM.deserialize("<gray>Klik kanan: lihat · Klik kanan pemain: tunjukkan")
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        int cmd = getConfig().getInt("item.custom-model-data", 0);
        if (cmd > 0) meta.setCustomModelData(cmd);
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.toString());
        item.setItemMeta(meta);
        return item;
    }

    private void giveCard(Player p, UUID owner) {
        p.getInventory().addItem(makeCard(owner)).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
    }

    private UUID cardOwner(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        try { return s == null ? null : UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    private void save() {
        try { data.save(dataFile); } catch (IOException e) { getLogger().severe("Gagal menyimpan data: " + e.getMessage()); }
    }

    // ---------- Event ----------

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        UUID owner = cardOwner(e.getItem());
        if (owner == null) return;
        e.setCancelled(true);
        showCard(e.getPlayer(), owner);
    }

    @EventHandler
    public void onShow(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !(e.getRightClicked() instanceof Player target)) return;
        UUID owner = cardOwner(e.getPlayer().getInventory().getItemInMainHand());
        if (owner == null) return;
        e.setCancelled(true);
        target.sendMessage(msg("<#f1d98a>" + e.getPlayer().getName() + "</#f1d98a> menunjukkan kartu identitasnya:"));
        showCard(target, owner);
    }

    // ---------- Perintah ----------

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) { s.sendMessage(msg("/ki <daftar|lihat|tunjuk|kartu|hapus|reload>")); return true; }
        boolean admin = s.hasPermission("veyrion.id.admin");
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "daftar" -> register(s, a);
            case "lihat" -> {
                if (!(s instanceof Player p)) { s.sendMessage(msg("Hanya untuk pemain.")); return true; }
                UUID target = p.getUniqueId();
                if (a.length > 1) {
                    if (!admin) { s.sendMessage(msg("Kamu tidak punya izin.")); return true; }
                    Player t = Bukkit.getPlayerExact(a[1]);
                    if (t == null) { s.sendMessage(msg("Pemain tidak ditemukan.")); return true; }
                    target = t.getUniqueId();
                }
                showCard(p, target);
            }
            case "tunjuk" -> {
                if (!(s instanceof Player p)) return true;
                if (!data.contains("players." + p.getUniqueId())) { p.sendMessage(msg("Kamu belum punya kartu.")); return true; }
                double r = getConfig().getDouble("show-radius", 6);
                p.sendMessage(msg("Kartu ditunjukkan ke pemain di sekitarmu."));
                for (Player o : p.getWorld().getPlayers()) {
                    if (o.equals(p) || o.getLocation().distance(p.getLocation()) > r) continue;
                    o.sendMessage(msg("<#f1d98a>" + p.getName() + "</#f1d98a> menunjukkan kartu identitasnya:"));
                    showCard(o, p.getUniqueId());
                }
            }
            case "kartu" -> {
                if (!(s instanceof Player p)) return true;
                if (!data.contains("players." + p.getUniqueId())) { p.sendMessage(msg("Daftar dulu: /ki daftar")); return true; }
                giveCard(p, p.getUniqueId());
                p.sendMessage(msg("Kartu identitasmu diberikan."));
            }
            case "hapus" -> {
                if (!admin) { s.sendMessage(msg("Kamu tidak punya izin.")); return true; }
                if (a.length < 2) { s.sendMessage(msg("/ki hapus <pemain>")); return true; }
                @SuppressWarnings("deprecation") var off = Bukkit.getOfflinePlayer(a[1]);
                data.set("players." + off.getUniqueId(), null);
                save();
                s.sendMessage(msg("Data identitas " + a[1] + " dihapus."));
            }
            case "reload" -> {
                if (!admin) { s.sendMessage(msg("Kamu tidak punya izin.")); return true; }
                reloadConfig();
                data = YamlConfiguration.loadConfiguration(dataFile);
                s.sendMessage(msg("Konfigurasi dimuat ulang."));
            }
            default -> s.sendMessage(msg("Perintah tidak dikenal."));
        }
        return true;
    }

    private void register(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage(msg("Hanya untuk pemain.")); return; }
        if (a.length < 5) {
            p.sendMessage(msg("/ki daftar <nama> <umur> <DD/MM/YYYY> <ras>"));
            p.sendMessage(msg("<gray>Pakai _ sebagai spasi pada nama. Contoh: /ki daftar Kynaro_Karamazov 17 11/01/2009 Elf"));
            return;
        }
        String key = "players." + p.getUniqueId();
        if (data.contains(key) && !getConfig().getBoolean("allow-reregister", false)
                && !p.hasPermission("veyrion.id.admin")) {
            p.sendMessage(msg("Kamu sudah terdaftar. Hubungi staf untuk mengubah data."));
            return;
        }
        String nama = a[1].replace('_', ' ');
        if (nama.length() < 2 || nama.length() > 32) { p.sendMessage(msg("Nama harus 2–32 karakter.")); return; }
        int umur;
        try { umur = Integer.parseInt(a[2]); } catch (NumberFormatException e) { umur = -1; }
        if (umur < 1 || umur > 9999) { p.sendMessage(msg("Umur harus angka 1–9999.")); return; }
        try { LocalDate.parse(a[3], DATE); } catch (Exception e) { p.sendMessage(msg("Format tanggal lahir: DD/MM/YYYY.")); return; }
        String ras = getConfig().getStringList("races").stream()
                .filter(x -> x.equalsIgnoreCase(a[4])).findFirst().orElse(null);
        if (ras == null) {
            p.sendMessage(msg("Ras tidak valid. Pilihan: " + String.join(", ", getConfig().getStringList("races"))));
            return;
        }
        data.set(key + ".nama", nama);
        data.set(key + ".umur", umur);
        data.set(key + ".lahir", a[3]);
        data.set(key + ".ras", ras);
        data.set(key + ".terbit", LocalDate.now().format(DATE));
        save();
        giveCard(p, p.getUniqueId());
        p.sendMessage(msg("Kamu resmi tercatat sebagai penduduk Veyrion."));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> opts = new ArrayList<>();
        if (a.length == 1) {
            opts.addAll(List.of("daftar", "lihat", "tunjuk", "kartu"));
            if (s.hasPermission("veyrion.id.admin")) opts.addAll(List.of("hapus", "reload"));
        } else if (a.length == 2 && (a[0].equalsIgnoreCase("hapus") || a[0].equalsIgnoreCase("lihat"))) {
            Bukkit.getOnlinePlayers().forEach(p -> opts.add(p.getName()));
        } else if (a.length == 5 && a[0].equalsIgnoreCase("daftar")) {
            opts.addAll(getConfig().getStringList("races"));
        }
        String last = a[a.length - 1].toLowerCase(Locale.ROOT);
        return opts.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(last)).collect(Collectors.toList());
    }
}
