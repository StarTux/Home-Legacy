package com.winthier.home;

import com.avaje.ebean.EbeanServer;
import com.winthier.home.sql.HomeRow;
import java.io.InputStream;
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
        Message.clearStore();
    }

    public void disable() {
        Message.clearStore();
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
    public abstract boolean sendJSONMessage(UUID player, String json);
    public abstract boolean playerHasRank(UUID player, String rank);
    public abstract boolean playerHasPermission(UUID sender, Permission perm);
    public abstract String formatMoney(double amount);
    public abstract boolean msg(UUID sender, String msg, Object... args);

    // API functions

    public Home findHome(UUID player, String name) {
        HomeRow row = HomeRow.find(player, name);
        if (row == null) return null;
        return homeForRow(row);
    }

    public int countHomes(UUID player) {
        return HomeRow.find(player).size();
    }

    public void reload() {
        Message.clearStore();
        Rank.clearStore();
    }
}
