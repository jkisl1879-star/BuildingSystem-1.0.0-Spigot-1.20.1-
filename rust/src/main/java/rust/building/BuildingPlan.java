package rust.building;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public abstract class BuildingPlan {
    private final String id;
    private final String displayName;
    private final Material icon;

    public BuildingPlan(String id, String displayName, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }

    public abstract void build(Player player, Location origin);
    public abstract String getValidationError(Location origin);
}