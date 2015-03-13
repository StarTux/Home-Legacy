package com.winthier.home;

import com.avaje.ebean.SqlRow;
import com.winthier.home.Rank;
import com.winthier.home.sql.HomeRow;
import com.winthier.home.sql.IgnoreInviteRow;
import com.winthier.home.sql.InviteRow;
import com.winthier.home.sql.PlayerRow;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdminCommands {
    private final Homes homes;

    private void usage(UUID sender) {
        if (Permission.HOME_ADMIN_LIST.has(sender))
            homes.msg(sender, "/HomeAdmin List <Player> - List player homes");
        if (Permission.HOME_ADMIN_GOTO.has(sender))
            homes.msg(sender, "/HomeAdmin GoTo <Player>:<Home> - List player homes");
        if (Permission.HOME_ADMIN_GRANT.has(sender))
            homes.msg(sender, "/HomeAdmin Grant <Player> - Grant player an extra home");
        if (Permission.HOME_ADMIN_RELOAD.has(sender))
            homes.msg(sender, "/HomeAdmin Reload - Reload configuration");
        if (Permission.HOME_ADMIN_CONSISTENCY.has(sender))
            homes.msg(sender, "/HomeAdmin Consistency (Fix) - Detect and optionally fix consistency issues");
        // if (Permission.HOME_ADMIN_IMPORT.has(sender))
        //     homes.msg(sender, "/HomeAdmin Import - Import legacy homes.txt and invites.txt");
    }

    public boolean command(UUID sender, String[] args) {
        try {
            boolean result = _command(sender, args);
            if (!result) usage(sender);
        } catch (HomeException he) {
            homes.msg(sender, "&c%s", he.getMessage());
        }
        return true;
    }

    private boolean _command(UUID sender, String[] args) throws HomeException {
            if (args.length == 0) {
                return false;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
                if (!Permission.HOME_ADMIN_LIST.has(sender)) return false;
                String playerName = args[1];
                UUID playerUuid = homes.getPlayerUuid(playerName);
                if (playerUuid == null) throw new HomeException("Player not found: " + playerName);
                String tmp = homes.getPlayerName(playerUuid);
                if (tmp != null) playerName = tmp;
                PlayerRow player = PlayerRow.find(playerUuid);
                if (player == null) throw new HomeException("Player " + playerName + " has no homes on this server");
                List<HomeRow> homeList = HomeRow.find(playerUuid);
                homes.msg(sender, "&bHomes of %s (%d/%d):", playerName, homeList.size(), player.getTotalMaxHomes());
                for (HomeRow home : homeList) {
                    String homeName = home.isNamed() ? home.getName() : "[default]";
                    homes.msg(sender, "&b- %s %s,%d,%d,%d", homeName, home.getWorld().getName(), (int)home.getX(), (int)home.getY(), (int)home.getZ());
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("goto")) {
                if (!Permission.HOME_ADMIN_GOTO.has(sender)) return false;
                String homeArg = args[1];
                if (!homeArg.contains(":")) return false;
                String[] tokens = homeArg.split(":");
                if (tokens.length < 1 || tokens.length > 2) return false;
                String playerName = tokens[0];
                String homeName = tokens.length >= 2 ? tokens[1] : null;
                UUID playerUuid = homes.getPlayerUuid(playerName);
                if (playerUuid == null) throw new HomeException("Player not found: " + playerName);
                String tmp = homes.getPlayerName(playerUuid);
                if (tmp != null) playerName = tmp;
                HomeRow home = HomeRow.find(playerUuid, homeName);
                if (home == null) throw new HomeException("Home not found: " + homeArg);
                homes.msg(sender, "&bTeleporting you to %s:%s", home.getOwner().getName(), home.getNiceName());
                homes.homeForRow(home).teleport(sender);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("grant")) {
                if (!Permission.HOME_ADMIN_GRANT.has(sender)) return false;
                String playerName = args[1];
                UUID playerUuid = homes.getPlayerUuid(playerName);
                if (playerUuid == null) throw new HomeException("Player not found: " + playerName);
                String tmp = homes.getPlayerName(playerUuid);
                if (tmp != null) playerName = tmp;
                PlayerRow player = PlayerRow.findOrCreate(playerUuid);
                int extraHomes = player.getExtraHomes() + 1;
                player.setExtraHomes(extraHomes);
                homes.msg(sender, "&bBumped extra home count of %s to %d", playerName, extraHomes);
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!Permission.HOME_ADMIN_RELOAD.has(sender)) return false;
                homes.reload();
                homes.msg(sender, "&bConfiguration reloaded");
            // } else if (args.length == 1 && args[0].equalsIgnoreCase("import")) {
            //     if (!Permission.HOME_ADMIN_IMPORT.has(sender)) return false;
            //     migrate(sender);
            } else if (args.length <= 2 && args[0].equalsIgnoreCase("consistency")) {
                if (!Permission.HOME_ADMIN_CONSISTENCY.has(sender)) return false;
                boolean fix = false;
                if (args.length >= 2) {
                    if ("fix".equalsIgnoreCase(args[1])) fix = true;
                    else return false;
                }
                consistency(sender, fix);
            } else {
                return false;
            }
        return true;
    }

    // private void migrate(UUID sender) {
    //     homes.msg(sender, "Starting migration...");
    //     com.winthier.home.util.Legacy.migrate();
    //     homes.msg(sender, "Finished migration...");
    // }

    private void consistency(UUID sender, boolean fix) {
        // Remove duplicate playerList
        homes.msg(sender, "Checking duplicate players");
        {
            List<PlayerRow> playerList = new ArrayList<>();
            for (SqlRow row : homes.getDatabase().createSqlQuery("SELECT players.id AS id FROM players LEFT OUTER JOIN (SELECT id, uuid FROM players GROUP BY uuid) AS keep ON players.id = keep.id WHERE keep.id IS NULL").findList()) {
                PlayerRow player = PlayerRow.forId(row.getInteger("id"));
                playerList.add(player);
                if (!fix) {
                    homes.msg(sender, "[%d] Duplicate player entry: %s (%s)", player.getId(), player.getName(), player.getUuid());
                }
            }
            if (fix && !playerList.isEmpty()) {
                int result = homes.getDatabase().delete(playerList);
                homes.msg(sender, "Removed %d duplicate players", result);
            }
        }
        // Remove duplicate homes
        homes.msg(sender, "Checking duplicate homes");
        {
            List<HomeRow> homeList = new ArrayList<>();
            for (SqlRow row : homes.getDatabase().createSqlQuery("SELECT homes.id AS id FROM homes LEFT OUTER JOIN (SELECT MIN(id) as id, owner_id, name FROM homes GROUP BY owner_id, name) AS keep ON homes.id = keep.id WHERE keep.id IS NULL").findList()) {
                HomeRow home = HomeRow.forId(row.getInteger("id"));
                homeList.add(home);
                if (!fix) {
                    homes.msg(sender, "[%d] Duplicate home: %s:%s", home.getId(), home.getOwner().getName(), home.getNiceName());
                }
            }
            if (fix && !homeList.isEmpty()) {
                int result = homes.getDatabase().delete(homeList);
                homes.msg(sender, "Removed %d duplicate homes", result);
            }
        }
        homes.msg(sender, "Checking duplicate invites");
        // Remove duplicate invites
        {
            List<InviteRow> inviteList = new ArrayList<>();
            for (SqlRow row : homes.getDatabase().createSqlQuery("SELECT invites.id AS id FROM invites LEFT OUTER JOIN (SELECT id, home_id, invitee_id FROM invites GROUP BY home_id, invitee_id) AS keep ON invites.id = keep.id WHERE keep.id IS NULL").findList()) {
                InviteRow invite = InviteRow.forId(row.getInteger("id"));
                inviteList.add(invite);
                if (!fix) {
                    homes.msg(sender, "[%d] Duplicate invite for %s to %s:%s", invite.getId(), invite.getInvitee().getName(), invite.getHome().getOwner().getName(), invite.getHome().getNiceName());
                }
            }
            if (fix && !inviteList.isEmpty()) {
                int result = homes.getDatabase().delete(inviteList);
                homes.msg(sender, "Removed %d duplicate invites", result);
            }
        }
        homes.msg(sender, "Checking duplicate ignores");
        // Remove duplicate ignores
        {
            List<IgnoreInviteRow> ignoreList = new ArrayList<>();
            for (SqlRow row : homes.getDatabase().createSqlQuery("SELECT ignore_invites.id AS id FROM ignore_invites LEFT OUTER JOIN (SELECT id, player_id, invite_id FROM ignore_invites GROUP BY player_id, invite_id) AS keep ON ignore_invites.id = keep.id WHERE keep.id IS NULL").findList()) {
                IgnoreInviteRow ignore = IgnoreInviteRow.forId(row.getInteger("id"));
                ignoreList.add(ignore);
                if (!fix) {
                    homes.msg(sender, "[%d] Duplicate ignore for %s of %s:%s", ignore.getId(), ignore.getPlayer().getName(), ignore.getInvite().getHome().getOwner().getName(), ignore.getInvite().getHome().getName());
                }
            }
            if (fix && !ignoreList.isEmpty()) {
                int result = homes.getDatabase().delete(ignoreList);
                homes.msg(sender, "Removed %d duplicate ignored", result);
            }
        }
        homes.msg(sender, "Checking excess homes");
        // Remove excess homes (kind of)
        {
            int max = 0;
            for (Rank rank : Rank.allRanks()) if (rank.getMaxHomes() > max) max = rank.getMaxHomes();
            List<HomeRow> deleteList = new ArrayList<>();
            for (SqlRow row : homes.getDatabase().createSqlQuery("SELECT id FROM players LEFT OUTER JOIN (SELECT owner_id, COUNT(*) AS count FROM homes GROUP BY owner_id) AS homes ON players.id = homes.owner_id WHERE homes.count > players.extra_homes + :max").setParameter("max", max).findList()) {
                PlayerRow player = PlayerRow.forId(row.getInteger("id"));
                List<HomeRow> homeList = player.getHomes();
                if (homeList == null) continue;
                final int excess = homeList.size() - player.getTotalMaxHomes();
                if (excess <= 0) continue;
                // Problems start here...
                if (!fix) {
                    homes.msg(sender, "[%d] %s has %d more homes than allowed", player.getId(), player.getName(), excess);
                }
                if (fix) {
                    for (int i = 0; i < excess; ++i) {
                        deleteList.add(homeList.get(homeList.size() - 1 - i));
                    }
                }
            }
            if (fix) {
                int result = homes.getDatabase().delete(deleteList);
                homes.msg(sender, "Removed %d excess homes", result);
            }
        }
        homes.msg(sender, "Checking obsolete invites");
        // Remove obsolete invites
        // {
        //     List<InviteRow> inviteList = new ArrayList<>();
        //     for (SqlRow row : homes.getDatabase().createSqlQuery("SELECT id FROM invites JOIN (SELECT home_id FROM invites WHERE invitee_id IS NULL) AS publics ON invites.home_id = publics.home_id WHERE invitee_id IS NOT NULL").findList()) {
        //         InviteRow invite = InviteRow.forId(row.getInteger("id"));
        //         inviteList.add(invite);
        //         if (!fix) {
        //             homes.msg(sender, "[%d] Obsolete invite for %s to %s:%s", invite.getId(), invite.getInvitee().getName(), invite.getHome().getOwner().getName(), invite.getHome().getNiceName());
        //         }
        //     }
        //     if (fix && !inviteList.isEmpty()) {
        //         int result = homes.getDatabase().delete(inviteList);
        //         homes.msg(sender, "Removed %d obsolete invites", result);
        //     }
        // }
        homes.msg(sender, "Consistency check finished");
    }
}
