package com.winthier.home.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import com.winthier.home.Homes;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Player owns Home owns Invite owns IgnoreInvite.
 *
 * This table allows players to ignore public invites. They can
 * delete invites targeted at them themselves if they bother them,
 * but for public invites, they may ignore them for the duration
 * of their existence.
 */
@Entity
@Table(
    name = "ignore_invites",
    uniqueConstraints = @UniqueConstraint(columnNames={"player_id", "invite_id"})
    )
@Getter
@Setter
public class IgnoreInviteRow {
    @Id
    private Integer id;

    @NotNull
    @ManyToOne
    private PlayerRow player;

    @NotNull
    @ManyToOne
    private InviteRow invite;

    @NotNull
    private Date dateCreated;

    @Version
    private Integer version;

    public static IgnoreInviteRow create(PlayerRow player, InviteRow invite) {
        IgnoreInviteRow result = new IgnoreInviteRow();
        result.setPlayer(player);
        result.setInvite(invite);
        result.setDateCreated(new Date());
        return result;
    }

    public static IgnoreInviteRow forId(int id) {
        return Homes.getInstance().getDatabase().find(IgnoreInviteRow.class).where().idEq(id).findUnique();
    }
}
