package com.winthier.home.bukkit;

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
        // Write some files to disk
        saveResource("ranks.yml", false);
        //saveResource("messages.yml", false);
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
        // Setup commands to all homes.getCommand().*
        getCommand("homes").setExecutor(new CommandExecutor() { @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return homes.getCommands().homes(getUuid(sender), args); } });
        getCommand("home").setExecutor(new CommandExecutor() { @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return homes.getCommands().home(getUuid(sender), args); } });
        getCommand("sethome").setExecutor(new CommandExecutor() { @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return homes.getCommands().setHome(getUuid(sender), args); } });
        getCommand("invitehome").setExecutor(new CommandExecutor() { @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return homes.getCommands().inviteHome(getUuid(sender), args); } });
        getCommand("uninvitehome").setExecutor(new CommandExecutor() { @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return homes.getCommands().uninviteHome(getUuid(sender), args); } });
        getCommand("listhomes").setExecutor(new CommandExecutor() { @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return homes.getCommands().listHomes(getUuid(sender), args); } });
        getCommand("deletehome").setExecutor(new CommandExecutor() { @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return homes.getCommands().deleteHome(getUuid(sender), args); } });
        getCommand("listinvites").setExecutor(new CommandExecutor() { @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return homes.getCommands().listInvites(getUuid(sender), args); } });
        getCommand("listmyinvites").setExecutor(new CommandExecutor() { @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return homes.getCommands().listMyInvites(getUuid(sender), args); } });
        getCommand("deleteinvite").setExecutor(new CommandExecutor() { @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return homes.getCommands().deleteInvite(getUuid(sender), args); } });
        getCommand("buyhome").setExecutor(new CommandExecutor() { @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return homes.getCommands().buyHome(getUuid(sender), args); } });
        // Admin command
        getCommand("homeadmin").setExecutor(new CommandExecutor() { @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { return homes.getAdminCommands().command(getUuid(sender), args); } });
    }

    @Override
    public void onDisable() {
        homes.disable();
        homes = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String args[]) {
        return false;
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> result = new ArrayList<>();
        result.add(PlayerRow.class);
        result.add(WorldRow.class);
        result.add(HomeRow.class);
        result.add(InviteRow.class);
        result.add(IgnoreInviteRow.class);
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
}
