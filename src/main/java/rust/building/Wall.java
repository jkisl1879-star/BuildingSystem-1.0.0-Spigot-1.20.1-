package rust.building;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Wall extends BuildingPlan {

    public Wall() {
        super("wall", "§dWall (5x4)", Material.PINK_WOOL);
    }

    @Override
    public void build(Player player, Location origin) {
        boolean alongX = (origin.getYaw() == 0f); 

        for (int y = 0; y < 4; y++) {
            for (int i = -2; i <= 2; i++) {
                Location loc = origin.clone();
                if (alongX) {
                    loc.add(i, y, 0); 
                } else {
                    loc.add(0, y, i); 
                }
                
                Material mat = (Math.abs(i) == 2 || y == 3) ? Material.OAK_WOOD : Material.PINK_WOOL;
                loc.getBlock().setType(mat);
            }
        }
    }

    @Override
    public String getValidationError(Location origin) {
        Location foundBase = null;
        for (Location existing : FoundationManager.getPlacedFoundations()) {
            if (existing.getWorld().equals(origin.getWorld())) {
                int dx = Math.abs(existing.getBlockX() - origin.getBlockX());
                int dz = Math.abs(existing.getBlockZ() - origin.getBlockZ());
                int dy = origin.getBlockY() - existing.getBlockY();
                
                if (dy >= 1 && (dy - 1) % 4 == 0 && ((dx == 2 && dz == 0) || (dx == 0 && dz == 2))) {
                    foundBase = existing;
                    break;
                }
            }
        }

        if (foundBase == null) return "§cWall must be placed strictly on the edge of a foundation grid!";

        int dy = origin.getBlockY() - foundBase.getBlockY();
        if (dy > 1) {
            Material belowMat = origin.clone().subtract(0, 1, 0).getBlock().getType();
            if (belowMat == Material.AIR) {
                return "§cMust be placed on another wall!";
            }
        }

        // Проверка: свободно ли уникальное пространство самой стены
        boolean alongX = (origin.getYaw() == 0f); 
        for (int y = 1; y <= 2; y++) { // Проверяем только центральные ряды по высоте
            for (int i = -1; i <= 1; i++) { // И центральные по ширине
                Location loc = origin.clone();
                if (alongX) {
                    loc.add(i, y, 0); 
                } else {
                    loc.add(0, y, i); 
                }
                
                if (loc.getBlock().getType() != Material.AIR) {
                    return "§cThere is already a wall here!";
                }
            }
        }

        return null;
    }
}