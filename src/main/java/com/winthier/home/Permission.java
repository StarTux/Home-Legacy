package com.winthier.home;

import java.util.UUID;

public enum Permission {
    HOME_ADMIN,
    HOME_ADMIN_LIST,
    HOME_ADMIN_GOTO,
    HOME_ADMIN_GRANT,
    HOME_ADMIN_RELOAD,
    HOME_ADMIN_CONSISTENCY,
    HOME_ADMIN_IMPORT,
    ;
    public final String key;

    Permission() {
        key = name().toLowerCase().replace("_", ".");
    }

    public boolean has(UUID player) {
        if (player == null) return true;
        return Homes.getInstance().playerHasPermission(player, this);
    }
}
