package com.winthier.home.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import com.winthier.home.Homes;
import com.winthier.home.Rank;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * A player owns all their homes, meaning that when a player gets
 * deleted, all their homes will be deleted, too.
 *
 * This class will only ever find players of this server by
 * implicitly searching for Home.getThisServer() via ServerRow.
 */
@Entity
@Table(
    name = "players",
    uniqueConstraints = @UniqueConstraint(columnNames={"uuid, server_id"})
    )
@Getter
@Setter
public class PlayerRow {
    private static final Map<UUID, PlayerRow> cache = new HashMap<>();
        
    @Id
    private Integer id;

    @NotNull
    private UUID uuid;

    @NotNull
    @ManyToOne
    private ServerRow server;

    // Max homes will be added to the standard homes which can be
    // configured by rank.
    @NotNull
    private Integer extraHomes;

    @NotNull
    private Date dateCreated;

    @Version
    private Integer version;

    // Referencing this

    // Cascade removal
    @OneToMany(mappedBy="owner", cascade=CascadeType.REMOVE)
    private List<HomeRow> homes;

    @OneToMany(mappedBy="invitee")
    private List<InviteRow> invites;

    @OneToMany(mappedBy="player")
    private List<IgnoreInviteRow> ignoreInvites;

    public int getTotalMaxHomes() {
        return Rank.forPlayer(getUuid()).getMaxHomes() + getExtraHomes();
    }

    public int giveExtraHome() {
        int count = getExtraHomes() + 1;
        setExtraHomes(count);
        return count;
    }

    public static PlayerRow forId(int id) {
        return DB.get().find(PlayerRow.class).where().idEq(id).findUnique();
    }

    public static PlayerRow find(@NonNull UUID uuid) {
        PlayerRow result = cache.get(uuid);
        if (result != null) return result;
        result = DB.unique(DB.get().find(PlayerRow.class).where().eq("uuid", uuid).eq("server", ServerRow.getThisServer()).findList());
        cache.put(uuid, result); // may insert null
        return result;
    }

    public static PlayerRow findOrCreate(@NonNull UUID uuid) {
        PlayerRow result = find(uuid);
        if (result == null) {
            result = new PlayerRow();
            result.setUuid(uuid);
            result.setServer(ServerRow.getThisServer());
            result.setExtraHomes(0);
            result.setDateCreated(new Date());
            result.save();
            cache.put(uuid, result);
        }
        return result;
    }

    public String getName() {
        return Homes.getInstance().getPlayerName(getUuid());
    }

    public void save() {
        DB.get().save(this);
    }

    public static void clearCache() {
        cache.clear();
    }
}
