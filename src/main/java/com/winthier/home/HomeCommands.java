package com.winthier.home;

import com.winthier.home.sql.HomeRow;
import com.winthier.home.util.Players;
import com.winthier.home.util.Strings;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This file only exists so Homes.java doesn't get too cluttered.
 */
@RequiredArgsConstructor
public class HomeCommands {
    public static enum Type {
        HOMES("Homes"),
        HOME("Home"),
        SETHOME("SetHome"),
        LISTHOMES("ListHomes"),
        DELETEHOME("DeleteHome"),
        INVITEHOME("InviteHome"),
        UNINVITEHOME("UnInviteHome"),
        LISTINVITES("ListInvites"),
        LISTMYINVITES("ListMyInvites"),
        DELETEINVITE("DeleteInvite"),
        BUYHOME("BuyHome"),
        HOMEADMIN("HomeAdmin"),
        ;
        final String camel;
        Type(String camel) {
            this.camel = camel;
        }
        public static Type forString(String input) {
            try {
                return valueOf(input.toUpperCase());
            } catch (IllegalArgumentException iae) {
                iae.printStackTrace();
            }
            return null;
        }
    }

    private final Homes homes;

    public boolean onCommand(@NonNull Type type, UUID sender, @NonNull String[] args) {
        try {
            switch (type) {
            case HOMES: return homes(sender, args);
            case HOME: return home(sender, args);
            case SETHOME: return setHome(sender, args);
            case LISTHOMES: return listHomes(sender, args);
            case DELETEHOME: return deleteHome(sender, args);
            case INVITEHOME: return inviteHome(sender, args);
            case UNINVITEHOME: return uninviteHome(sender, args);
            case LISTINVITES: return listInvites(sender, args);
            case LISTMYINVITES: return listMyInvites(sender, args);
            case DELETEINVITE: return deleteInvite(sender, args);
            case BUYHOME: return buyHome(sender, args);
            case HOMEADMIN: return homes.getAdminCommands().command(sender, args);
            }
        } catch (HomeCommandException hce) {
            hce.getHomeMessage().send();
            return true;
        }
        System.err.println("Command not implemented: " + type.camel);
        return false;
    }

    boolean homes(UUID sender, String[] args) {
        Message.Key.HOMES_MENU_TITLE.make(sender).send();
        if (Permission.HOME_LISTHOMES.has(sender)) Message.Key.HOMES_MENU_LISTHOMES.make(sender).send();
        if (Permission.HOME_SETHOME.has(sender)) Message.Key.HOMES_MENU_SETHOME.make(sender).send();
        if (Permission.HOME_HOME.has(sender)) Message.Key.HOMES_MENU_HOME.make(sender).send();
        if (Permission.HOME_LISTINVITES.has(sender)) Message.Key.HOMES_MENU_LISTINVITES.make(sender).send();
        if (Permission.HOME_LISTMYINVITES.has(sender)) Message.Key.HOMES_MENU_LISTMYINVITES.make(sender).send();
        if (Permission.HOME_INVITEHOME.has(sender)) Message.Key.HOMES_MENU_INVITEHOME.make(sender).send();
        if (Permission.HOME_BUYHOME.has(sender)) Message.Key.HOMES_MENU_BUYHOME.make(sender).send();
        return true;
    }
    
    boolean home(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        if (args.length == 0) {
            // home
            homes.getActions().goHome(sender, null);
        } else if (args.length == 1) {
            // home ...
            if (!args[0].contains(":")) {
                homes.getActions().goHome(sender, args[0]);
            } else {
                // home player:name
                String[] tokens = args[0].split(":");
                if (tokens.length < 1 || tokens.length > 2) return false;
                String ownerName = tokens[0];
                if (ownerName.isEmpty()) return false;
                String homeName = null;
                if (tokens.length >= 2) homeName = tokens[1];
                if (homeName != null && homeName.isEmpty()) homeName = null;
                UUID ownerUuid = Players.getUuid(ownerName);
                if (ownerUuid == null) Message.Key.PLAYER_NOT_FOUND.make(sender).replace("%playername%", ownerName).raise();
                if (ownerUuid.equals(sender)) return false;
                homes.getActions().visitHome(sender, ownerUuid, homeName);
            }
        } else {
            return false;
        }
        return true;
    }

    boolean setHome(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        if (args.length == 0) {
            homes.getActions().setHome(sender, null);
        } else if (args.length == 1) {
            String homeName = args[0];
            if (homeName.contains(":")) {
                if (!Permission.HOME_OVERRIDE_EDIT.has(sender)) return false;
                String[] tokens = homeName.split(":");
                if (tokens.length > 2) return false;
                String ownerName = tokens.length < 1 ? null : tokens[0];
                if (ownerName == null) return false;
                homeName = tokens.length < 2 ? null : tokens[1];
                if (homeName != null && homeName.isEmpty()) homeName = null;
                UUID ownerUuid = Players.getUuid(ownerName);
                if (ownerUuid == null) Message.Key.PLAYER_NOT_FOUND.make(sender).replace("%playername%", ownerName).raise();
                homes.getActions().setHomeOverride(sender, ownerUuid, homeName);
                return true;
            }
            if (homeName.isEmpty()) return false; // Should never happen, but better make sure.
            if (homeName.length() > HomeRow.MAX_HOME_NAME_LENGTH) Message.Key.HOME_NAME_TOO_LONG.make(sender).replace("home", homeName).replace("maxlength", HomeRow.MAX_HOME_NAME_LENGTH).raise();
            if (!Strings.isValidHomeName(homeName)) Message.Key.INVALID_HOME_NAME.make(sender).replace("home", homeName).raise();
            homes.getActions().setHome(sender, homeName);
        } else {
            return false;
        }
        return true;
    }

