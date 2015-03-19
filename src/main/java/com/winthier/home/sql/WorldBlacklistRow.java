package com.winthier.home.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(
    name = "world_blacklist",
    uniqueConstraints = @UniqueConstraint(columnNames={"server_id", "world_id"})
    )
@Getter
@Setter
public class WorldBlacklistRow {
    private static Set<String> cache = null;

    @Id
    private Integer id;

    @ManyToOne(optional=false)
    private ServerRow server;
    
    @ManyToOne(optional=false)
    private WorldRow world;

    public static List<String> getAllNames() {
        if (cache == null) {
            cache = new HashSet<String>();
            for (WorldBlacklistRow row : DB.get().find(WorldBlacklistRow.class).where().eq("server", ServerRow.getThisServer()).findList()) {
                cache.add(row.getWorld().getName());
            }
        }
        return new ArrayList<String>(cache);
    }

    static WorldBlacklistRow find(@NonNull WorldRow world) {
        return DB.unique(DB.get().find(WorldBlacklistRow.class).where().eq("server", ServerRow.getThisServer()).eq("world", world).findList());
    }

    static boolean blacklistWorld(@NonNull WorldRow world) {
        if (find(world) != null) return false;
        WorldBlacklistRow result = new WorldBlacklistRow();
        result.setServer(ServerRow.getThisServer());
        result.setWorld(world);
        DB.get().save(result);
        clearCache();
        return true;
    }

    public static boolean blacklistWorld(@NonNull String worldName) {
        WorldRow world = WorldRow.findOrCreate(worldName);
        return blacklistWorld(world);
    }

    public static boolean removeFromBlacklist(@NonNull String worldName) {
        WorldRow world = WorldRow.find(worldName);
        if (world == null) return false;
        WorldBlacklistRow row = find(world);
        if (row == null) return false;
        DB.get().delete(row);
        clearCache();
        return true;
    }

    public static boolean isBlacklisted(@NonNull String worldName) {
        return getAllNames().contains(worldName);
    }

    public static void clearCache() {
        cache = null;
    }
}

