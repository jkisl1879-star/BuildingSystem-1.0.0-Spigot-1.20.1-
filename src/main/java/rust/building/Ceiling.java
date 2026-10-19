package rust.building;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class Ceiling extends BuildingPlan {

    public Ceiling() {
        super("ceiling", "§dCeiling (5x5)", Material.PINK_WOOL);
    }

    @Override
    public void build(Player player, Location origin) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Material mat = (Math.abs(x) == 2 || Math.abs(z) == 2) ? Material.OAK_WOOD : Material.PINK_WOOL;
                origin.clone().add(x, 0, z).getBlock().setType(mat);
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
                
                if (dx == 0 && dz == 0 && dy >= 4 && dy % 4 == 0) {
                    foundBase = existing;
                    break;
                }
            }
        }

        if (foundBase == null) return "§cCeiling must be placed directly above a foundation grid!";

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Block current = origin.clone().add(x, 0, z).getBlock();
                Material type = current.getType();
                
                // Проверяем центральные 3x3 блока (они не могут пересекаться со стенами и должны быть пустными)
                if (Math.abs(x) < 2 && Math.abs(z) < 2) {
                    if (type != Material.AIR) {
                        return "§cThere is already an object here!";
                    }
                } else {
                    // Края могут пересекаться с балками дуба или шерстью от стен
                    if (type != Material.AIR && type != Material.OAK_WOOD && type != Material.PINK_WOOL) {
                        return "§cArea is obstructed!";
                    }
                }
            }
        }

        return null;
    }
}