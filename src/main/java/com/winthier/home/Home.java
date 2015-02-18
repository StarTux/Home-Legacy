package com.winthier.home;

import com.winthier.home.sql.HomeRow;
import java.util.UUID;
import lombok.Data;

/**
 * A container for home information
 */
@Data
public abstract class Home {
    private final UUID owner;
    private final String name;

    protected Home(UUID owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    protected Home(HomeRow row) {
        this(row.getOwner().getUuid(), row.getName());
    }

    public abstract boolean teleport(UUID player);

    public boolean isNamed() {
        return name != null;
    }
}
