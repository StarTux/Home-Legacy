package com.winthier.home;

import com.winthier.home.util.Conf;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.yaml.snakeyaml.Yaml;

/**
 * This class is meant to configure player usage per rank. The
 * configuration is in the file "ranks.yml". The rank "default"
 * serves as the default configuration which all values are copied
 * from unless they are overriden in concrete ranks.
 */
@Getter
@ToString
public class Rank {
    private static List<Rank> store;
    private final String name;
    private final int maxHomes;
    private final double buyHomeCostBase;
    private final double buyHomeCostGrowth;
    private final double defaultSetHomeCost;
    private final double defaultGoHomeCost;
    private final double defaultVisitHomeCost;
    private final double defaultVisitHomeReward;
    private final double namedSetHomeCost;
    private final double namedGoHomeCost;
    private final double namedVisitHomeCost;
    private final double namedVisitHomeReward;

    private Rank() {
        name = "default";
        maxHomes = 0;
        buyHomeCostBase = buyHomeCostGrowth = -1.0;
        defaultSetHomeCost = defaultGoHomeCost = defaultVisitHomeCost = defaultVisitHomeReward = namedSetHomeCost = namedGoHomeCost = namedVisitHomeCost = namedVisitHomeReward = 0.0;
    }
    
    private Rank(String name, Map<?, ?> map, Rank def) {
        this.name = name;
        maxHomes = Conf.getInt(map, "MaxHomes", def.maxHomes);
        buyHomeCostBase = Conf.getDouble(map, "Economy.BuyHomeCostBase", def.buyHomeCostBase);
        buyHomeCostGrowth = Conf.getDouble(map, "Economy.BuyHomeCostGrowth", def.buyHomeCostGrowth);
        defaultSetHomeCost = Conf.getDouble(map, "Economy.Default.SetHomeCost", def.defaultSetHomeCost);
        defaultGoHomeCost = Conf.getDouble(map, "Economy.Default.GoHomeCost", def.defaultGoHomeCost);
        defaultVisitHomeCost = Conf.getDouble(map, "Economy.Default.VisitHomeCost", def.defaultVisitHomeCost);
        defaultVisitHomeReward = Conf.getDouble(map, "Economy.Default.VisitHomeReward", def.defaultVisitHomeReward);
        namedSetHomeCost = Conf.getDouble(map, "Economy.Named.SetHomeCost", def.namedSetHomeCost);
        namedGoHomeCost = Conf.getDouble(map, "Economy.Named.GoHomeCost", def.namedGoHomeCost);
        namedVisitHomeCost = Conf.getDouble(map, "Economy.Named.VisitHomeCost", def.namedVisitHomeCost);
        namedVisitHomeReward = Conf.getDouble(map, "Economy.Named.VisitHomeReward", def.namedVisitHomeReward);
    }

    public static void clearStore() {
        store = null;
    }

    private static void loadStore() {
        store = new ArrayList<Rank>();
        Yaml yaml = new Yaml();
        InputStream in = Homes.getInstance().getResource("ranks.yml");
        @SuppressWarnings("unchecked")
            Map<?, ?> map = (Map<?, ?>)yaml.load(in);
        Rank def = new Rank();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            final String name = entry.getKey().toString();
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                    final val section = (Map<?, ?>)entry.getValue();
                Rank rank = new Rank(name, section, def);
                store.add(rank);
                def = rank;
            }
        }
    }

    public static Rank forPlayer(UUID uuid) {
        if (store == null) loadStore();
        for (int i = store.size() - 1; i >= 0; --i) {
            final Rank rank = store.get(i);
            if (Homes.getInstance().playerHasRank(uuid, rank.getName())) {
//                System.out.println("Rank for " + Homes.getInstance().getPlayerName(uuid) + ": " + rank.getName());
                return rank;
            }
        }
        return store.get(0);
    }

    public static List<Rank> allRanks() {
        if (store == null) loadStore();
        return new ArrayList<Rank>(store);
    }
}
