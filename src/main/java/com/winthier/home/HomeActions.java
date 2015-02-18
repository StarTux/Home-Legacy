package com.winthier.home;

import com.winthier.home.sql.HomeRow;
import com.winthier.home.sql.InviteRow;
import com.winthier.home.sql.PlayerRow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * This class only exists so Homes.java doesn't get too cluttered.
 */
@RequiredArgsConstructor
public class HomeActions {
    private final Homes homes;
    
    public void goHome(UUID playerUuid, String homeName) {
        // Find home
        final Home home = homes.findHome(playerUuid, homeName);
        if (home == null) {
            if (homeName == null) {
                Message.forKeyAndRecipient(Message.Key.DEFAULT_HOME_NOT_FOUND, playerUuid).send();
            } else {
                Message.forKeyAndRecipient(Message.Key.NAMED_HOME_NOT_FOUND, playerUuid).replace("%homename%", homeName).send();
            }
            return;
        }
        // Deduct money
        final Rank rank = Rank.forPlayer(playerUuid);
        final double price = homeName == null ? rank.getDefaultGoHomeCost() : rank.getNamedGoHomeCost();
        if (price > 0.0 && !homes.takeMoney(playerUuid, price)) {
            Message.forKeyAndRecipient(Message.Key.NOT_ENOUGH_MONEY, playerUuid).replace("%price%", homes.formatMoney(price)).send();
            return;
        }
        // Send player
        home.teleport(playerUuid);
        // Send messages
        if (price > 0.0) {
            if (homeName == null) {
                Message.forKeyAndRecipient(Message.Key.TELEPORTED_DEFAULT_HOME_WITH_PRICE, playerUuid).replace("%price%", homes.formatMoney(price)).send();
            } else {
                Message.forKeyAndRecipient(Message.Key.TELEPORTED_NAMED_HOME_WITH_PRICE, playerUuid).replace("%homename%", homeName).replace("%price%", homes.formatMoney(price)).send();
            }
        } else {
            if (homeName == null) {
                Message.forKeyAndRecipient(Message.Key.TELEPORTED_DEFAULT_HOME, playerUuid).send();
            } else {
                Message.forKeyAndRecipient(Message.Key.TELEPORTED_NAMED_HOME, playerUuid).replace("%homename%", homeName).send();
            }
        }
    }

    public void visitHome(UUID playerUuid, UUID ownerUuid, String homeName) {
        // Find home
        String ownerName = homes.getPlayerName(ownerUuid);
        HomeRow homeRow = HomeRow.find(ownerUuid, homeName);
        if (homeRow == null) {
            Message.forKeyAndRecipient(Message.Key.YOU_ARE_NOT_INVITED, playerUuid).replace("%playername%", ownerName).send();
            return;
        }
        homeName = homeRow.getName();
        // Check if invited
        if (!homeRow.isInvited(playerUuid)) {
            Message.forKeyAndRecipient(Message.Key.YOU_ARE_NOT_INVITED, playerUuid).replace("%playername%", ownerName).send();
            return;
        }
        // Deduct money
        Rank rank = Rank.forPlayer(playerUuid);
        double price = homeName == null ? rank.getDefaultVisitHomeCost() : rank.getNamedVisitHomeCost();
        if (price > 0.0 && !homes.takeMoney(playerUuid, price)) {
            Message.forKeyAndRecipient(Message.Key.NOT_ENOUGH_MONEY, playerUuid).replace("%price%", homes.formatMoney(price)).send();
            return;
        }
        // Give reward
        Rank ownerRank = Rank.forPlayer(ownerUuid);
        double reward = homeName == null ? ownerRank.getDefaultVisitHomeReward() : ownerRank.getNamedVisitHomeReward();
        if (reward > 0.0) {
            if (homes.giveMoney(ownerUuid, reward)) {
                String playerName = homes.getPlayerName(playerUuid);
                if (homeName == null) {
                    Message.forKeyAndRecipient(Message.Key.DEFAULT_HOME_REWARDED, ownerUuid).replace("%playername%", playerName).replace("%reward%", homes.formatMoney(reward)).send();
                } else {
                    Message.forKeyAndRecipient(Message.Key.NAMED_HOME_REWARDED, ownerUuid).replace("%playername%", playerName).replace("%reward%", homes.formatMoney(reward)).replace("%homename%", homeName).send();
                }
            } else {
            }
        }
        // Teleport
        homes.homeForRow(homeRow).teleport(playerUuid);
        // Send message
        if (homeName == null) {
            if (price > 0.0) {
                Message.forKeyAndRecipient(Message.Key.VISITED_DEFAULT_HOME_WITH_PRICE, playerUuid).replace("%playername%", ownerName).replace("%price%", homes.formatMoney(price)).send();
            } else {
                Message.forKeyAndRecipient(Message.Key.VISITED_DEFAULT_HOME, playerUuid).replace("%playername%", ownerName).send();
            }
        } else {
            if (price > 0.0) {
                Message.forKeyAndRecipient(Message.Key.VISITED_NAMED_HOME_WITH_PRICE, playerUuid).replace("%playername%", ownerName).replace("%homename%", homeName).replace("%price%", homes.formatMoney(price)).send();
            } else {
                Message.forKeyAndRecipient(Message.Key.VISITED_NAMED_HOME, playerUuid).replace("%playername%", ownerName).replace("%homename%", homeName).send();
            }
        }
    }

