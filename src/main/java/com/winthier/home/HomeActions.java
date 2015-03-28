package com.winthier.home;

import com.winthier.home.sql.*;
import com.winthier.home.util.Players;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This class only exists so Homes.java doesn't get too cluttered.
 */
@RequiredArgsConstructor
public class HomeActions {
    private final Homes homes;
    
    public void goHome(@NonNull UUID playerUuid, String homeName) {
        // Find home
        final HomeRow homeRow = HomeRow.find(playerUuid, homeName);
        if (homeRow == null) {
            Message.Key key = homeName == null ? Message.Key.DEFAULT_HOME_NOT_FOUND : Message.Key.NAMED_HOME_NOT_FOUND;
            key.make(playerUuid).replace("%homename%", homeName).raise();
        }
        checkHomeAndRaise(playerUuid, homeRow);
        // Deduct money
        final Rank rank = Rank.forPlayer(playerUuid);
        final double price = homeName == null ? rank.getDefaultGoHomeCost() : rank.getNamedGoHomeCost();
        if (price > 0.0 && !homes.takeMoney(playerUuid, price)) Message.Key.NOT_ENOUGH_MONEY.make(playerUuid).replace("%price%", homes.formatMoney(price)).raise();
        // Teleport player
        homes.homeForRow(homeRow).teleport(playerUuid);
        // Send messages
        {
            Message.Key key = homeName == null ? (price > 0.0 ? Message.Key.PLAYER_DID_TELEPORT_DEFAULT_HOME_WITH_PRICE : Message.Key.PLAYER_DID_TELEPORT_DEFAULT_HOME) : (price > 0.0 ? Message.Key.PLAYER_DID_TELEPORT_NAMED_HOME_WITH_PRICE : Message.Key.PLAYER_DID_TELEPORT_NAMED_HOME);
            key.make(playerUuid).replace("%homename%", homeName).replace("%price%", homes.formatMoney(price)).sendAndSubtitle();
        }
    }

    public void visitHome(@NonNull UUID playerUuid, @NonNull UUID ownerUuid, String homeName) {
        if (Permission.HOME_OVERRIDE.has(playerUuid)) {
            Home home = homes.findHome(ownerUuid, homeName);
            if (home == null) {
                homes.msg(playerUuid, "&cNot found");
            } else {
                home.teleport(playerUuid);
                homes.msg(playerUuid, "&eTeleported to home");
            }
            return;
        }
        String ownerName = Players.getName(ownerUuid);
        // Check if invited
        if (!InviteRow.isInvited(ownerUuid, homeName, playerUuid)) Message.Key.YOU_ARE_NOT_INVITED.make(playerUuid).replace("%playername%", ownerName).raise();
        // Check home
        HomeRow homeRow = HomeRow.find(ownerUuid, homeName);
        checkHomeAndRaise(playerUuid, homeRow);
        // Deduct money
        Rank rank = Rank.forPlayer(playerUuid);
        double price = homeName == null ? rank.getDefaultVisitHomeCost() : rank.getNamedVisitHomeCost();
        if (price > 0.0 && !homes.takeMoney(playerUuid, price)) Message.Key.NOT_ENOUGH_MONEY.make(playerUuid).replace("%price%", homes.formatMoney(price)).raise();
        // Teleport
        homes.homeForRow(homeRow).teleport(playerUuid);
        // Send message
        Message.Key key = homeName == null ? (price > 0.0 ? Message.Key.PLAYER_DID_VISIT_DEFAULT_HOME_WITH_PRICE : Message.Key.PLAYER_DID_VISIT_DEFAULT_HOME) : (price > 0.0 ? Message.Key.PLAYER_DID_VISIT_NAMED_HOME_WITH_PRICE : Message.Key.PLAYER_DID_VISIT_NAMED_HOME);
        key.make(playerUuid).replace("%playername%", ownerName).replace("%homename%", homeName).replace("%price%", homes.formatMoney(price)).send();
        // Give reward
        Rank ownerRank = Rank.forPlayer(ownerUuid);
        double reward = homeName == null ? ownerRank.getDefaultVisitHomeReward() : ownerRank.getNamedVisitHomeReward();
        if (reward > 0.0 && homes.giveMoney(ownerUuid, reward)) {
            key = homeName == null ? Message.Key.DEFAULT_HOME_REWARDED : Message.Key.NAMED_HOME_REWARDED;
            key.make(ownerUuid).replace("%playername%", Players.getName(playerUuid)).replace("%reward%", homes.formatMoney(reward)).replace("%homename%", homeName).send();
        }
    }

