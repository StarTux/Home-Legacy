package com.winthier.home.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import com.winthier.home.Homes;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@Entity
@Table(
    name = "worlds",
    uniqueConstraints = @UniqueConstraint(columnNames={"name"})
    )
@Getter
@Setter
public class WorldRow {
    public static final int MAX_WORLD_NAME_LENGTH = 32;

    @Id
    private Integer id;

    @NotEmpty
    @Length(max = MAX_WORLD_NAME_LENGTH)
    private String name;

    @Version
    private Integer version;

    public static WorldRow forName(String name) {
        if (name == null) throw new NullPointerException("World name cannot be null");
        if (name.length() == 0) throw new IllegalArgumentException("World name cannot be empty");
        if (name.length() > MAX_WORLD_NAME_LENGTH) throw new IllegalArgumentException("World name cannot be longer than " + MAX_WORLD_NAME_LENGTH);
        final val list = Homes.getInstance().getDatabase().find(WorldRow.class).where().eq("name", name).findList();
        WorldRow row = Util.unique(list);
        if (row == null) {
            row = new WorldRow();
            row.setName(name);
            Homes.getInstance().getDatabase().save(row);
        }
        return row;
    }
}