    public void setHome(UUID playerUuid, final String homeName) {
        HomeRow homeRow = null; // pivot
        // Fetch some data
        final Rank rank = Rank.forPlayer(playerUuid);
        final PlayerRow playerRow = PlayerRow.findOrCreate(playerUuid);
        List<HomeRow> homeRows = HomeRow.find(playerUuid);
        // Try to find the existing home
        for (HomeRow row : homeRows) {
            if ((homeName == null && row.getName() == null) || (homeName != null && homeName.equals(row.getName()))) {
                homeRow = row;
                break;
            }
        }
        // Make a new home if necessary
        if (homeRow == null) {
            // Check max homes
            int maxHomes = rank.getMaxHomes() + playerRow.getExtraHomes();
            if (homeRows.size() >= maxHomes) {
                // Too many homes
                Message.forKeyAndRecipient(Message.Key.TOO_MANY_HOMES, playerUuid).replace("%maxhomes%", maxHomes).replace("%homecount%", homeRows.size()).send();
                return;
            }
            homeRow = HomeRow.create(playerUuid, homeName);
        }
        // Update home location
        if (!homes.setCurrentLocation(homeRow, playerUuid)) {
            // Should never happen
            throw new RuntimeException("Failed setting current location for player " + playerUuid);
        }
        // Deduct price, if any
        final double price = homeName == null ? rank.getDefaultSetHomeCost() : rank.getNamedSetHomeCost();
        if (price > 0.0) {
            if (!homes.takeMoney(playerUuid, price)) {
                Message.forKeyAndRecipient(Message.Key.NOT_ENOUGH_MONEY, playerUuid).replace("%price%", homes.formatMoney(price)).send();
                return;
            }
        }
        // Apply changes
        homes.getDatabase().save(homeRow);
        // Send message
        if (price > 0.0) {
            if (homeName == null) {
                Message.forKeyAndRecipient(Message.Key.DEFAULT_HOME_SET_WITH_PRICE, playerUuid).replace("%price%", homes.formatMoney(price)).send();
            } else {
                Message.forKeyAndRecipient(Message.Key.NAMED_HOME_SET_WITH_PRICE, playerUuid).replace("%price%", homes.formatMoney(price)).replace("%homename%", homeName).send();
            }
        } else {
            if (homeName == null) {
                Message.forKeyAndRecipient(Message.Key.DEFAULT_HOME_SET, playerUuid).send();
            } else {
                Message.forKeyAndRecipient(Message.Key.NAMED_HOME_SET, playerUuid).replace("%homename%", homeName).send();
            }
        }
    }

    public void listHomes(UUID playerUuid) {
        PlayerRow playerRow = PlayerRow.findOrCreate(playerUuid);
        List<HomeRow> homeRows = HomeRow.find(playerRow);
        Rank rank = Rank.forPlayer(playerUuid);
        List<Message> messages = new ArrayList<>(homeRows.size() * 2);
        Message separator = Message.forKeyAndRecipient(Message.Key.LIST_HOMES_SEPARATOR, playerUuid);
        for (HomeRow homeRow : homeRows) {
            if (!messages.isEmpty()) messages.add(separator);
            if (homeRow.isNamed()) {
                // Named home
                double price = rank.getNamedGoHomeCost();
                Message.Key key = price > 0.0 ? Message.Key.LIST_HOMES_NAMED_ENTRY_WITH_PRICE : Message.Key.LIST_HOMES_NAMED_ENTRY;
                messages.add(Message.forKeyAndRecipient(key, playerUuid).replace("%homename%", homeRow.getName()).replace("%price%", homes.formatMoney(price)));
            } else {
                // Default home
                double price = rank.getDefaultGoHomeCost();
                Message.Key key = price > 0.0 ? Message.Key.LIST_HOMES_DEFAULT_ENTRY_WITH_PRICE : Message.Key.LIST_HOMES_DEFAULT_ENTRY;
                messages.add(Message.forKeyAndRecipient(key, playerUuid).replace("%price%", homes.formatMoney(price)));
            }
        }
        int homeCount = homeRows.size();
        int maxHomes = rank.getMaxHomes() + playerRow.getExtraHomes();
        Message.forKeyAndRecipient(Message.Key.LIST_HOMES, playerUuid).replaceList("%homelist%", messages).replace("%maxhomes%", maxHomes).replace("%homecount%", homeCount).send();
    }