    public void setHome(@NonNull UUID playerUuid, String homeName) {
        // Find existing home or make new one
        HomeRow homeRow = HomeRow.find(playerUuid, homeName);
        if (homeRow == null) {
            int homeCount = homes.countHomes(playerUuid);
            int maxHomes = homes.getTotalMaxHomes(playerUuid);
            if (homeCount >= maxHomes) Message.Key.TOO_MANY_HOMES.make(playerUuid).replace("%maxhomes%", maxHomes).replace("%homecount%", homeCount).raise();
            homeRow = HomeRow.create(playerUuid, homeName);
        }
        // Apply changes and check
        if (!homes.setCurrentLocation(homeRow, playerUuid)) throw new RuntimeException("Failed setting current location for player " + playerUuid);
        checkHomeAndRaise(playerUuid, homeRow);
        // Deduct price, if any
        final Rank rank = Rank.forPlayer(playerUuid);
        final double price = homeName == null ? rank.getDefaultSetHomeCost() : rank.getNamedSetHomeCost();
        if (price > 0.0 && !homes.takeMoney(playerUuid, price)) Message.Key.NOT_ENOUGH_MONEY.make(playerUuid).replace("%price%", homes.formatMoney(price)).raise();
        // Save
        homeRow.save();
        // Send message
        Message.Key key = homeName == null ? (price > 0.0 ? Message.Key.PLAYER_DID_SET_DEFAULT_HOME_WITH_PRICE : Message.Key.PLAYER_DID_SET_DEFAULT_HOME) : (price > 0.0 ? Message.Key.PLAYER_DID_SET_NAMED_HOME_WITH_PRICE : Message.Key.PLAYER_DID_SET_NAMED_HOME);
        key.make(playerUuid).replace("%price%", homes.formatMoney(price)).replace("%homename%", homeName).sendAndSubtitle();
    }

    public void setHomeOverride(@NonNull UUID sender, @NonNull UUID player, String homeName) {
        if (!Permission.HOME_OVERRIDE_EDIT.has(sender)) return;
        HomeRow homeRow = HomeRow.find(player, homeName);
        if (homeRow == null) {
            homeRow = HomeRow.create(player, homeName);
        }
        if (!homes.setCurrentLocation(homeRow, sender)) {
            homes.msg(sender, "&4Failed setting home");
        } else {
            homeRow.save();
            homes.msg(sender, "&ePlayer home set");
        }
    }

    public void listHomes(@NonNull UUID playerUuid) {
        List<HomeRow> homeRows = HomeRow.findAll(playerUuid);
        Rank rank = Rank.forPlayer(playerUuid);
        List<Message> messages = new ArrayList<>(homeRows.size() * 2);
        Message separator = Message.forKeyAndRecipient(Message.Key.LIST_HOMES_SEPARATOR, playerUuid);
        for (HomeRow homeRow : homeRows) {
            if (!messages.isEmpty()) messages.add(separator);
            String homeName = homeRow.getName();
            double price = homeName == null ? rank.getDefaultGoHomeCost() : rank.getNamedGoHomeCost();
            Message.Key key = homeName == null ? (price > 0.0 ? Message.Key.LIST_HOMES_DEFAULT_ENTRY_WITH_PRICE : Message.Key.LIST_HOMES_DEFAULT_ENTRY) : (price > 0.0 ? Message.Key.LIST_HOMES_NAMED_ENTRY_WITH_PRICE : Message.Key.LIST_HOMES_NAMED_ENTRY);
            messages.add(key.make(playerUuid).replace("%homename%", homeName).replace("%price%", homes.formatMoney(price)));
        }
        Message.Key.LIST_HOMES.make(playerUuid).replaceList("%homelist%", messages).replace("%maxhomes%", homes.getTotalMaxHomes(playerUuid)).replace("%homecount%", homeRows.size()).send();
    }

