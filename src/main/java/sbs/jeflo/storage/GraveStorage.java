package sbs.jeflo.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.Base64;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import java.util.Map;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import org.bukkit.configuration.InvalidConfigurationException;

public class GraveStorage {
    private String jdbcUrl;
    private static final String TABLE = "graves";

    public GraveStorage(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.jdbcUrl = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + File.separator + "DeadGravesDB.sqlite";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                    + "x INTEGER, y INTEGER, z INTEGER, world TEXT, uuid TEXT, contents TEXT, PRIMARY KEY (x, y, z, world))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveGrave(Location loc, UUID uuid, Inventory inv) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR REPLACE INTO " + TABLE + " (x, y, z, world, uuid, contents) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, loc.getBlockX());
            ps.setInt(2, loc.getBlockY());
            ps.setInt(3, loc.getBlockZ());
            ps.setString(4, loc.getWorld().getName());
            ps.setString(5, uuid.toString());
            ps.setString(6, serializeInventory(inv));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadAllGraves(Map<Location, Inventory> graves, Map<Location, UUID> owners, String inventoryTitle) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + TABLE);
            while (rs.next()) {
                Location loc = new Location(
                        Bukkit.getWorld(rs.getString("world")),
                        rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                Inventory inv = Bukkit.createInventory(null, 54, inventoryTitle);
                ItemStack[] items = deserializeInventory(rs.getString("contents"));
                inv.setContents(items);
                graves.put(loc, inv);
                owners.put(loc, uuid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteGrave(Location loc) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM " + TABLE + " WHERE x=? AND y=? AND z=? AND world=?");
            ps.setInt(1, loc.getBlockX());
            ps.setInt(2, loc.getBlockY());
            ps.setInt(3, loc.getBlockZ());
            ps.setString(4, loc.getWorld().getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String serializeInventory(Inventory inv) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("items", inv.getContents());
        return config.saveToString();
    }

    private ItemStack[] deserializeInventory(String data) {
        if (data == null || data.isEmpty()) return new ItemStack[54];
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(data);
            @SuppressWarnings("unchecked")
            java.util.List<ItemStack> list = (java.util.List<ItemStack>) config.getList("items");
            return list != null ? list.toArray(new ItemStack[54]) : new ItemStack[54];
        } catch (Exception e) {
            e.printStackTrace();
            return new ItemStack[54];
        }
    }
}
