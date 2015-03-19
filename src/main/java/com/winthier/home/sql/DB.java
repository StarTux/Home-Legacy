package com.winthier.home.sql;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.ExpressionFactory;
import com.winthier.home.Homes;
import java.util.List;

public class DB {
    static EbeanServer get() {
        return Homes.getInstance().getDatabase();
    }

    static <T> T unique(List<T> list) {
        if (list.isEmpty()) return null;
        if (list.size() > 1) {
            System.err.println("Unique query of returned more than one result!");
            Thread.dumpStack();
        }
        return list.get(0);
    }

    static ExpressionFactory fact() {
        return get().getExpressionFactory();
    }
}