    public void listHomesOverride(@NonNull UUID playerUuid, @NonNull UUID ownerUuid) {
        if (!Permission.HOME_OVERRIDE.has(playerUuid)) return;
        StringBuilder sb = new StringBuilder("Homes of ").append(Players.getName(ownerUuid)).append(":");
        for (Home home : homes.findHomes(ownerUuid)) {
            if (home.isNamed()) {
                sb.append(" ").append(home.getName());
            } else {
                sb.append(" [Default]");
            }
        }
        homes.msg(playerUuid, "&e%s", sb.toString());
    }

    public void deleteHome(@NonNull UUID playerUuid, String homeName) {
        HomeRow homeRow = HomeRow.find(playerUuid, homeName);
        if (homeRow == null) Message.Key.NAMED_HOME_NOT_FOUND.make(playerUuid).replace("%homename%", homeName).raise();
        homeRow.delete();
        Message.Key.PLAYER_DID_DELETE_NAMED_HOME.make(playerUuid).replace("%homename%", homeName).send();
    }

    public void deleteHomeOverride(@NonNull UUID sender, @NonNull UUID player, String homeName) {
        HomeRow homeRow = HomeRow.find(player, homeName);
        if (homeRow == null) {
            homes.msg(sender, "&4Home not found");
        } else {
            homeRow.delete();
            homes.msg(sender, "&eHome deleted");
        }
    }

    public void inviteHome(@NonNull UUID playerUuid, UUID inviteeUuid, String homeName) {
        // Find home
        HomeRow homeRow = HomeRow.find(playerUuid, homeName);
        if (homeRow == null) {
            Message.Key key = homeName == null ? Message.Key.DEFAULT_HOME_NOT_FOUND : Message.Key.NAMED_HOME_NOT_FOUND;
            key.make(playerUuid).replace("%homename%", homeName).raise();
        }
        // Find existing invite
        InviteRow inviteRow = inviteeUuid == null ? InviteRow.findPublic(playerUuid, homeName) : InviteRow.find(playerUuid, homeName, inviteeUuid);
        if (inviteRow != null) {
            if (inviteRow.isPublic()) {
                inviteRow.unignoreAll();
            }
        } else {
            // Create invite
            inviteRow = inviteeUuid == null ? InviteRow.createPublic(playerUuid, homeName) : InviteRow.create(playerUuid, homeName, inviteeUuid);
        }
        // Send message to owner
        Message.Key key = inviteeUuid == null ? Message.Key.PLAYER_DID_INVITE_PUBLIC : Message.Key.PLAYER_DID_INVITE_PLAYER;
        String inviteeName = inviteeUuid == null ? null : Players.getName(inviteeUuid);
        key.make(playerUuid).replace("%playername%", inviteeName).sendAndSubtitle();
        // Send message to invitee
        if (inviteeUuid != null) {
            double price = homeName == null ? Rank.forPlayer(inviteeUuid).getDefaultVisitHomeCost() : Rank.forPlayer(inviteeUuid).getNamedVisitHomeCost();
            String playerName = Players.getName(playerUuid);
            key = homeName == null ? (price > 0.0 ? Message.Key.PLAYER_DID_RECEIVE_DEFAULT_INVITE_WITH_PRICE : Message.Key.PLAYER_DID_RECEIVE_DEFAULT_INVITE) : (price > 0.0 ? Message.Key.PLAYER_DID_RECEIVE_NAMED_INVITE_WITH_PRICE : Message.Key.PLAYER_DID_RECEIVE_NAMED_INVITE);
            key.make(inviteeUuid).replace("%playername%", playerName).replace("%homename%", homeName).replace("%price%", homes.formatMoney(price)).send();
        }
    }

