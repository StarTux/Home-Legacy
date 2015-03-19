package com.winthier.home.util;

import com.winthier.home.Homes;
import java.util.UUID;
import lombok.NonNull;

public class Players {
    public static String getName(@NonNull UUID uuid) {
        String result = Homes.getInstance().getPlayerName(uuid);
        if (result == null) return "Player";
        return result;
    }

    public static UUID getUuid(@NonNull String name) {
        return Homes.getInstance().getPlayerUuid(name);
    }
}
