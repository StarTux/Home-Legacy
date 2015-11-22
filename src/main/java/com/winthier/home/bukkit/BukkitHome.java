package com.winthier.home.bukkit;

import com.winthier.home.Home;
import com.winthier.home.sql.HomeRow;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

@Data
@EqualsAndHashCode(callSuper=true)
public class BukkitHome extends Home {
    private final Location location;

    public BukkitHome(UUID owner, String name, Location location) {
        super(owner, name);
        this.location = location;
    }

    public BukkitHome(HomeRow row) {
        super(row);
        World world = Bukkit.getServer().getWorld(row.getWorld().getName());
        if (world == null) world = Bukkit.getServer().getWorlds().get(0);
        this.location = new Location(world, row.getX(), row.getY(), row.getZ(), row.getYaw(), row.getPitch());
    }

    @Override
    public boolean teleport(UUID uuid) {
        Player player = Bukkit.getServer().getPlayer(uuid);
        if (player == null) return false;
        player.eject();
        return player.teleport(location);
    }
}
