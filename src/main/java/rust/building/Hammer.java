package rust.building;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Hammer {

    public static ItemStack createItem() {
        ItemStack item = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Hammer");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isHammer(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_AXE || !item.hasItemMeta()) return false;
        return item.getItemMeta().getDisplayName().contains("Hammer");
    }

    public static void removeStructure(FoundationManager.StructureTarget target) {
        Location center = target.center;
        
        // Проигрываем звук разрушения несколько раз с разной тональностью для эффекта "треска"
        for (int i = 0; i < 4; i++) {
            center.getWorld().playSound(center, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.BLOCKS, 1.0f, 0.6f + (float)Math.random() * 0.6f);
        }

        if (target.type.equals("foundation") || target.type.equals("ceiling")) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Block b = center.clone().add(x, 0, z).getBlock();
                    if (b.getType() == Material.PINK_WOOL || b.getType() == Material.OAK_WOOD) {
                        b.setType(Material.AIR);
                        center.getWorld().spawnParticle(Particle.CLOUD, b.getLocation().add(0.5, 0.5, 0.5), 4, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            }
            // Если это был фундамент, освобождаем место в сетке менеджера
            if (target.type.equals("foundation")) {
                FoundationManager.getPlacedFoundations().removeIf(loc -> 
                    loc.getBlockX() == center.getBlockX() && 
                    loc.getBlockY() == center.getBlockY() && 
                    loc.getBlockZ() == center.getBlockZ()
                );
            }
        } else if (target.type.equals("wall")) {
            for (int y = 0; y < 4; y++) {
                for (int i = -2; i <= 2; i++) {
                    Location loc = center.clone();
                    if (target.alongX) loc.add(i, y, 0);
                    else loc.add(0, y, i);
                    
                    Block b = loc.getBlock();
                    if (b.getType() == Material.PINK_WOOL || b.getType() == Material.OAK_WOOD) {
                        b.setType(Material.AIR);
                        center.getWorld().spawnParticle(Particle.CLOUD, b.getLocation().add(0.5, 0.5, 0.5), 4, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            }
        }
    }
}