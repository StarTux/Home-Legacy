package com.winthier.home.util;

import java.util.UUID;
import com.winthier.home.Homes;

public class Players {
    public static String getName(UUID uuid) {
        String result = Homes.getInstance().getPlayerName(uuid);
        if (result == null) return "Player";
        return result;
    }

    public static UUID getUuid(String name) {
        return Homes.getInstance().getPlayerUuid(name);
    }
}
