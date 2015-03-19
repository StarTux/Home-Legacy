package com.winthier.home.bukkit;

import com.winthier.home.HomeCommands;
import com.winthier.home.Homes;
import com.winthier.home.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.PersistenceException;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Serve as the JavaPlugin instance for Bukkit.
 */
public class BukkitHomePlugin extends JavaPlugin {
    private Homes homes;
    private Economy economy;
    private Permission permission;

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) economy = economyProvider.getProvider();
        return (economy != null);
    }

    private boolean setupPermission() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
        if (permissionProvider != null) permission = permissionProvider.getProvider();
        return (permission != null);
    }

    private static UUID getUuid(CommandSender sender) {
        if (sender instanceof Player) return ((Player)sender).getUniqueId();
        return null;
    }

    @Override
    public void onEnable() {
        reloadConfig();
        // Write some files to disk
        saveDefaultConfig();
        saveResource("ranks.yml", false);
        // Setup database
        try {
            for (Class<?> clazz : getDatabaseClasses()) {
                getDatabase().find(clazz).findRowCount();
            }
        } catch (PersistenceException ex) {
            System.out.println("Installing database for " + getDescription().getName() + " due to first time usage");
            installDDL();
        }
        homes = new BukkitHomes(this);
        homes.enable();
    }

    @Override
    public void onDisable() {
        homes.disable();
        homes = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String args[]) {
        HomeCommands.Type type = HomeCommands.Type.forString(command.getName());
        UUID uuid = null;
        if (sender instanceof Player) uuid = ((Player)sender).getUniqueId();
        return homes.getCommands().onCommand(type, uuid, args);
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> result = new ArrayList<>();
        result.add(HomeRow.class);
        result.add(IgnoreInviteRow.class);
        result.add(InviteRow.class);
        result.add(PlayerRow.class);
        result.add(ServerRow.class);
        result.add(WorldBlacklistRow.class);
        result.add(WorldRow.class);
        return result;
    }

    public Economy getEconomy() {
        if (economy == null && !setupEconomy()) {
            throw new RuntimeException("Failed to setup economy.");
        }
        return economy;
    }

    public Permission getPermission() {
        if (permission == null && !setupPermission()) {
            throw new RuntimeException("Failed to setup permission.");
        }
        return permission;
    }

    public String getServerName() {
        return getConfig().getString("ServerName", "default");
    }
}
