package com.winthier.home.util;

public class Strings {
    private final static String validHomeNameRange = "-A-Za-z0-9!@#$%^&*()_+<>{}'?/\\\\.";
    
    public static String camelCase(String string) {
        StringBuilder builder = new StringBuilder();
        for (String token : string.split("_")) {
            builder.append(token.substring(0, 1).toUpperCase());
            builder.append(token.substring(1).toLowerCase());
        }
        return builder.toString();
    }

    public static boolean isValidHomeName(String name) {
        return name.matches("^[" + validHomeNameRange + "]+$");
    }

    public static String fixInvalidHomeName(String name) {
        if (name.isEmpty()) return name;
        final String result = name.replaceAll("[^" + validHomeNameRange + "]", "");
        if (result.isEmpty()) return "_";
        return result;
    }
}