    public void deleteHome(UUID playerUuid, String homeName) {
        HomeRow homeRow = HomeRow.find(playerUuid, homeName);
        if (homeRow == null) {
            Message.forKeyAndRecipient(Message.Key.NAMED_HOME_NOT_FOUND, playerUuid).replace("%homename%", homeName).send();
            return;
        }
        Homes.getInstance().getDatabase().delete(homeRow);
        Message.forKeyAndRecipient(Message.Key.NAMED_HOME_DELETED, playerUuid).replace("%homename%", homeName).send();
    }

    public void inviteHome(UUID playerUuid, UUID inviteeUuid, String homeName) {
        // Find home
        HomeRow homeRow = HomeRow.find(playerUuid, homeName);
        if (homeRow == null) {
            if (homeName == null) {
                Message.forKeyAndRecipient(Message.Key.DEFAULT_HOME_NOT_FOUND, playerUuid).send();
            } else {
                Message.forKeyAndRecipient(Message.Key.NAMED_HOME_NOT_FOUND, playerUuid).replace("%homename%", homeName).send();
            }
            return;
        }
        // Find existing invite
        PlayerRow inviteeRow = inviteeUuid == null ? null : PlayerRow.findOrCreate(inviteeUuid);
        if ((inviteeUuid == null && homeRow.isInvited((UUID)null)) || homeRow.isInvited(inviteeRow)) {
            if (inviteeUuid == null) {
                Message.forKeyAndRecipient(Message.Key.PUBLIC_ALREADY_INVITED, playerUuid).send();
            } else {
                String inviteeName = Homes.getInstance().getPlayerName(inviteeUuid);
                Message.forKeyAndRecipient(Message.Key.PLAYER_ALREADY_INVITED, playerUuid).replace("%playername%", inviteeName).send();
            }
            return;
        }
        // Create invite
        InviteRow inviteRow = InviteRow.create(homeRow, inviteeRow);
        Homes.getInstance().getDatabase().save(inviteRow);
        // Send messages
        if (inviteeUuid == null) {
            Message.forKeyAndRecipient(Message.Key.PUBLIC_INVITED, playerUuid).send();
        } else {
            String inviteeName = Homes.getInstance().getPlayerName(inviteeUuid);
            Message.forKeyAndRecipient(Message.Key.PLAYER_INVITED, playerUuid).replace("%playername%", inviteeName).send();
        }
        if (inviteeUuid != null && !inviteRow.isIgnoredBy(inviteeUuid)) {
            double price = homeName == null ? Rank.forPlayer(inviteeUuid).getDefaultVisitHomeCost() : Rank.forPlayer(inviteeUuid).getNamedVisitHomeCost();
            String playerName = homes.getPlayerName(playerUuid);
            Message.Key key;
            if (homeName == null) key = price > 0.0 ? Message.Key.PLAYER_DID_RECEIVE_DEFAULT_INVITE_WITH_PRICE : Message.Key.PLAYER_DID_RECEIVE_DEFAULT_INVITE;
            else key = price > 0.0 ? Message.Key.PLAYER_DID_RECEIVE_NAMED_INVITE_WITH_PRICE : Message.Key.PLAYER_DID_RECEIVE_NAMED_INVITE;
            Message.forKeyAndRecipient(key, inviteeUuid).replace("%playername%", playerName).replace("%homename%", homeName).replace("%price%", homes.formatMoney(price)).send();
        }
    }

