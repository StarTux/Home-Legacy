package com.winthier.home;

import java.util.UUID;

public enum Permission {
    HOME_ADMIN,
    HOME_ADMIN_GRANT,
    HOME_ADMIN_RELOAD,
    HOME_ADMIN_WORLDBLACKLIST,
    HOME_ADMIN_CONSISTENCY,
    HOME_ADMIN_IMPORT,
    HOME_OVERRIDE,
    HOME_OVERRIDE_EDIT,

    HOME_HOMES,
    HOME_HOME,
    HOME_SETHOME,
    HOME_LISTHOMES,
    HOME_DELETEHOME,
    HOME_INVITEHOME,
    HOME_UNINVITEHOME,
    HOME_LISTINVITES,
    HOME_LISTMYINVITES,
    HOME_DELETEINVITE,
    HOME_BUYHOME,
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
