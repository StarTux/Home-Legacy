package com.winthier.home.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.val;

public class Conf {
    /**
     * This deep copy algorithm assumes that all objects are
     * immutable.
     */
    public static Object deepCopy(Object raw) {
        if (raw instanceof List) {
            @SuppressWarnings("unchecked")
                List list = (List<Object>)raw;
            List<Object> result = new ArrayList<>(list.size());
            for (Object obj : list) {
                result.add(deepCopy(obj));
            }
            return result;
        } else if (raw instanceof Map) {
            @SuppressWarnings("unchecked")
                Map<?, ?> map = (Map<?, ?>)raw;
            Map<Object, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(entry.getKey(), deepCopy(entry.getValue()));
            }
            return result;
        } else {
            return raw;
        }
    }

    /**
     * This replace algorithm assumes that all objects are
     * immutable.
     */
    public static Object replace(Object raw, Map<String, Object> replacements) {
        if (replacements.isEmpty()) return raw;
        if (raw instanceof List) {
            @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>)raw;
            for (int i = 0; i < list.size(); ++i) {
                final Object a = list.get(i);
                final Object b = replace(a, replacements);
                if (a != b) list.set(i, b);
            }
            return raw;
        } else if (raw instanceof Map) {
            @SuppressWarnings("unchecked")
                final val map = (Map<?, Object>)raw;
            for (Map.Entry<?, Object> entry : map.entrySet()) {
                final Object a = entry.getValue();
                final Object b = replace(a, replacements);
                entry.setValue(b);
            }
            return raw;
        } else if (raw instanceof String) {
            String string = (String)raw;
            for (Map.Entry<String, Object> entry : replacements.entrySet()) {
                string = string.replace(entry.getKey(), entry.getValue().toString());
            }
            return string;
        }
        return raw;
    }

    public static boolean replaceList(Object raw, String key, List<?> replacement) {
        if (raw == null) return false;
        if (raw instanceof List) {
            @SuppressWarnings("unchecked")
                final val list = (List<Object>)raw;
            if (list.size() == 1 && list.contains(key)) {
                list.clear();
                if (replacement.isEmpty()) {
                    list.add("");
                } else {
                    list.addAll(replacement);
                }
                return true;
            }
            for (Object o : list) {
                if (replaceList(o, key, replacement)) return true;
            }
        } else if (raw instanceof Map) {
            @SuppressWarnings("unchecked")
                final val map = (Map<?, ?>)raw;
            for (Object o : map.values()) {
                if (replaceList(o, key, replacement)) return true;
            }
        } else {
            return false;
        }
        return false;
    }

    /**
     * Find a path in a map containing other maps or values.
     */
    public static Object findPath(Map<?, ?> map, String path) {
        Object result = null;
        for (String token : path.split("\\.")) {
            // path ended too soon
            if (map == null) return null;
            Object o = map.get(token);
            if (o == null) {
                // path not found
                return null;
            } else if (o instanceof Map) {
                // path continues
                @SuppressWarnings("unchecked")
                    final val tmp = (Map<?, ?>)o;
                map = tmp;
            } else {
                // path ends
                map = null;
                result = o;
            }
        }
        return result;
    }

    /**
     * Find an integer in a configuration map tree or return
     * default value if it fails.
     */
    public static int getInt(Map<?, ?> map, String path, int def) {
        Object o = findPath(map, path);
        if (o == null) return def;
        if (o instanceof Integer) return (Integer)o;
        if (o instanceof Number) return ((Number)o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException nfe) {}
        return def;
    }

    /**
     * Find a double in a configuration map tree or return default
     * value if it fails.
     */
    public static double getDouble(Map<?, ?> map, String path, double def) {
        return toDouble(findPath(map, path), def);
    }

    public static Map<?, ?> getMap(Map<?, ?> map, String path) {
        Object o = findPath(map, path);
        if (o == null) return null;
        if (o instanceof Map) {
            @SuppressWarnings("unchecked")
                final val result = (Map<?, ?>)o;
            return result;
        }
        return null;
    }

    public static List<?> getList(Map<?, ?> map, String path) {
        Object o = findPath(map, path);
        if (o == null) return null;
        if (o instanceof List) {
            @SuppressWarnings("unchecked")
                final val result = (List<?>)o;
            return result;
        }
        return null;
    }

    public static double toDouble(Object o, double def) {
        if (o == null) return def;
        if (o instanceof Double) return (Double)o;
        if (o instanceof Number) return ((Number)o).doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException nfe) {}
        return def;
    }
}    
