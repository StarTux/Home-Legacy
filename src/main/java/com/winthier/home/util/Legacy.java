// package com.winthier.home.util;

// import com.winthier.home.Homes;
// import com.winthier.home.sql.HomeRow;
// import com.winthier.home.sql.InviteRow;
// import com.winthier.home.util.Strings;
// import com.winthier.playercache.PlayerCache;
// import java.io.BufferedReader;
// import java.io.InputStreamReader;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.UUID;
// import javax.persistence.PersistenceException;

// public class Legacy {
//     private static UUID uuidForName(String name) {
//             UUID uuid = PlayerCache.uuidForName(name);
//             if (uuid == null) uuid = PlayerCache.uuidForLegacyName(name);
//             return uuid;
//     }

//     private void migrateHomes() throws Exception {
//         BufferedReader in = new BufferedReader(new InputStreamReader(Homes.getInstance().getResource("homes.txt")));
//         String line;
//         int linum = 0;
//         int successCount = 0;
//         while (null != (line = in.readLine())) {
//             linum += 1;
//             if (line.isEmpty() || line.startsWith("#")) continue;
//             String[] tokens = line.split(";");
//             if (tokens.length != 7 && tokens.length != 8) {
//                 System.out.println(String.format("[Home] Skipping line %d: %s", linum, line));
//                 continue;
//             }
//             String ownerName = tokens[0];
//             double x = Double.parseDouble(tokens[1]);
//             double y = Double.parseDouble(tokens[2]);
//             double z = Double.parseDouble(tokens[3]);
//             float pitch = Float.parseFloat(tokens[4]);
//             float yaw = Float.parseFloat(tokens[5]);
//             String worldName = tokens[6];
//             String homeName = tokens.length < 8 ? null : tokens[7];
//             if (homeName != null && homeName.isEmpty()) homeName = null;
//             if (homeName != null && !Strings.isValidHomeName(homeName)) {
//                 String tmp = Strings.fixInvalidHomeName(homeName);
//                 System.out.println(String.format("Fixing invalid home name %s:%s => %s", ownerName, homeName, tmp));
//                 homeName = tmp;
//             }

//             UUID ownerUuid = uuidForName(ownerName);
//             if (ownerUuid == null) {
//                 System.err.println(String.format("[Home] Player name in line %d unknown: %s. Skipping.", linum, ownerName));
//                 continue;
//             }

//             try {
//                 Homes.getInstance().getDatabase().save(HomeRow.create(ownerUuid, homeName, worldName, x, y, z, yaw, pitch));
//             } catch (PersistenceException pe) {
//                 System.out.println(String.format("Issue with home in line %d: %s:%s", linum, ownerName, homeName));
//                 continue;
//             }
            
//             successCount++;
//         }
//         System.out.println(String.format("Parsed %d homes", successCount));
//     }

//     private void migrateInvites() throws Exception {
//         BufferedReader in = new BufferedReader(new InputStreamReader(Homes.getInstance().getResource("invites.txt")));
//         String line;
//         int linum = 0;
//         int successCount = 0;
//         int noHomeCount = 0;
//         int noInviteeCount = 0;
//         while (null != (line = in.readLine())) {
//             linum += 1;
//             if (line.isEmpty() || line.startsWith("#")) continue;
//             String[] tokens = line.split(";");
//             if (tokens.length < 3) {
//                 System.out.println(String.format("[Invite] Skipping line %d: %s", linum, line));
//                 continue;
//             }
//             String ownerName = tokens[0];
//             String homeName = tokens[1];
//             if (homeName != null && homeName.isEmpty()) homeName = null;
//             if (homeName != null && !Strings.isValidHomeName(homeName)) {
//                 String tmp = Strings.fixInvalidHomeName(homeName);
//                 System.out.println(String.format("Fixing invalid home name %s:%s => %s", ownerName, homeName, tmp));
//                 homeName = tmp;
//             }
//             String inviteeName = tokens[2];
//             if (inviteeName.isEmpty() || inviteeName.equals("*")) inviteeName = null;

//             UUID ownerUuid = uuidForName(ownerName);
//             if (ownerUuid == null) {
//                 System.err.println(String.format("[Invite] Owner name in line %d unknown: %s. Skipping.", linum, ownerName));
//                 continue;
//             }
//             UUID inviteeUuid = null;
//             if (inviteeName != null) {
//                 inviteeUuid = uuidForName(inviteeName);
//                 if (inviteeUuid == null) {
//                     //System.err.println(String.format("[Invite] Invitee name in line %d unknown: %s. Skipping.", linum, inviteeName));
//                     noInviteeCount += 1;
//                     continue;
//                 }
//             }

//             HomeRow homeRow = HomeRow.find(ownerUuid, homeName);
//             if (homeRow == null) {
//                 noHomeCount += 1;
//                 continue;
//             }

//             try {
//                 Homes.getInstance().getDatabase().save(InviteRow.create(homeRow, inviteeUuid));
//             } catch (PersistenceException pe) {
//                 System.out.println(String.format("Issue with home invite in line %d: %s %s:%s", linum, inviteeName, ownerName, homeName));
//             }

//             successCount++;
//         }
//         System.out.println(String.format("Parsed %d invites", successCount));
//         System.out.println(String.format("Found %d invites without a home", noHomeCount));
//         System.out.println(String.format("Found %d invites without a bad invitee name", noInviteeCount));
//     }

//     public static void migrate() {
//         Legacy legacy = new Legacy();
//         try {
//             legacy.migrateHomes();
//             legacy.migrateInvites();
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }
// }
