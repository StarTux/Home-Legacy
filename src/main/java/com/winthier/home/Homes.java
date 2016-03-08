package com.winthier.home;

import com.avaje.ebean.EbeanServer;
import com.winthier.home.sql.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

/**
 * This is the central singleton class of the Home plugin. The
 * BukkitHomePlugin class will create its subclass implementing
 * the abstract methods. This design will hopefully make the
 * plugin fairly easy to port to another API.
 *
 * Collections of repetetive methods have been grouped and moved
 * to other classes, such as HomeCommands and HomeActions, to
 * avoid cluttering this class and so the whole project stays
 * readable.
 */
public abstract class Homes {
    @Getter
    private static Homes instance;
    @Getter
    private final HomeCommands commands = new HomeCommands(this);
    @Getter
    private final AdminCommands adminCommands = new AdminCommands(this);
    @Getter
    private final HomeActions actions = new HomeActions(this);

    protected Homes() {
        instance = this;
    }

    public void enable() {
        reload();
    }

    public void disable() {
        reload();
    }

    // Methods to override

    public abstract EbeanServer getDatabase();
    public abstract InputStream getResource(String path);
    public abstract Home homeForRow(HomeRow row);
    public abstract boolean setCurrentLocation(HomeRow row, UUID player);
    public abstract String getPlayerName(UUID player);
    public abstract UUID getPlayerUuid(String name);
    public abstract boolean hasMoney(UUID player, double amount);
    public abstract boolean giveMoney(UUID player, double amount);
    public abstract boolean takeMoney(UUID player, double amount);
    public abstract boolean sendJsonMessage(UUID player, String json);
    public abstract boolean sendSubtitle(UUID player, String msg);
    public abstract boolean playerHasRank(UUID player, String rank);
    public abstract boolean playerHasPermission(UUID sender, Permission perm);
    public abstract String formatMoney(double amount);
    public abstract boolean msg(UUID sender, String msg, Object... args);
    public abstract String getServerName();
    public abstract boolean doCheckClaims();
    public void onReload() {}

    // API functions

    public Home findHome(UUID player, String name) {
        HomeRow row = HomeRow.find(player, name);
        if (row == null) return null;
        return homeForRow(row);
    }

    public List<Home> findHomes(UUID player) {
        List<HomeRow> rows = HomeRow.findAll(player);
        List<Home> result = new ArrayList<>(rows.size());
        for (HomeRow row : rows) result.add(homeForRow(row));
        return result;
    }

    public int countHomes(UUID player) {
        return HomeRow.count(player);
    }

    public int getTotalMaxHomes(UUID player) {
        return PlayerRow.findOrCreate(player).getTotalMaxHomes();
    }

    final void reload() {
        Message.clearStore();
        Rank.clearStore();
        ServerRow.clearCache();
        WorldRow.clearCache();
        PlayerRow.clearCache();
        WorldBlacklistRow.clearCache();
        onReload();
    }
}
