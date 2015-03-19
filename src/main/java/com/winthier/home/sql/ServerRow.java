package com.winthier.home.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import com.winthier.home.Homes;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "servers",
    uniqueConstraints = @UniqueConstraint(columnNames={"name"})
    )
@Getter
@Setter
public class ServerRow {
    private static ServerRow thisServer = null;
    public static final int MAX_SERVER_NAME_LENGTH = 32;

    @Id
    private Integer id;

    @NotEmpty
    @Length(max = MAX_SERVER_NAME_LENGTH)
    private String name;

    @Version
    private Integer version;

    public static ServerRow getThisServer() {
        ServerRow result;
        result = thisServer;
        if (result != null) return result;
        result = DB.get().find(ServerRow.class).where().eq("name", Homes.getInstance().getServerName()).findUnique();
        if (result == null) {
            result = new ServerRow();
            result.setName(Homes.getInstance().getServerName());
            DB.get().save(result);
            thisServer = result;
        }
        return result;
    }

    public static void clearCache() {
        thisServer = null;
    }
}