    boolean listHomes(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        if (args.length == 1) {
            if (!Permission.HOME_OVERRIDE.has(sender)) return false;
            String ownerName = args[0];
            UUID ownerUuid = Players.getUuid(ownerName);
            if (ownerUuid == null) Message.Key.PLAYER_NOT_FOUND.make(sender).replace("%playername%", ownerName).raise();
            homes.getActions().listHomesOverride(sender, ownerUuid);
            return true;
        } else if (args.length > 0) {
            return false;
        }
        homes.getActions().listHomes(sender);
        return true;
    }

    boolean deleteHome(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        if (args.length == 0) {
            Message.Key.CANNOT_DELETE_DEFAULT_HOME.make(sender).raise();
        } else if (args.length == 1) {
            String homeName = args[0];
            if (homeName.contains(":")) {
                if (!Permission.HOME_OVERRIDE_EDIT.has(sender)) return false;
                String[] tokens = homeName.split(":");
                if (tokens.length > 2) return false;
                String ownerName = tokens.length < 1 ? null : tokens[0];
                if (ownerName == null) return false;
                homeName = tokens.length < 2 ? null : tokens[1];
                if (homeName != null && homeName.isEmpty()) homeName = null;
                UUID ownerUuid = Players.getUuid(ownerName);
                if (ownerUuid == null) Message.Key.PLAYER_NOT_FOUND.make(sender).replace("%playername%", ownerName).raise();
                homes.getActions().deleteHomeOverride(sender, ownerUuid, homeName);
                return true;
            }
            if (!Strings.isValidHomeName(homeName)) Message.Key.INVALID_HOME_NAME.make(sender).replace("home", homeName).raise();
            homes.getActions().deleteHome(sender, homeName);
        } else {
            return false;
        }
        return true;
    }

    boolean inviteHome(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        if (args.length != 1 && args.length != 2) return false;
        String inviteeName = args[0];
        UUID inviteeUuid = null;
        if (inviteeName.equals("*")) {
            // do nothing
        } else {
            inviteeUuid = Players.getUuid(inviteeName);
            if (inviteeUuid == null) Message.Key.PLAYER_NOT_FOUND.make(sender).replace("%playername%", inviteeName).raise();
        }
        String homeName = null;
        if (args.length == 2) homeName = args[1];
        homes.getActions().inviteHome(sender, inviteeUuid, homeName);
        return true;
    }

    boolean uninviteHome(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        if (args.length != 1 && args.length != 2) return false;
        String inviteeName = args[0];
        UUID inviteeUuid = null;
        if (inviteeName.equals("*")) {
            // do nothing
        } else {
            inviteeUuid = Players.getUuid(inviteeName);
            if (inviteeUuid == null) Message.Key.PLAYER_NOT_FOUND.make(sender).replace("%playername%", inviteeName).raise();
        }
        String homeName = null;
        if (args.length == 2) homeName = args[1];
        homes.getActions().uninviteHome(sender, inviteeUuid, homeName);
        return true;
    }

    boolean listInvites(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        UUID ownerUuid = null;
        boolean edit = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--edit") ||
                arg.equalsIgnoreCase("-e")) {
                if (edit) return false; // guard against multiple flags
                edit = true;
            } else {
                if (ownerUuid != null) return false; // only one player name allowed
                ownerUuid = Players.getUuid(arg);
                if (ownerUuid == null) Message.Key.PLAYER_NOT_FOUND.make(sender).replace("%playername%", arg).raise();
                if (ownerUuid.equals(sender)) return false;
            }
        }
        if (ownerUuid == null) {
            homes.getActions().listInviters(sender, edit);
        } else {
            homes.getActions().listInvites(sender, ownerUuid, edit);
        }
        return true;
    }

    boolean listMyInvites(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        boolean edit = false;
        if (args.length == 0) {
            // nothing
        } else if (args.length == 1 && (args[0].equals("-e") || args[0].equals("--edit"))) {
            edit = true;
        } else {
            return false;
        }
        homes.getActions().listMyInvites(sender, edit);
        return true;
    }

    boolean deleteInvite(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        if (args.length != 1) return false;
        String[] tokens = args[0].split(":");
        if (tokens.length < 1 || tokens.length > 2) return false;
        // Get owner
        String ownerName = tokens[0];
        UUID ownerUuid = Players.getUuid(ownerName);
        if (ownerUuid == null) Message.Key.PLAYER_NOT_FOUND.make(sender).replace("%playername", ownerName).raise();
        if (ownerUuid.equals(sender)) return false;
        // Figure out home name
        String homeName = null;
        if (tokens.length >= 2) homeName = tokens[1];
        if (homeName != null) {
            if (homeName.isEmpty()) homeName = null;
            else if (homeName.equals("*")) {
                // deleteinvite player:*
                homes.getActions().deleteInvites(sender, ownerUuid);
                return true;
            }
        }
        // deletehome player:name
        homes.getActions().deleteInvite(sender, ownerUuid, homeName);
        return true;
    }

    boolean buyHome(UUID sender, String[] args) {
        if (args.length == 0) {
            homes.getActions().buyHome(sender);
        } else if (args.length == 1) {
            double price = -1.0;
            try {
                price = Double.parseDouble(args[0]);
            } catch (NumberFormatException nfe) {
                // Do nothing
            }
            homes.getActions().buyHome(sender, price);
        } else {
            return false;
        }
        return true;
    }

    private boolean noPlayer(UUID uuid) {
        if (uuid != null) return false;
        System.err.println("Player expected");
        return true;
    }
}
