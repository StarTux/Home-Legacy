package com.winthier.home.sql;

import java.util.List;

class Util {
    static <T> T unique(List<T> list) {
        if (list.isEmpty()) return null;
        if (list.size() > 1) {
            System.err.println("Unique query of returned more than one result!");
            Thread.dumpStack();
        }
        return list.get(0);
    }
}