    public void uninviteHome(UUID playerUuid, UUID inviteeUuid, String homeName) {
        String inviteeName = inviteeUuid == null ? null : Players.getName(inviteeUuid);
        // Find home
        HomeRow homeRow = HomeRow.find(playerUuid, homeName);
        if (homeRow == null) {
            Message.Key key = homeName == null ? Message.Key.DEFAULT_HOME_NOT_FOUND : Message.Key.NAMED_HOME_NOT_FOUND;
            key.make(playerUuid).replace("%homename%", homeName).raise();
        }
        // Find invite
        InviteRow inviteRow = inviteeUuid == null ? InviteRow.findPublic(playerUuid, homeName) : InviteRow.find(playerUuid, homeName, inviteeUuid);
        if (inviteRow == null) {
            Message.Key key = inviteeUuid == null ? Message.Key.PUBLIC_NOT_INVITED : Message.Key.PLAYER_NOT_INVITED;
            key.make(playerUuid).replace("%playername%", inviteeName).raise();
        }
        // Delete invite
        inviteRow.delete();
        // Send message
        Message.Key key = inviteeUuid == null ? Message.Key.PLAYER_DID_UNINVITE_PUBLIC : Message.Key.PLAYER_DID_UNINVITE_PLAYER;
        key.make(playerUuid).replace("%playername%", inviteeName).send();
    }

    public void listInviters(UUID playerUuid, boolean edit) {
        Set<UUID> uuids = new HashSet<>();
        for (InviteRow row : InviteRow.findAllForInviteeOrPublic(playerUuid)) {
            if (row.isIgnoredBy(playerUuid)) continue;
            UUID uuid = row.getHome().getOwner().getUuid();
            if (uuid.equals(playerUuid)) continue;
            uuids.add(uuid);
        }
        String[] names = new String[uuids.size()];
        int i = 0;
        for (UUID uuid : uuids) names[i++] = Players.getName(uuid);
        Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
        List<Message> list = new ArrayList<>();
        Message separator = Message.Key.LIST_INVITERS_SEPARATOR.make(playerUuid);
        boolean headerSent = false;
        Message.Key.LIST_INVITERS_HEADER.make(playerUuid).replace("%playercount%", names.length).send();
        for (String name : names) {
            if (!list.isEmpty()) list.add(separator);
            list.add(Message.Key.LIST_INVITERS_ENTRY.make(playerUuid).replace("%playername%", name));
            if (edit) {
                list.add(Message.Key.LIST_INVITERS_DELETE.make(playerUuid).replace("%playername%", name));
            }
            if (list.size() >= (edit ? 8 : 5)) {
                Message.forListAndRecipient(list, playerUuid).send();
                list.clear();
            }
        }
        if (!list.isEmpty()) Message.forListAndRecipient(list, playerUuid).send();
    }

