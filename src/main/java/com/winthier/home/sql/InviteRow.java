package com.winthier.home.sql;

import com.avaje.ebean.ExpressionFactory;
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
import lombok.Setter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Player owns Home owns Invite owns IgnoreInvite.
 *
 * An invite owns it ignores, so when an invite gets deleted, all
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
        return Homes.getInstance().getDatabase().find(InviteRow.class).where().idEq(id).findUnique();
    }

    public static List<InviteRow> forIds(List<Integer> ids) {
        return Homes.getInstance().getDatabase().find(InviteRow.class).where().idIn(ids).findList();
    }
    
    public static InviteRow find(HomeRow home, PlayerRow invitee) {
        final val list = Homes.getInstance().getDatabase().find(InviteRow.class).where().eq("home", home).eq("invitee", invitee).findList();
        return Util.unique(list);
    }

    public static List<InviteRow> find(PlayerRow invitee) {
        ExpressionFactory expr = Homes.getInstance().getDatabase().getExpressionFactory();
        return Homes.getInstance().getDatabase().find(InviteRow.class).where().or(expr.isNull("invitee"), expr.eq("invitee", invitee)).findList();
    }

    public static List<InviteRow> find(UUID inviteeUuid) {
        PlayerRow invitee = PlayerRow.find(inviteeUuid);
        if (invitee == null) return Collections.<InviteRow>emptyList();
        return find(invitee);
    }

    public static List<InviteRow> find(List<HomeRow> homes, PlayerRow invitee) {
        ExpressionFactory expr = Homes.getInstance().getDatabase().getExpressionFactory();
        return Homes.getInstance().getDatabase().find(InviteRow.class).where().in("home", homes).or(expr.isNull("invitee"), expr.eq("invitee", invitee)).findList();
    }

    public static List<InviteRow> findWithOwnerAndInvitee(UUID ownerUuid, UUID inviteeUuid) {
        List<HomeRow> homes = HomeRow.find(ownerUuid);
        if (homes.isEmpty()) return Collections.<InviteRow>emptyList();
        PlayerRow invitee = PlayerRow.find(inviteeUuid);
        return find(homes, invitee);
    }

    public static InviteRow create(HomeRow home, PlayerRow invitee) {
        InviteRow result = new InviteRow();
        result.setHome(home);
        result.setInvitee(invitee);
        result.setDateCreated(new Date());
        return result;
    }

    public static InviteRow create(HomeRow home, UUID inviteeUuid) {
        PlayerRow invitee = null;
        if (inviteeUuid != null) invitee = PlayerRow.findOrCreate(inviteeUuid);
        return create(home, invitee);
    }

    public boolean isIgnoredBy(UUID player) {
        if (!isPublic()) return false; // only public invites can be ignored
        List<IgnoreInviteRow> ignores = getIgnores();
        if (ignores == null) return false;
        for (IgnoreInviteRow ignore : ignores) {
            if (ignore.getPlayer().getUuid().equals(player)) return true;
        }
        return false;
    }

    public boolean setIgnoredBy(PlayerRow player) {
        if (!isPublic()) return false; // only public homes can be ignored
        if (isIgnoredBy(player.getUuid())) return false;
        IgnoreInviteRow ignore = IgnoreInviteRow.create(player, this);
        Homes.getInstance().getDatabase().save(ignore);
        return true;
    }

    public boolean setIgnoredBy(UUID playerUuid) {
        PlayerRow player = PlayerRow.find(playerUuid);
        if (player == null) return false;
        return setIgnoredBy(player);
    }
}
