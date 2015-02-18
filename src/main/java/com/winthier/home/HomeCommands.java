package com.winthier.home;

import com.winthier.home.sql.HomeRow;
import com.winthier.home.util.Strings;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * This file only exists so Homes.java doesn't get too cluttered.
 */
@RequiredArgsConstructor
public class HomeCommands {
    private final Homes homes;
    public boolean home(UUID sender, String[] args) {
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
                UUID ownerUuid = Homes.getInstance().getPlayerUuid(ownerName);
                if (ownerUuid == null) {
                    Message.forKeyAndRecipient(Message.Key.PLAYER_NOT_FOUND, sender).replace("%playername%", ownerName).send();
                    return true;
                }
                if (ownerUuid.equals(sender)) return false;
                homes.getActions().visitHome(sender, ownerUuid, homeName);
            }
        } else {
            return false;
        }
        return true;
    }

    public boolean setHome(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        if (args.length == 0) {
            homes.getActions().setHome(sender, null);
        } else if (args.length == 1) {
            String homeName = args[0];
            if (homeName.isEmpty()) return false; // Should never happen, but better make sure.
            if (homeName.length() > HomeRow.MAX_HOME_NAME_LENGTH) {
                Message.forKeyAndRecipient(Message.Key.HOME_NAME_TOO_LONG, sender).replace("home", homeName).replace("maxlength", HomeRow.MAX_HOME_NAME_LENGTH).send();
                return true;
            }
            if (!Strings.isValidHomeName(homeName)) {
                Message.forKeyAndRecipient(Message.Key.INVALID_HOME_NAME, sender).replace("home", homeName).send();
                return true;
            }
            homes.getActions().setHome(sender, homeName);
        } else {
            return false;
        }
        return true;
    }

    public boolean listHomes(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        if (args.length > 0) return false;
        homes.getActions().listHomes(sender);
        return true;
    }

    public boolean deleteHome(UUID sender, String[] args) {
        if (args.length == 0) {
            Message.forKeyAndRecipient(Message.Key.CANNOT_DELETE_DEFAULT_HOME, sender).send();
        } else if (args.length == 1) {
            String homeName = args[0];
            if (!Strings.isValidHomeName(homeName)) {
                Message.forKeyAndRecipient(Message.Key.INVALID_HOME_NAME, sender).replace("home", homeName).send();
                return true;
            }
            homes.getActions().deleteHome(sender, homeName);
        } else {
            return false;
        }
        return true;
    }

    public boolean inviteHome(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        if (args.length != 1 && args.length != 2) return false;
        String inviteeName = args[0];
        UUID inviteeUuid = null;
        if (inviteeName.equals("*")) {
            // do nothing
        } else {
            inviteeUuid = homes.getPlayerUuid(inviteeName);
            if (inviteeUuid == null) {
                Message.forKeyAndRecipient(Message.Key.PLAYER_NOT_FOUND, sender).replace("%playername%", inviteeName).send();
                return true;
            }
        }
        String homeName = null;
        if (args.length == 2) homeName = args[1];
        homes.getActions().inviteHome(sender, inviteeUuid, homeName);
        return true;
    }

    public boolean uninviteHome(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        if (args.length != 1 && args.length != 2) return false;
        String inviteeName = args[0];
        UUID inviteeUuid = null;
        if (inviteeName.equals("*")) {
            // do nothing
        } else {
            inviteeUuid = homes.getPlayerUuid(inviteeName);
            if (inviteeUuid == null) {
                Message.forKeyAndRecipient(Message.Key.PLAYER_NOT_FOUND, sender).replace("%playername%", inviteeName).send();
                return true;
            }
        }
        String homeName = null;
        if (args.length == 2) homeName = args[1];
        homes.getActions().uninviteHome(sender, inviteeUuid, homeName);
        return true;
    }

    public boolean listInvites(UUID sender, String[] args) {
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
                ownerUuid = homes.getPlayerUuid(arg);
                if (ownerUuid == null) {
                    Message.forKeyAndRecipient(Message.Key.PLAYER_NOT_FOUND, sender).replace("%playername%", arg).send();
                    return true;
                }
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

    public boolean deleteInvite(UUID sender, String[] args) {
        if (noPlayer(sender)) return true;
        if (args.length != 1) return false;
        String[] tokens = args[0].split(":");
        if (tokens.length < 1 || tokens.length > 2) return false;
        // Get owner
        String ownerName = tokens[0];
        UUID ownerUuid = homes.getPlayerUuid(ownerName);
        if (ownerUuid == null) {
            Message.forKeyAndRecipient(Message.Key.PLAYER_NOT_FOUND, sender).replace("%playername", ownerName).send();
            return true;
        }
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

    public boolean buyHome(UUID sender, String[] args) {
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