    public void listInvites(@NonNull UUID playerUuid, UUID ownerUuid, boolean edit) {
        Set<String> names = new HashSet<>();
        Message separator = Message.Key.LIST_INVITES_SEPARATOR.make(playerUuid);
        Rank rank = Rank.forPlayer(playerUuid);
        List<Message> list = new ArrayList<>();
        String ownerName = Players.getName(ownerUuid);
        int count = 0;
        for (InviteRow inviteRow : InviteRow.findWithOwnerAndInviteeOrPublic(ownerUuid, playerUuid)) {
            if (inviteRow.isIgnoredBy(playerUuid)) continue;
            count += 1;
            if (!list.isEmpty()) list.add(separator);
            HomeRow homeRow = inviteRow.getHome();
            String homeName = homeRow.getName();
            double price = homeName == null ? rank.getDefaultVisitHomeCost() : rank.getNamedVisitHomeCost();
            Message.Key key = homeName == null ? (price > 0.0 ? Message.Key.LIST_INVITES_DEFAULT_ENTRY_WITH_PRICE : Message.Key.LIST_INVITES_DEFAULT_ENTRY) : (price > 0.0 ? Message.Key.LIST_INVITES_NAMED_ENTRY_WITH_PRICE : Message.Key.LIST_INVITES_NAMED_ENTRY);
            list.add(key.make(playerUuid).replace("%playername%", ownerName).replace("%price%", homes.formatMoney(price)).replace("%homename%", homeRow.getName()));
            if (edit) {
                key = homeName == null ? Message.Key.LIST_INVITES_DELETE_DEFAULT : Message.Key.LIST_INVITES_DELETE_NAMED;
                list.add(key.make(playerUuid).replace("%playername%", ownerName).replace("%homename%", homeRow.getName()));
            }
        }
        Message.Key.LIST_INVITES.make(playerUuid).replaceList("%homelist%", list).replace("%playername%", ownerName).replace("%homecount%", count).send();
    }

    public void listMyInvites(@NonNull UUID sender, boolean edit) {
        List<HomeRow> homeRows = HomeRow.findAll(sender);
        if (homeRows.isEmpty()) Message.Key.PLAYER_HAS_NO_HOMES.make(sender).raise();
        Message.Key.LIST_MY_INVITES_TITLE.make(sender).send();
        Message separator = Message.Key.LIST_MY_INVITES_SEPARATOR.make(sender);
        for (HomeRow homeRow : homeRows) {
            String homeName = homeRow.getName();
            Message.Key key = homeName == null ? Message.Key.LIST_MY_INVITES_DEFAULT_HEADER : Message.Key.LIST_MY_INVITES_NAMED_HEADER;
            Message header = key.make(sender).replace("%homename%", homeName);
            List<Message> contents = new ArrayList<>();
            List<InviteRow> inviteRows = InviteRow.findAll(homeRow);
            // Sort it so the public invite, if any, comes first
        sortLoop:
            for (int i = 1; i < inviteRows.size(); ++i) {
                if (inviteRows.get(i).isPublic()) {
                    InviteRow tmp = inviteRows.get(0);
                    inviteRows.set(0, inviteRows.get(i));
                    inviteRows.set(i, tmp);
                    break sortLoop;
                }
            }
        inviteLoop:
            for (InviteRow inviteRow : InviteRow.findAll(homeRow)) {
                if (!contents.isEmpty()) contents.add(separator);
                UUID inviteeUuid = inviteRow.getInvitee() == null ? null : inviteRow.getInvitee().getUuid();
                String inviteeName = inviteeUuid == null ? null : Players.getName(inviteeUuid);
                key = inviteeUuid == null ? Message.Key.LIST_MY_INVITES_PUBLIC_ENTRY : Message.Key.LIST_MY_INVITES_PLAYER_ENTRY;
                contents.add(key.make(sender).replace("%homename%", homeName).replace("%playername%", inviteeName));
                if (edit) {
                    key = homeName == null ? Message.Key.LIST_MY_INVITES_DELETE_DEFAULT_PLAYER : Message.Key.LIST_MY_INVITES_DELETE_NAMED_PLAYER;
                    contents.add(key.make(sender).replace("%homename%", homeName).replace("%playername%", inviteeName));
                }
            }
            header.replaceList("%playerlist%", contents).send();
        }
    }

    public void deleteInvites(@NonNull UUID playerUuid, UUID ownerUuid) {
        for (InviteRow inviteRow : InviteRow.findWithOwnerAndInviteeOrPublic(ownerUuid, playerUuid)) {
            if (inviteRow.isPublic()) {
                inviteRow.setIgnoredBy(playerUuid);
            } else {
                inviteRow.delete();
            }
        }
        // Send success message
        Message.Key.PLAYER_DID_DELETE_ALL_INVITES_OF_PLAYER.make(playerUuid).replace("%playername%", Players.getName(ownerUuid)).send();
    }