    public void uninviteHome(UUID playerUuid, UUID inviteeUuid, String homeName) {
        // Find home
        HomeRow homeRow = HomeRow.find(playerUuid, homeName);
        if (homeRow == null) {
            if (homeName == null) {
                Message.forKeyAndRecipient(Message.Key.DEFAULT_HOME_NOT_FOUND, playerUuid).send();
            } else {
                Message.forKeyAndRecipient(Message.Key.NAMED_HOME_NOT_FOUND, playerUuid).replace("%homename%", homeName).send();
            }
            return;
        }
        // Find invite
        if (inviteeUuid != null) {
            // Uninvite a specific player
            InviteRow inviteRow = homeRow.getInviteFor(inviteeUuid);
            if (inviteRow == null) {
                String inviteeName = homes.getPlayerName(inviteeUuid);
                Message.forKeyAndRecipient(Message.Key.PLAYER_NOT_INVITED, playerUuid).replace("%playername%", inviteeName).send();
                return;
            }
            Homes.getInstance().getDatabase().delete(inviteRow);
        } else {
            // Uninvite everyone
            List<InviteRow> inviteRows = homeRow.getInvites();
            if (inviteRows == null || inviteRows.isEmpty()) {
                Message.forKeyAndRecipient(Message.Key.NOBODY_IS_INVITED, playerUuid).send();
                return;
            }
            Homes.getInstance().getDatabase().delete(inviteRows);
        }
        // Delete invite
        if (inviteeUuid == null) {
            Message.forKeyAndRecipient(Message.Key.PLAYER_DID_UNINVITE_EVERYONE, playerUuid).send();
        } else {
                String inviteeName = homes.getPlayerName(inviteeUuid);
            Message.forKeyAndRecipient(Message.Key.PLAYER_UNINVITED, playerUuid).replace("%playername%", inviteeName).send();
        }
    }

    public void listInviters(UUID playerUuid, boolean edit) {
        Set<UUID> uuids = new HashSet<>();
        for (InviteRow row : InviteRow.find(playerUuid)) {
            if (row.isIgnoredBy(playerUuid)) continue;
            if (row.getHome().getOwner().getUuid().equals(playerUuid)) continue;
            uuids.add(row.getHome().getOwner().getUuid());
        }
        String[] names = new String[uuids.size()];
        int i = 0;
        for (UUID uuid : uuids) {
            String name = homes.getPlayerName(uuid);
            if (name == null) name = "N/A";
            names[i++] = name;
        }
        Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
        List<Message> list = new ArrayList<>();
        Message separator = Message.forKeyAndRecipient(Message.Key.LIST_INVITERS_SEPARATOR, playerUuid);
        boolean headerSent = false;
        Message.forKeyAndRecipient(Message.Key.LIST_INVITERS_HEADER, playerUuid).replace("%playercount%", names.length).send();
        for (String name : names) {
            if (!list.isEmpty()) list.add(separator);
            list.add(Message.forKeyAndRecipient(Message.Key.LIST_INVITERS_ENTRY, playerUuid).replace("%playername%", name));
            if (edit) {
                list.add(Message.forKeyAndRecipient(Message.Key.LIST_INVITERS_DELETE, playerUuid).replace("%playername%", name));
            }
            if (list.size() >= (edit ? 8 : 5)) {
                Message.forListAndRecipient(list, playerUuid).send();
                list.clear();
            }
        }
        if (!list.isEmpty()) Message.forListAndRecipient(list, playerUuid).send();
    }

    public void listInvites(UUID playerUuid, UUID ownerUuid, boolean edit) {
        Set<String> names = new HashSet<>();
        Message separator = Message.forKeyAndRecipient(Message.Key.LIST_INVITES_SEPARATOR, playerUuid);
        Rank rank = Rank.forPlayer(playerUuid);
        List<Message> list = new ArrayList<>();
        String ownerName = homes.getPlayerName(ownerUuid);
        List<InviteRow> inviteRows = InviteRow.findWithOwnerAndInvitee(ownerUuid, playerUuid);
        int count = 0;
        for (InviteRow inviteRow : inviteRows) {
            if (inviteRow.isIgnoredBy(playerUuid)) continue;
            count += 1;
            if (!list.isEmpty()) list.add(separator);
            HomeRow homeRow = inviteRow.getHome();
            if (!homeRow.isNamed()) {
                double price = rank.getDefaultVisitHomeCost();
                Message.Key key = price > 0.0 ? Message.Key.LIST_INVITES_DEFAULT_ENTRY_WITH_PRICE : Message.Key.LIST_INVITES_DEFAULT_ENTRY;
                list.add(Message.forKeyAndRecipient(key, playerUuid).replace("%playername%", ownerName).replace("%price%", homes.formatMoney(price)));
                if (edit) {
                    list.add(Message.forKeyAndRecipient(Message.Key.LIST_INVITES_DELETE_DEFAULT, playerUuid).replace("%playername%", ownerName));
                }
            } else {
                double price = rank.getNamedVisitHomeCost();
                Message.Key key = price > 0.0 ? Message.Key.LIST_INVITES_NAMED_ENTRY_WITH_PRICE : Message.Key.LIST_INVITES_NAMED_ENTRY;
                list.add(Message.forKeyAndRecipient(key, playerUuid).replace("%playername%", ownerName).replace("%price%", homes.formatMoney(price)).replace("%homename%", homeRow.getName()));
                if (edit) {
                    list.add(Message.forKeyAndRecipient(Message.Key.LIST_INVITES_DELETE_NAMED, playerUuid).replace("%playername%", ownerName).replace("%homename%", homeRow.getName()));
                }
            }
        }
        Message.forKeyAndRecipient(Message.Key.LIST_INVITES, playerUuid).replaceList("%homelist%", list).replace("%playername%", ownerName).replace("%homecount%", count).send();
    }

