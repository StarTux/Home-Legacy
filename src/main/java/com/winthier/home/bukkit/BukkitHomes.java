package com.winthier.home.bukkit;

import com.avaje.ebean.EbeanServer;
import com.winthier.home.Homes;
import com.winthier.home.Permission;
import com.winthier.home.sql.HomeRow;
import com.winthier.playercache.PlayerCache;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class BukkitHomes extends Homes {
    private final BukkitHomePlugin plugin;

    public BukkitHomes(BukkitHomePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public EbeanServer getDatabase() {
        return plugin.getDatabase();
    }

    @Override
    public InputStream getResource(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (file.exists()) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException fnfe) {
                // do nothing
            }
        }
        return plugin.getResource(path);
    }
    
    @Override
    public BukkitHome homeForRow(HomeRow row) {
        return new BukkitHome(row);
    }

    @Override
    public boolean setCurrentLocation(HomeRow row, UUID uuid) {
        final Player player = Bukkit.getServer().getPlayer(uuid);
        if (player == null) return false;
        final Location loc = player.getLocation();
        row.setLocation(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        return true;
    }
    
    @Override
    public String getPlayerName(UUID uuid) {
        String result;
        result = PlayerCache.nameForUuid(uuid);
        if (result == null) result =  Bukkit.getServer().getOfflinePlayer(uuid).getName();
        return result;
    }

    @Override
    public UUID getPlayerUuid(String name) {
        UUID result;
        result = PlayerCache.uuidForName(name);
        // if (result == null) {
        //     @SuppressWarnings("deprecation")
        //         final val tmp = Bukkit.getServer().getOfflinePlayer(name).getUniqueId();
        //     result = tmp;
        // }
        return result;
    }
    
    @Override
    public boolean hasMoney(UUID uuid, double amount) {
        if (amount < 0.0) throw new IllegalArgumentException("Amount must be positive");
        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(uuid);
        return plugin.getEconomy().has(player, amount);
    }

    @Override
    public boolean giveMoney(UUID uuid, double amount) {
        if (amount < 0.0) throw new IllegalArgumentException("Amount must be positive");
        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(uuid);
        plugin.getLogger().info(String.format("Gave %s to %s.", formatMoney(amount), getPlayerName(uuid)));
        return plugin.getEconomy().depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean takeMoney(UUID uuid, double amount) {
        if (amount < 0.0) throw new IllegalArgumentException("Amount must be positive");
        OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(uuid);
        plugin.getLogger().info(String.format("Took %s from %s.", formatMoney(amount), getPlayerName(uuid)));
        return plugin.getEconomy().withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean sendJsonMessage(UUID uuid, String json) {
        final Player player = Bukkit.getServer().getPlayer(uuid);
        if (player == null) return false;
        final CommandSender console = Bukkit.getServer().getConsoleSender();
        final String command = "minecraft:tellraw " + player.getName() + " " + json;
        // System.out.println("Command: " + command);
        Bukkit.getServer().dispatchCommand(console, command);
        return true;
    }

    @Override
    public boolean sendSubtitle(UUID uuid, String msg) {
        final Player player = Bukkit.getServer().getPlayer(uuid);
        if (player == null) return false;
        player.sendTitle("", ChatColor.translateAlternateColorCodes('&', msg));
        return true;
    }
    
    @Override
    public boolean playerHasRank(UUID uuid, String rank) {
        final OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(uuid);
        return plugin.getPermission().playerInGroup((String)null, player, rank);
    }

    @Override
    public boolean playerHasPermission(UUID sender, Permission perm) {
        final Player player = Bukkit.getServer().getPlayer(sender);
        if (player == null) return false;
        return player.hasPermission(perm.key);
    }

    @Override
    public String formatMoney(double amount) {
        return plugin.getEconomy().format(amount);
    }

    @Override
    public boolean msg(UUID sender, String msg, Object... args) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) msg = String.format(msg, args);
        if (sender == null) {
            Bukkit.getServer().getConsoleSender().sendMessage(msg);
        } else {
            Player player = Bukkit.getServer().getPlayer(sender);
            if (player == null) return false;
            player.sendMessage(msg);
        }
        return true;
    }

    @Override
    public String getServerName() {
        return plugin.getServerName();
    }

    @Override
    public boolean doCheckClaims() {
        if (!plugin.getConfig().getBoolean("CheckClaims", false)) return false;
        return (Bukkit.getServer().getPluginManager().getPlugin("Claims") != null);
    }
    
    @Override
    public void onReload() {
        plugin.reloadConfig();
    }
}
