package rust;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import rust.building.BuildingPlan;
import rust.building.Ceiling;
import rust.building.Foundation;
import rust.building.FoundationManager;
import rust.building.Hammer;
import rust.building.Wall;

import java.util.*;

public class BuildingPlanPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, BuildingPlan> activeSessions = new HashMap<>();
    private final Map<UUID, FoundationManager.StructureTarget> hammerSessions = new HashMap<>();
    private final Map<UUID, List<BlockDisplay>> ghosts = new HashMap<>();
    private final Map<String, BuildingPlan> availablePlans = new HashMap<>();
    private BukkitRunnable ghostTask;

    @Override
    public void onEnable() {
        if (getCommand("buildingplan") != null) {
            getCommand("buildingplan").setExecutor(this);
            getCommand("buildingplan").setTabCompleter(this);
        }
        if (getCommand("hammer") != null) {
            getCommand("hammer").setExecutor(this);
        }
        getServer().getPluginManager().registerEvents(this, this);
        
        registerPlan(new Foundation());
        registerPlan(new Wall()); 
        registerPlan(new Ceiling());
        
        startGhostTask();
        getLogger().info("BuildingPlan enabled.");
    }

    private void registerPlan(BuildingPlan plan) {
        availablePlans.put(plan.getId(), plan);
    }

    @Override
    public void onDisable() {
        if (ghostTask != null) ghostTask.cancel();
        clearAllGhosts();
        FoundationManager.clearAll();
        getLogger().info("BuildingPlan disabled.");
    }

    private boolean isHoldingPlan(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.STICK || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Building Plan");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("hammer")) {
            if (!sender.hasPermission("hammer.give")) return true;
            if (args.length == 0 || !args[0].equalsIgnoreCase("give")) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /hammer give [player]");
                return true;
            }
            Player target = (args.length >= 2) ? Bukkit.getPlayerExact(args[1]) : (sender instanceof Player ? (Player) sender : null);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            target.getInventory().addItem(Hammer.createItem());
            target.sendMessage(ChatColor.GREEN + "You received a Hammer.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("buildingplan")) {
            if (!sender.hasPermission("buildingplan.give")) return true;
            if (args.length == 0 || !args[0].equalsIgnoreCase("give")) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /buildingplan give [player]");
                return true;
            }
            Player target = (args.length >= 2) ? Bukkit.getPlayerExact(args[1]) : (sender instanceof Player ? (Player) sender : null);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            target.getInventory().addItem(createBuildingPlanItem());
            target.sendMessage(ChatColor.GREEN + "You received a Building Plan.");
            return true;
        }
        return false;
    }

    private ItemStack createBuildingPlanItem() {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.BLUE + "Building Plan");
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // ЛОГИКА HAMMER (КИЯНКИ)
        if (Hammer.isHammer(item) && event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            Block clicked = event.getClickedBlock();
            if (clicked != null && (clicked.getType() == Material.PINK_WOOL || clicked.getType() == Material.OAK_WOOD)) {
                FoundationManager.StructureTarget target = FoundationManager.getStructureAt(clicked);
                if (target != null) {
                    Inventory gui = Bukkit.createInventory(null, 9, "§8Hammer Menu");
                    ItemStack removeBtn = new ItemStack(Material.BARRIER);
                    ItemMeta meta = removeBtn.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("§cRemove");
                        removeBtn.setItemMeta(meta);
                    }
                    gui.setItem(4, removeBtn);
                    
                    player.openInventory(gui);
                    hammerSessions.put(player.getUniqueId(), target);
                }
            }
            return;
        }

        // ЛОГИКА BUILDING PLAN (ПЛАНА)
        if (activeSessions.containsKey(player.getUniqueId())) return;
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Building Plan")) return;
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) return;

        event.setCancelled(true);

        Inventory gui = Bukkit.createInventory(null, 27, "§8Select Building");
        int slot = 10;
        for (BuildingPlan plan : availablePlans.values()) {
            ItemStack icon = new ItemStack(plan.getIcon());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plan.getDisplayName());
                icon.setItemMeta(meta);
            }
            gui.setItem(slot++, icon);
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals("§8Hammer Menu")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.BARRIER && clicked.hasItemMeta() && clicked.getItemMeta().getDisplayName().equals("§cRemove")) {
                FoundationManager.StructureTarget target = hammerSessions.remove(player.getUniqueId());
                if (target != null) {
                    Hammer.removeStructure(target);
                    player.sendMessage("§a[Hammer] Structure removed!");
                }
                player.closeInventory();
            }
            return;
        }

        if (title.equals("§8Select Building")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

            String clickedName = clicked.getItemMeta().getDisplayName();

            for (BuildingPlan plan : availablePlans.values()) {
                if (clickedName.equals(plan.getDisplayName())) {
                    activeSessions.put(player.getUniqueId(), plan);
                    player.closeInventory();
                    player.sendMessage("§a[Build] Selected: " + plan.getDisplayName());
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onBuildInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!activeSessions.containsKey(player.getUniqueId())) return;
        if (!isHoldingPlan(player)) return;

        event.setCancelled(true);

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            activeSessions.remove(player.getUniqueId());
            clearGhost(player);
            player.sendMessage("§c[Build] Canceled.");
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
            BuildingPlan plan = activeSessions.get(player.getUniqueId());
            Location rawLoc = getTargetLocation(player);
            
            if (rawLoc != null) {
                Location finalLoc;
                if (plan.getId().equals("wall")) {
                    finalLoc = FoundationManager.snapWallToEdge(rawLoc);
                } else if (plan.getId().equals("ceiling")) {
                    finalLoc = FoundationManager.snapCeiling(rawLoc);
                } else {
                    finalLoc = FoundationManager.snapToGrid(rawLoc);
                }
                
                String error = plan.getValidationError(finalLoc);
                if (error == null) {
                    plan.build(player, finalLoc);
                    activeSessions.remove(player.getUniqueId());
                    clearGhost(player);
                    spawnBuildParticles(finalLoc, plan.getId());
                    player.sendMessage("§a[Build] Built successfully!");
                } else {
                    player.sendMessage(error);
                }
            }
        }
    }

    private void spawnBuildParticles(Location center, String planId) {
        new BukkitRunnable() {
            int ticks = 0;
            int soundsPlayed = 0;

            @Override
            public void run() {
                if (ticks >= 40) {
                    this.cancel();
                    return;
                }

                if (soundsPlayed < 3) {
                    float pitch = 0.8f + (soundsPlayed * 0.1f);
                    center.getWorld().playSound(center, org.bukkit.Sound.BLOCK_SCAFFOLDING_PLACE, org.bukkit.SoundCategory.BLOCKS, 1.0f, pitch);
                    soundsPlayed++;
                }

                if (planId.equals("wall")) {
                    boolean alongX = (center.getYaw() == 0f);
                    for (int i = 0; i < 15; i++) {
                        double offsetX = alongX ? (Math.random() * 5 - 2.5) : (Math.random() * 1.5 - 0.75);
                        double offsetY = Math.random() * 4;
                        double offsetZ = alongX ? (Math.random() * 1.5 - 0.75) : (Math.random() * 5 - 2.5);
                        center.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(offsetX, offsetY, offsetZ), 1, 0, 0, 0, 0.02);
                    }
                } else {
                    for (int i = 0; i < 20; i++) {
                        double offsetX = Math.random() * 5 - 2.5;
                        double offsetY = Math.random() * 1 - 0.5;
                        double offsetZ = Math.random() * 5 - 2.5;
                        center.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(offsetX, offsetY, offsetZ), 1, 0, 0, 0, 0.02);
                    }
                }

                ticks += 5;
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            if (!isHoldingPlan(event.getPlayer())) {
                clearGhost(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activeSessions.remove(event.getPlayer().getUniqueId());
        hammerSessions.remove(event.getPlayer().getUniqueId());
        clearGhost(event.getPlayer());
    }

    private void startGhostTask() {
        ghostTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    BuildingPlan plan = activeSessions.get(player.getUniqueId());
                    if (plan == null || !isHoldingPlan(player)) {
                        clearGhost(player);
                        continue;
                    }

                    Location rawLoc = getTargetLocation(player);
                    if (rawLoc == null) {
                        clearGhost(player);
                        continue;
                    }

                    Location finalLoc;
                    if (plan.getId().equals("wall")) {
                        finalLoc = FoundationManager.snapWallToEdge(rawLoc);
                    } else if (plan.getId().equals("ceiling")) {
                        finalLoc = FoundationManager.snapCeiling(rawLoc);
                    } else {
                        finalLoc = FoundationManager.snapToGrid(rawLoc);
                    }
                    
                    renderGhost(player, finalLoc, plan.getValidationError(finalLoc) == null, plan.getId());
                }
            }
        };
        ghostTask.runTaskTimer(this, 0L, 2L);
    }

    private void renderGhost(Player player, Location center, boolean isValid, String planId) {
        List<BlockDisplay> playerGhosts = ghosts.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        int index = 0;

        Material mat = isValid ? Material.LIGHT_BLUE_STAINED_GLASS : Material.RED_STAINED_GLASS;

        if (planId.equals("foundation") || planId.equals("ceiling")) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Location loc = center.clone().add(x, 0, z);
                    updateGhostDisplay(player.getWorld(), loc, mat, playerGhosts, index);
                    index++;
                }
            }
        } else if (planId.equals("wall")) {
            boolean alongX = (center.getYaw() == 0f); 
            for (int y = 0; y < 4; y++) {
                for (int i = -2; i <= 2; i++) {
                    Location loc = center.clone();
                    if (alongX) {
                        loc.add(i, y, 0);
                    } else {
                        loc.add(0, y, i);
                    }
                    updateGhostDisplay(player.getWorld(), loc, mat, playerGhosts, index);
                    index++;
                }
            }
        }

        while (playerGhosts.size() > index) {
            playerGhosts.remove(playerGhosts.size() - 1).remove();
        }
    }

    private void updateGhostDisplay(org.bukkit.World world, Location loc, Material mat, List<BlockDisplay> list, int index) {
        Location centeredLoc = loc.clone().add(0.5, 0, 0.5);
        
        if (index >= list.size()) {
            BlockDisplay display = world.spawn(centeredLoc, BlockDisplay.class);
            display.setBlock(mat.createBlockData());
            display.setTransformation(new Transformation(
                    new Vector3f(-0.5f, 0f, -0.5f), new AxisAngle4f(), new Vector3f(1f, 1f, 1f), new AxisAngle4f()
            ));
            list.add(display);
        } else {
            BlockDisplay display = list.get(index);
            display.teleport(centeredLoc);
            display.setBlock(mat.createBlockData());
        }
    }

    private void clearGhost(Player player) {
        List<BlockDisplay> list = ghosts.remove(player.getUniqueId());
        if (list != null) {
            for (BlockDisplay display : list) display.remove();
        }
    }

    private void clearAllGhosts() {
        for (List<BlockDisplay> list : ghosts.values()) {
            for (BlockDisplay display : list) display.remove();
        }
        ghosts.clear();
    }

    private Location getTargetLocation(Player player) {
        Block targetBlock = player.getTargetBlockExact(6);
        if (targetBlock != null && targetBlock.getType() != Material.AIR) {
            if (player.getLocation().getPitch() > 45) {
                return targetBlock.getLocation();
            }
            return targetBlock.getLocation().add(0, 1, 0);
        }
        return null;
    }
}