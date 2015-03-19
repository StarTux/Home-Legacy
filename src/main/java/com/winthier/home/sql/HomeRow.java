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
import lombok.NonNull;
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
        WorldRow world = WorldRow.findOrCreate(worldName);
        setWorld(world);
        setX(x);
        setY(y);
        setZ(z);
        setYaw(yaw);
        setPitch(pitch);
    }

    public static HomeRow forId(int id) {
        return DB.get().find(HomeRow.class).where().idEq(id).findUnique();
    }

    static HomeRow find(PlayerRow owner, String name) {
        return DB.unique(DB.get().find(HomeRow.class).where().eq("owner", owner).eq("name", name).findList());
    }

    static List<HomeRow> findAll(PlayerRow owner) {
        return DB.get().find(HomeRow.class).where().eq("owner", owner).findList();
    }

    public static HomeRow find(UUID owner, String name) {
        PlayerRow player = PlayerRow.find(owner);
        if (player == null) return null;
        return find(player, name);
    }

    public static List<HomeRow> findAll(UUID owner) {
        PlayerRow player = PlayerRow.find(owner);
        if (player == null) return Collections.<HomeRow>emptyList();
        return findAll(player);
    }

    public static int count(UUID owner) {
        PlayerRow player = PlayerRow.find(owner);
        if (player == null) return 0;
        return DB.get().find(HomeRow.class).where().eq("owner", player).findRowCount();
    }

    /**
     * You have to set location on the result of this.
     */
    public static HomeRow create(@NonNull UUID ownerUuid, String name) {
        PlayerRow owner = PlayerRow.findOrCreate(ownerUuid);
        HomeRow result = new HomeRow();
        result.setOwner(owner);
        result.setName(name);
        result.setDateCreated(new Date());
        return result;
    }

    public static HomeRow create(@NonNull UUID ownerUuid, String name, @NonNull String worldName, double x, double y, double z, float yaw, float pitch) {
        HomeRow result = new HomeRow();
        result.setOwner(PlayerRow.findOrCreate(ownerUuid));
        result.setName(name);
        result.setWorld(WorldRow.findOrCreate(worldName));
        result.setX(x);
        result.setY(y);
        result.setZ(z);
        result.setYaw(yaw);
        result.setPitch(pitch);
        result.setDateCreated(new Date());
        return result;
    }

    public void save() {
        DB.get().save(this);
    }

    public void delete() {
        DB.get().delete(this);
    }
}
