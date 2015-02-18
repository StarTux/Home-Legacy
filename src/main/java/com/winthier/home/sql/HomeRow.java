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
 * Player owns Home owns Invite.
 *
 * A home owns all its invites, so when a home gets deleted, all
 * invites to that home get deleted, too.
 */
@Entity
@Table(
    name = "homes",
    uniqueConstraints = @UniqueConstraint(columnNames={"owner_id", "name"})
    )
@Getter
@Setter
public class HomeRow {
    public static final int MAX_HOME_NAME_LENGTH = 32;
    
    @Id
    private Integer id;

    @NotNull
    @ManyToOne
    private PlayerRow owner;

    @Length(max=MAX_HOME_NAME_LENGTH)
    private String name;

    @NotNull
    @ManyToOne
    private WorldRow world;

    @NotNull
    private double x;

    @NotNull
    private double y;

    @NotNull
    private double z;

    @NotNull
    private float yaw;

    @NotNull
    private float pitch;

    @NotNull
    private Date dateCreated;

    @Version
    private Integer version;

    // Referencing this

    @OneToMany(mappedBy="home", cascade=CascadeType.REMOVE)
    private List<InviteRow> invites;

    public boolean isNamed() {
        return getName() != null;
    }

    public String getNiceName() {
        final String name = getName();
        return name == null ? "" : name;
    }

    public void setLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        WorldRow world = WorldRow.forName(worldName);
        setWorld(world);
        setX(x);
        setY(y);
        setZ(z);
        setYaw(yaw);
        setPitch(pitch);
    }

    public InviteRow getInviteFor(PlayerRow playerRow) {
        final val list = Homes.getInstance().getDatabase().find(InviteRow.class).where().eq("home", this).eq("invitee", playerRow).findList();
        return Util.unique(list);
    }

    public InviteRow getInviteFor(UUID playerUuid) {
        if (playerUuid == null) {
            final val list = Homes.getInstance().getDatabase().find(InviteRow.class).where().isNull("invitee").findList();
            return Util.unique(list);
        }
        PlayerRow player = PlayerRow.find(playerUuid);
        if (player == null) return null;
        return getInviteFor(player);
    }

    public boolean isInvited(PlayerRow playerRow) {
        ExpressionFactory expr = Homes.getInstance().getDatabase().getExpressionFactory();
        return 0 < Homes.getInstance().getDatabase().find(InviteRow.class).where().eq("home", this).or(expr.isNull("invitee"), expr.eq("invitee", playerRow)).findRowCount();
    }

    public boolean isInvited(UUID playerUuid) {
        if (playerUuid == null) return 0 < Homes.getInstance().getDatabase().find(InviteRow.class).where().eq("home", this).isNull("invitee").findRowCount();
        PlayerRow player = PlayerRow.find(playerUuid);
        if (player == null) return false;
        return isInvited(player);
    }

    public static HomeRow forId(int id) {
        return Homes.getInstance().getDatabase().find(HomeRow.class).where().idEq(id).findUnique();
    }

    public static List<HomeRow> forId(List<Integer> ids) {
        return Homes.getInstance().getDatabase().find(HomeRow.class).where().idIn(ids).findList();
    }

    public static HomeRow find(PlayerRow owner, String name) {
        final val list = Homes.getInstance().getDatabase().find(HomeRow.class).where().eq("owner", owner).eq("name", name).findList();
        return Util.unique(list);
    }

    public static HomeRow find(UUID ownerUuid, String name) {
        if (name != null && name.length() == 0) throw new IllegalArgumentException("Name must not be empty");
        PlayerRow owner = PlayerRow.find(ownerUuid);
        return find(owner, name);
    }

    public static List<HomeRow> find(PlayerRow owner) {
        List<HomeRow> result = owner.getHomes();
        if (result == null) return Collections.<HomeRow>emptyList();
        return result;
    }

    public static List<HomeRow> find(UUID ownerUuid) {
        PlayerRow owner = PlayerRow.find(ownerUuid);
        if (owner == null) return Collections.<HomeRow>emptyList();
        return find(owner);
    }

    /**
     * You have to set location on the result of this.
     */
    public static HomeRow create(UUID ownerUuid, String name) {
        PlayerRow owner = PlayerRow.findOrCreate(ownerUuid);
        HomeRow result = new HomeRow();
        result.setOwner(owner);
        result.setName(name);
        result.setDateCreated(new Date());
        return result;
    }

    public static HomeRow create(UUID ownerUuid, String name, String worldName, double x, double y, double z, float yaw, float pitch) {
        HomeRow result = new HomeRow();
        result.setOwner(PlayerRow.findOrCreate(ownerUuid));
        result.setName(name);
        result.setWorld(WorldRow.forName(worldName));
        result.setX(x);
        result.setY(y);
        result.setZ(z);
        result.setYaw(yaw);
        result.setPitch(pitch);
        result.setDateCreated(new Date());
        return result;
    }
}