    public void deleteInvite(@NonNull UUID playerUuid, UUID ownerUuid, String homeName) {
        String ownerName = Players.getName(ownerUuid);
        // Find invite
        // Commit
        for (InviteRow inviteRow : InviteRow.findOrPublic(ownerUuid, homeName, playerUuid)) {
            if (inviteRow.isPublic()) {
                inviteRow.setIgnoredBy(playerUuid);
            } else {
                inviteRow.delete();
            }
        }
        // Send success message
        Message.Key key = homeName == null ? Message.Key.PLAYER_DID_DELETE_DEFAULT_INVITE : Message.Key.PLAYER_DID_DELETE_NAMED_INVITE;
        key.make(playerUuid).replace("%playername%", ownerName).replace("%homename%", homeName).send();
    }

    public void buyHome(@NonNull UUID playerUuid) {
        PlayerRow player = PlayerRow.findOrCreate(playerUuid);
        Rank rank = Rank.forPlayer(playerUuid);
        double price = rank.getBuyHomeCost(player.getExtraHomes());
        if (price <= 0.0) {
            System.err.println(String.format("%s managed to use /buyhome, but the price is negative", Players.getName(playerUuid)));
            return;
        }
        int maxHomes = rank.getMaxHomes() + player.getExtraHomes();
        Message.forKeyAndRecipient(Message.Key.BUY_HOME_MENU, playerUuid).replace("%maxhomes%", maxHomes).replacePrice(price).send();
    }

    public void buyHome(@NonNull UUID playerUuid, double confirmPrice) {
        PlayerRow player = PlayerRow.findOrCreate(playerUuid);
        Rank rank = Rank.forPlayer(playerUuid);
        double price = rank.getBuyHomeCost(player.getExtraHomes());
        if (price <= 0.0) {
            System.err.println(String.format("%s managed to use /buyhome, but the price is negative", Players.getName(playerUuid)));
            return;
        }
        if (0 != Double.compare(price, confirmPrice)) return;
        if (!homes.takeMoney(playerUuid, price)) Message.Key.NOT_ENOUGH_MONEY.make(playerUuid).replacePrice(price).raise();
        player.giveExtraHome();
        player.save();
        Message.Key.PLAYER_DID_BUY_HOME.make(playerUuid).replace("%maxhomes%", homes.getTotalMaxHomes(playerUuid)).replacePrice(price).send();
    }

    boolean isHomeInBlacklistedWorld(HomeRow homeRow) {
        return WorldBlacklistRow.isBlacklisted(homeRow.getWorld().getName());
    }

    private boolean isHomeClaimedPrivate(HomeRow homeRow) {
        UUID owner = homeRow.getOwner().getUuid();
        return !com.winthier.claims.Claims.getInstance().canBuild(owner, homeRow.getWorld().getName(), (int)homeRow.getX(), (int)homeRow.getY(), (int)homeRow.getZ());
    }

    boolean isHomeClaimed(HomeRow homeRow) {
        if (!homes.doCheckClaims()) return false;
        return isHomeClaimedPrivate(homeRow);
    }

    void checkHomeAndRaise(@NonNull UUID sender, HomeRow homeRow) {
        UUID owner = homeRow.getOwner().getUuid();
        Message.Key key = null;
        if (isHomeInBlacklistedWorld(homeRow)) {
            if (sender.equals(owner)) key = Message.Key.HOME_IN_BLACKLISTED_WORLD;
            else key = Message.Key.HOME_UNAVAILABLE;
        }
        if (isHomeClaimed(homeRow)) {
            if (sender.equals(owner)) key = Message.Key.HOME_INSIDE_CLAIM;
            else key = Message.Key.HOME_UNAVAILABLE;
        }
        if (key != null) key.make(sender).raise();
    }
}
