package sbs.jeflo.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.FileConfiguration;
import sbs.jeflo.DeadGraves;
import sbs.jeflo.storage.GraveStorage;
import sbs.jeflo.MessageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GraveListener implements Listener {
    private final Map<Location, Inventory> graves = new HashMap<>();
    private final Map<Location, UUID> graveOwners = new HashMap<>();
    private final GraveStorage storage;
    private final DeadGraves plugin;
    private final MessageManager messages;
    private final Map<Inventory, Location> openGraveLocations = new HashMap<>();
    private final FileConfiguration config;
    private String soundOpenGraveName;
    private String soundRemoveGraveName;

    public GraveListener(DeadGraves plugin) {
        this.plugin = plugin;
        this.storage = new GraveStorage(plugin.getDataFolder());
        this.messages = new MessageManager(plugin);
        this.config = plugin.getConfig();
        loadSounds();
        storage.loadAllGraves(graves, graveOwners, "Grave");
    }

    private void loadSounds() {
        soundOpenGraveName = config.getString("sounds.open-grave", "BLOCK_CHEST_OPEN");
        soundRemoveGraveName = config.getString("sounds.remove-grave", "ENTITY_WITHER_DEATH");
    }

    private Sound resolveSound(String name, Sound fallback) {
        if (name == null) return fallback;
        try {
            if (name.contains(":")) {
                String enumName = name.substring(name.indexOf(":") + 1).replace('.', '_').toUpperCase();
                return Sound.valueOf(enumName);
            } else {
                return Sound.valueOf(name);
            }
        } catch (Exception e) {
            return fallback;
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location graveLocation = player.getLocation().getBlock().getLocation();
        Inventory graveInventory = Bukkit.createInventory(null, 54, "Grave");
        for (ItemStack item : event.getDrops()) {
            if (item != null) {
                graveInventory.addItem(item.clone());
            }
        }
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        if (offHandItem != null && offHandItem.getType() != Material.AIR) {
            graveInventory.addItem(offHandItem.clone());
        }
        event.getDrops().clear();
        graveLocation.getBlock().setType(Material.PLAYER_HEAD);
        Skull skull = (Skull) graveLocation.getBlock().getState();
        skull.setOwningPlayer(player);
        skull.update();
        graves.put(graveLocation, graveInventory);
        graveOwners.put(graveLocation, player.getUniqueId());
        storage.saveGrave(graveLocation, player.getUniqueId(), graveInventory);
        if (messages.isEnabled("grave-placed")) messages.send("grave-placed", player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.PLAYER_HEAD) {
            return;
        }
        Location loc = clickedBlock.getLocation();
        if (!graves.containsKey(loc)) {
            return;
        }
        event.setCancelled(true);
        Inventory graveInventory = graves.get(loc);
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            List<ItemStack> itemsLeftInGrave = new ArrayList<>();
            boolean itemsEffectivelyMoved = false;
            PlayerInventory playerInv = player.getInventory();
            ItemStack[] currentGraveContentsSnapshot = graveInventory.getContents().clone();
            graveInventory.clear();
            for (ItemStack itemFromGrave : currentGraveContentsSnapshot) {
                if (itemFromGrave == null || itemFromGrave.getType() == Material.AIR) {
                    continue;
                }
                ItemStack currentItem = itemFromGrave.clone();
                boolean equippedOrFullyAdded = false;
                Material itemType = currentItem.getType();
                if (itemType.name().endsWith("_HELMET")) {
                    if (playerInv.getHelmet() == null || playerInv.getHelmet().getType() == Material.AIR) {
                        playerInv.setHelmet(currentItem);
                        itemsEffectivelyMoved = true;
                        equippedOrFullyAdded = true;
                    }
                } else if (itemType.name().endsWith("_CHESTPLATE")) {
                    if (playerInv.getChestplate() == null || playerInv.getChestplate().getType() == Material.AIR) {
                        playerInv.setChestplate(currentItem);
                        itemsEffectivelyMoved = true;
                        equippedOrFullyAdded = true;
                    }
                } else if (itemType.name().endsWith("_LEGGINGS")) {
                    if (playerInv.getLeggings() == null || playerInv.getLeggings().getType() == Material.AIR) {
                        playerInv.setLeggings(currentItem);
                        itemsEffectivelyMoved = true;
                        equippedOrFullyAdded = true;
                    }
                } else if (itemType.name().endsWith("_BOOTS")) {
                    if (playerInv.getBoots() == null || playerInv.getBoots().getType() == Material.AIR) {
                        playerInv.setBoots(currentItem);
                        itemsEffectivelyMoved = true;
                        equippedOrFullyAdded = true;
                    }
                }
                if (!equippedOrFullyAdded) {
                    HashMap<Integer, ItemStack> didNotFit = playerInv.addItem(currentItem.clone());
                    if (didNotFit.isEmpty()) {
                        itemsEffectivelyMoved = true;
                        equippedOrFullyAdded = true;
                    } else {
                        itemsLeftInGrave.addAll(didNotFit.values());
                        int originalAmount = currentItem.getAmount();
                        int leftoverAmount = 0;
                        for(ItemStack s : didNotFit.values()) leftoverAmount += s.getAmount();
                        if (leftoverAmount < originalAmount) {
                            itemsEffectivelyMoved = true;
                        }
                    }
                }
            }
            for (ItemStack itemToKeep : itemsLeftInGrave) {
                graveInventory.addItem(itemToKeep);
            }
            if (itemsEffectivelyMoved) {
                storage.saveGrave(loc, graveOwners.get(loc), graveInventory);
                if (messages.isEnabled("items-transferred")) messages.send("items-transferred", player);
            } else {
                if (messages.isEnabled("items-not-transferred")) messages.send("items-not-transferred", player);
            }
            boolean isEmpty = true;
            for (ItemStack item : graveInventory.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    isEmpty = false;
                    break;
                }
            }
            if (isEmpty) {
                removeGrave(loc, "emptied by quick loot/auto-equip");
            }
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            player.openInventory(graveInventory);
            openGraveLocations.put(graveInventory, loc);
            Sound s = resolveSound(soundOpenGraveName, Sound.BLOCK_CHEST_OPEN);
            if (s != null) player.playSound(player.getLocation(), s, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.PLAYER_HEAD) {
            Location loc = block.getLocation();
            if (graves.containsKey(loc)) {
                Inventory inv = graves.get(loc);
                for (ItemStack item : inv.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        block.getWorld().dropItemNaturally(loc, item);
                    }
                }
                removeGrave(loc, "broken");
                event.setDropItems(false);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (openGraveLocations.containsKey(inventory)) {
            Location loc = openGraveLocations.get(inventory);
            if (event.getView().getTitle().equals("Grave")) {
                boolean isEmpty = true;
                for (ItemStack item : inventory.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        isEmpty = false;
                        break;
                    }
                }
                if (isEmpty) {
                    removeGrave(loc, "emptied manually");
                }
            }
            openGraveLocations.remove(inventory);
        }
    }

    private void removeGrave(Location loc, String reason) {
        Block block = loc.getBlock();
        if (block.getType() == Material.PLAYER_HEAD) {
            block.setType(Material.AIR);
        }
        storage.deleteGrave(loc);
        graves.remove(loc);
        graveOwners.remove(loc);
        Sound s = resolveSound(soundRemoveGraveName, Sound.ENTITY_WITHER_DEATH);
        for (Player p : block.getWorld().getPlayers()) {
            if (p.getLocation().distance(loc) < 8 && s != null) {
                p.playSound(loc, s, SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
        }
        plugin.getLogger().info("Grave at " + loc.toString() + " removed because it was " + reason + ".");
    }
}
