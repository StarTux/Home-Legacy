package com.winthier.home.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import com.winthier.home.Homes;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Player owns Home owns Invite owns IgnoreInvite.
 *
 * An invite owns its ignores, so when an invite gets deleted, all
 * ignores get deleted, too.
 */
@Entity
@Table(
    name = "invites",
    uniqueConstraints = @UniqueConstraint(columnNames={"home_id", "invitee_id"})
    )
@Getter
@Setter
public class InviteRow {
    @Id
    private Integer id;

    @NotNull
    @ManyToOne
    private HomeRow home;

    // null means it's a public invite
    @ManyToOne
    private PlayerRow invitee;

    @NotNull
    private Date dateCreated;

    @Version
    private Integer version;

    // Referencing this

    @OneToMany(mappedBy="invite", cascade=CascadeType.REMOVE)
    private List<IgnoreInviteRow> ignores;

    public boolean isPublic() {
        return getInvitee() == null;
    }

    public static InviteRow forId(int id) {
        return DB.get().find(InviteRow.class).where().idEq(id).findUnique();
    }

    static InviteRow find(@NonNull HomeRow home, @NonNull PlayerRow invitee) {
        return DB.unique(DB.get().find(InviteRow.class).where().eq("home", home).eq("invitee", invitee).findList());
    }

    static List<InviteRow> findOrPublic(@NonNull HomeRow home, @NonNull PlayerRow invitee) {
        return DB.get().find(InviteRow.class).where().eq("home", home).or(DB.fact().isNull("invitee"), DB.fact().eq("invitee", invitee)).findList();
    }
    
    public static List<InviteRow> findAll(@NonNull HomeRow home) {
        return DB.get().find(InviteRow.class).where().eq("home", home).findList();
    }

    // List<InviteRow> findWithPublic(@NonNull HomeRow home, @NonNull PlayerRow invitee) {
    //     return DB.get().find(InviteRow.class).where().eq("home", home).or(DB.fact().isNull("invitee"), DB.fact().eq("invitee", invitee)).findList();
    // }
    
    // static List<InviteRow> findAll(@NonNull PlayerRow invitee) {
    //     return DB.get().find(InviteRow.class).where().eq("invitee", invitee).findList();
    // }

    // static List<InviteRow> findAllWithPublic(@NonNull PlayerRow invitee) {
    //     return DB.get().find(InviteRow.class).where().or(DB.fact().isNull("invitee"), DB.fact().eq("invitee", invitee)).findList();
    // }

    public static InviteRow findPublic(@NonNull HomeRow home) {
        return DB.unique(DB.get().find(InviteRow.class).where().eq("home", home).isNull("invitee").findList());
    }

    public static InviteRow find(UUID ownerUuid, String homeName, UUID inviteeUuid) {
        HomeRow home = HomeRow.find(ownerUuid, homeName);
        if (home == null) return null;
        PlayerRow invitee = PlayerRow.findOrCreate(inviteeUuid);
        return find(home, invitee);
    }

    public static List<InviteRow> findOrPublic(UUID ownerUuid, String homeName, UUID inviteeUuid) {
        HomeRow home = HomeRow.find(ownerUuid, homeName);
        if (home == null) return Collections.<InviteRow>emptyList();
        PlayerRow invitee = PlayerRow.findOrCreate(inviteeUuid);
        return findOrPublic(home, invitee);
    }

    public static List<InviteRow> findWithOwnerAndInviteeOrPublic(UUID ownerUuid, UUID inviteeUuid) {
        List<HomeRow> homes = HomeRow.findAll(ownerUuid);
        if (homes.isEmpty()) return Collections.<InviteRow>emptyList();
        PlayerRow invitee = PlayerRow.findOrCreate(inviteeUuid);
        return DB.get().find(InviteRow.class).where().in("home", homes).or(DB.fact().isNull("invitee"), DB.fact().eq("invitee", invitee)).findList();
    }

    public static InviteRow findPublic(UUID ownerUuid, String homeName) {
        HomeRow home = HomeRow.find(ownerUuid, homeName);
        if (home == null) return null;
        return findPublic(home);
    }

    public static List<InviteRow> findAllForInviteeOrPublic(@NonNull UUID inviteeUuid) {
        PlayerRow invitee = PlayerRow.findOrCreate(inviteeUuid);
        return DB.get().find(InviteRow.class).where().or(DB.fact().isNull("invitee"), DB.fact().eq("invitee", invitee)).findList();
    }

    public static boolean isInvited(UUID ownerUuid, String homeName, UUID inviteeUuid) {
        HomeRow home = HomeRow.find(ownerUuid, homeName);
        if (home == null) return false;
        PlayerRow invitee = PlayerRow.findOrCreate(inviteeUuid);
        return DB.get().find(InviteRow.class).where().eq("home", home).or(DB.fact().isNull("invitee"), DB.fact().eq("invitee", invitee)).findRowCount() > 0;
    }

    static InviteRow create(HomeRow home, PlayerRow invitee) {
        InviteRow result = new InviteRow();
        result.setHome(home);
        result.setInvitee(invitee);
        result.setDateCreated(new Date());
        result.save();
        return result;
    }

    public static InviteRow create(UUID ownerUuid, String homeName, UUID inviteeUuid) {
        HomeRow home = HomeRow.find(ownerUuid, homeName);
        if (home == null) return null;
        PlayerRow invitee = PlayerRow.findOrCreate(inviteeUuid);
        InviteRow result = create(home, invitee);
        result.save();
        return result;
    }

    public static InviteRow createPublic(UUID ownerUuid, String homeName) {
        HomeRow home = HomeRow.find(ownerUuid, homeName);
        if (home == null) return null;
        InviteRow result = create(home, null);
        result.save();
        return result;
    }

    public boolean isIgnoredBy(@NonNull UUID uuid) {
        if (!isPublic()) return false; // only public invites can be ignored
        PlayerRow player = PlayerRow.find(uuid);
        if (player == null) return false;
        return null != IgnoreInviteRow.find(player, this);
    }

    public boolean setIgnoredBy(@NonNull UUID player) {
        if (!isPublic()) return false; // only public homes can be ignored
        if (isIgnoredBy(player)) return false;
        IgnoreInviteRow ignore = IgnoreInviteRow.create(PlayerRow.findOrCreate(player), this);
        ignore.save();
        return true;
    }

    public void unignoreAll() {
        List<IgnoreInviteRow> ignores = IgnoreInviteRow.findAll(this);
        if (ignores.isEmpty()) return;
        DB.get().delete(ignores);
    }

    public void save() {
        DB.get().save(this);
    }

    public void delete() {
        DB.get().delete(this);
    }
}
