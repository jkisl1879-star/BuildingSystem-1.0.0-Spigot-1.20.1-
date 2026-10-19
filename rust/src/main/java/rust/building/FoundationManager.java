package rust.building;

import org.bukkit.Location;
import org.bukkit.block.Block;
import java.util.ArrayList;
import java.util.List;

public class FoundationManager {
    private static final List<Location> placedFoundations = new ArrayList<>();

    // Класс для хранения данных о найденной постройке
    public static class StructureTarget {
        public final String type;
        public final Location center;
        public final boolean alongX;

        public StructureTarget(String type, Location center, boolean alongX) {
            this.type = type;
            this.center = center;
            this.alongX = alongX;
        }
    }

    public static void registerFoundation(Location center) {
        placedFoundations.add(new Location(center.getWorld(), center.getBlockX(), center.getBlockY(), center.getBlockZ()));
    }

    public static List<Location> getPlacedFoundations() {
        return placedFoundations;
    }

    // Вычисляет, к какой постройке относится блок (для Hammer)
    public static StructureTarget getStructureAt(Block block) {
        Location loc = block.getLocation();
        Location closestBase = null;
        double minDist = 4.0;

        for (Location base : placedFoundations) {
            if (base.getWorld().equals(loc.getWorld())) {
                int dx = Math.abs(base.getBlockX() - loc.getBlockX());
                int dz = Math.abs(base.getBlockZ() - loc.getBlockZ());
                // Блок должен быть в пределах 5x5 зоны над фундаментом
                if (dx <= 2 && dz <= 2) {
                    closestBase = base;
                    break;
                }
            }
        }

        if (closestBase == null) return null;

        int dy = loc.getBlockY() - closestBase.getBlockY();
        if (dy < 0) return null;

        if (dy == 0) {
            return new StructureTarget("foundation", closestBase, false);
        } else if (dy >= 4 && dy % 4 == 0) {
            Location ceilCenter = closestBase.clone().add(0, dy, 0);
            return new StructureTarget("ceiling", ceilCenter, false);
        } else {
            // Если это не 0 и не 4,8,12... значит это Стена
            int level = (dy - 1) / 4;
            int wallBaseY = closestBase.getBlockY() + 1 + (level * 4);
            
            int dx = loc.getBlockX() - closestBase.getBlockX();
            int dz = loc.getBlockZ() - closestBase.getBlockZ();
            
            // Стена всегда стоит на краю (смещение 2 по одной из осей)
            if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                boolean alongX = (Math.abs(dz) == 2); // Если смещено по Z, значит стена тянется вдоль X
                
                Location wallCenter = closestBase.clone();
                wallCenter.setY(wallBaseY);
                if (alongX) {
                    wallCenter.setZ(closestBase.getBlockZ() + (dz > 0 ? 2 : -2));
                } else {
                    wallCenter.setX(closestBase.getBlockX() + (dx > 0 ? 2 : -2));
                }
                return new StructureTarget("wall", wallCenter, alongX);
            }
        }
        return null;
    }

    public static Location snapToGrid(Location rawLoc) {
        if (placedFoundations.isEmpty()) return rawLoc;
        
        Location closest = null;
        double minDistance = 10.0;
        
        for (Location existing : placedFoundations) {
            if (existing.getWorld().equals(rawLoc.getWorld())) {
                double dist = existing.distance(rawLoc);
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = existing;
                }
            }
        }
        
        if (closest == null) return rawLoc;

        int cx = closest.getBlockX();
        int cy = closest.getBlockY();
        int cz = closest.getBlockZ();
        
        int rx = rawLoc.getBlockX();
        int rz = rawLoc.getBlockZ();

        int dx = rx - cx;
        int dz = rz - cz;

        if (Math.abs(dx) > Math.abs(dz)) {
            return new Location(rawLoc.getWorld(), cx + (dx > 0 ? 4 : -4), cy, cz);
        } else {
            return new Location(rawLoc.getWorld(), cx, cy, cz + (dz > 0 ? 4 : -4));
        }
    }

    public static Location snapWallToEdge(Location rawLoc) {
        Location closest = null;
        double minDistance = 12.0;

        for (Location existing : placedFoundations) {
            if (existing.getWorld().equals(rawLoc.getWorld())) {
                double dist = Math.sqrt(Math.pow(existing.getBlockX() - rawLoc.getBlockX(), 2) + 
                                        Math.pow(existing.getBlockZ() - rawLoc.getBlockZ(), 2));
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = existing;
                }
            }
        }

        if (closest == null) return rawLoc;

        int cx = closest.getBlockX();
        int cy = closest.getBlockY();
        int cz = closest.getBlockZ();

        int rx = rawLoc.getBlockX();
        int ry = rawLoc.getBlockY();
        int rz = rawLoc.getBlockZ();

        int dx = rx - cx;
        int dz = rz - cz;

        int deltaY = ry - cy;
        int level = 0;
        if (deltaY > 0) {
            level = (deltaY - 1) / 4; 
        }
        if (level < 0) level = 0;

        int wallY = cy + 1 + (level * 4);
        Location wallCenter = new Location(closest.getWorld(), cx, wallY, cz);

        if (Math.abs(dx) > Math.abs(dz)) {
            wallCenter.setX(cx + (dx > 0 ? 2 : -2)); 
            wallCenter.setYaw(90); 
        } else {
            wallCenter.setZ(cz + (dz > 0 ? 2 : -2)); 
            wallCenter.setYaw(0);  
        }

        return wallCenter;
    }

    public static Location snapCeiling(Location rawLoc) {
        Location closest = null;
        double minDistance = 8.0;

        for (Location existing : placedFoundations) {
            if (existing.getWorld().equals(rawLoc.getWorld())) {
                double dist = Math.sqrt(Math.pow(existing.getBlockX() - rawLoc.getBlockX(), 2) + 
                                        Math.pow(existing.getBlockZ() - rawLoc.getBlockZ(), 2));
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = existing;
                }
            }
        }

        if (closest == null) return rawLoc;

        int cy = closest.getBlockY();
        int ry = rawLoc.getBlockY();
        int deltaY = ry - cy;
        
        int level = 1;
        if (deltaY > 2) {
            level = (deltaY + 1) / 4; 
        }
        if (level < 1) level = 1;

        return new Location(closest.getWorld(), closest.getBlockX(), cy + (level * 4), closest.getBlockZ());
    }

    public static boolean isAreaOccupied(Location newCenter) {
        for (Location existing : placedFoundations) {
            if (!existing.getWorld().equals(newCenter.getWorld())) continue;
            int dx = Math.abs(existing.getBlockX() - newCenter.getBlockX());
            int dz = Math.abs(existing.getBlockZ() - newCenter.getBlockZ());
            int dy = Math.abs(existing.getBlockY() - newCenter.getBlockY());
            
            if (dx < 4 && dz < 4 && dy < 1) return true;
        }
        return false;
    }

    public static void clearAll() {
        placedFoundations.clear();
    }
}