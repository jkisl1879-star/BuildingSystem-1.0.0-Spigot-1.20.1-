package rust.building;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class Foundation extends BuildingPlan {

    public Foundation() {
        super("foundation", "§dFoundation (5x5)", Material.PINK_WOOL);
    }

    @Override
    public void build(Player player, Location origin) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Material mat = (Math.abs(x) == 2 || Math.abs(z) == 2) ? Material.OAK_WOOD : Material.PINK_WOOL;
                origin.clone().add(x, 0, z).getBlock().setType(mat);
            }
        }
        FoundationManager.registerFoundation(origin);
    }

    @Override
    public String getValidationError(Location origin) {
        if (FoundationManager.isAreaOccupied(origin)) {
            return "§cCannot build over an existing foundation!";
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location loc = origin.clone().add(x, 0, z);
                Block current = loc.getBlock();
                Block below = loc.clone().subtract(0, 1, 0).getBlock();

                if (current.isLiquid() || below.isLiquid()) return "§cCannot build in water!";
                
                Material currType = current.getType();
                if (currType != Material.AIR && currType != Material.GRASS && currType != Material.SNOW && 
                    currType != Material.PINK_WOOL && currType != Material.OAK_WOOD) {
                    return "§cArea is obstructed!";
                }

                Material belowType = below.getType();
                
                if (belowType == Material.PINK_WOOL || belowType == Material.OAK_WOOD) return "§cCannot build on top of an existing foundation!";
                if (!belowType.isSolid() || belowType == Material.GRASS || belowType == Material.FERN) return "§cNeeds solid ground below!";
            }
        }
        return null;
    }
}