    public void deleteInvites(UUID playerUuid, UUID ownerUuid) {
        for (InviteRow inviteRow : InviteRow.findWithOwnerAndInvitee(ownerUuid, playerUuid)) {
            if (inviteRow.isPublic()) {
                inviteRow.setIgnoredBy(playerUuid);
            } else {
                Homes.getInstance().getDatabase().delete(inviteRow);
            }
        }
        // Send success message
        String ownerName = Homes.getInstance().getPlayerName(ownerUuid);
        Message.forKeyAndRecipient(Message.Key.PLAYER_DID_DELETE_ALL_INVITES_OF_PLAYER, playerUuid).replace("%playername%", ownerName).send();
    }

    public void deleteInvite(UUID playerUuid, UUID ownerUuid, String homeName) {
        String ownerName = Homes.getInstance().getPlayerName(ownerUuid);
        // Find home
        HomeRow homeRow = HomeRow.find(ownerUuid, homeName);
        if (homeRow == null) {
            Message.forKeyAndRecipient(Message.Key.YOU_ARE_NOT_INVITED, playerUuid).replace("%playername%", ownerName).send();
            return;
        }
        // Find invite
        InviteRow inviteRow = homeRow.getInviteFor(playerUuid);
        if (inviteRow == null) inviteRow = homeRow.getInviteFor((UUID)null);
        else if (inviteRow.isIgnoredBy(playerUuid)) System.out.println("ignored");
        if (inviteRow == null || inviteRow.isIgnoredBy(playerUuid)) {
            Message.forKeyAndRecipient(Message.Key.YOU_ARE_NOT_INVITED, playerUuid).replace("%playername%", ownerName).send();
            return;
        }
        // Commit
        if (inviteRow.isPublic()) {
            if (!inviteRow.setIgnoredBy(playerUuid)) {
                Message.forKeyAndRecipient(Message.Key.YOU_ARE_NOT_INVITED, playerUuid).replace("%playername%", ownerName).send();
                return;
            }
        } else {
            Homes.getInstance().getDatabase().delete(inviteRow);
        }
        // Send success message
        Message.Key key = homeName == null ? Message.Key.PLAYER_DID_DELETE_DEFAULT_INVITE : Message.Key.PLAYER_DID_DELETE_NAMED_INVITE;
        Message.forKeyAndRecipient(key, playerUuid).replace("%playername%", ownerName).replace("%homename%", homeName).send();
    }

    public void buyHome(UUID playerUuid) {
        PlayerRow player = PlayerRow.findOrCreate(playerUuid);
        Rank rank = Rank.forPlayer(playerUuid);
        double price = rank.getBuyHomeCostBase() + rank.getBuyHomeCostGrowth() * (double)player.getExtraHomes();
        if (price <= 0.0) {
            System.err.println(String.format("%s managed to use /buyhome, but the price is negative", homes.getPlayerName(playerUuid)));
            return;
        }
        int maxHomes = rank.getMaxHomes() + player.getExtraHomes();
        Message.forKeyAndRecipient(Message.Key.BUY_HOME_MENU, playerUuid).replace("%maxhomes%", maxHomes).replacePrice(price).send();
    }

    public void buyHome(UUID playerUuid, double confirmPrice) {
        PlayerRow player = PlayerRow.findOrCreate(playerUuid);
        Rank rank = Rank.forPlayer(playerUuid);
        double price = rank.getBuyHomeCostBase() + rank.getBuyHomeCostGrowth() * (double)player.getExtraHomes();
        if (0 != Double.compare(price, confirmPrice)) return;
        if (!homes.takeMoney(playerUuid, price)) {
            Message.forKeyAndRecipient(Message.Key.NOT_ENOUGH_MONEY, playerUuid).replacePrice(price).send();
            return;
        }
        player.giveExtraHome();
        homes.getDatabase().save(player);
        int maxHomes = rank.getMaxHomes() + player.getExtraHomes();
        Message.forKeyAndRecipient(Message.Key.PLAYER_DID_BUY_HOME, playerUuid).replace("%maxhomes%", maxHomes).replacePrice(price).send();
    }
}
