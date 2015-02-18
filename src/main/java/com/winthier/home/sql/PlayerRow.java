package com.winthier.home.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import com.winthier.home.Homes;
import com.winthier.home.Rank;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * A player owns all their homes, meaning that when a player gets
 * deleted, all their homes will be deleted, too.
 */
@Entity
@Table(
    name = "players",
    uniqueConstraints = @UniqueConstraint(columnNames={"uuid"})
    )
@Getter
@Setter
public class PlayerRow {
    @Id
    private Integer id;

    @NotNull
    private UUID uuid;

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
        return Homes.getInstance().getDatabase().find(PlayerRow.class).where().idEq(id).findUnique();
    }

    public static List<PlayerRow> forIds(List<Integer> ids) {
        return Homes.getInstance().getDatabase().find(PlayerRow.class).where().idIn(ids).findList();
    }

    public static PlayerRow find(UUID uuid) {
        final val list = Homes.getInstance().getDatabase().find(PlayerRow.class).where().eq("uuid", uuid).findList();
        return Util.unique(list);
    }

    public static PlayerRow findOrCreate(UUID uuid) {
        PlayerRow row = find(uuid);
        if (row == null) {
            row = new PlayerRow();
            row.setUuid(uuid);
            row.setExtraHomes(0);
            row.setDateCreated(new Date());
            Homes.getInstance().getDatabase().save(row);
        }
        return row;
    }

    public String getName() {
        return Homes.getInstance().getPlayerName(getUuid());
    }
}
