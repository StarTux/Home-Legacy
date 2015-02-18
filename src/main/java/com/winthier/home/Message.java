package com.winthier.home;

import com.winthier.home.util.Conf;
import com.winthier.home.util.Strings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.simple.JSONValue;
import org.yaml.snakeyaml.Yaml;

public class Message {
    public static enum Key {
        DEFAULT_HOME_NOT_FOUND,
        NAMED_HOME_NOT_FOUND,
        CANNOT_DELETE_DEFAULT_HOME,

        PLAYER_ALREADY_INVITED,
        PUBLIC_ALREADY_INVITED,

        PLAYER_NOT_INVITED,
        NOBODY_IS_INVITED,
        YOU_ARE_NOT_INVITED,

        PLAYER_UNINVITED,
        PLAYER_DID_UNINVITE_EVERYONE,

        INVALID_HOME_NAME,
        HOME_NAME_TOO_LONG,
        PLAYER_NOT_FOUND,

        NOT_ENOUGH_MONEY,
        TOO_MANY_HOMES,

        TELEPORTED_DEFAULT_HOME,
        TELEPORTED_DEFAULT_HOME_WITH_PRICE,
        TELEPORTED_NAMED_HOME,
        TELEPORTED_NAMED_HOME_WITH_PRICE,

        VISITED_DEFAULT_HOME,
        VISITED_DEFAULT_HOME_WITH_PRICE,
        VISITED_NAMED_HOME,
        VISITED_NAMED_HOME_WITH_PRICE,

        DEFAULT_HOME_REWARDED,
        NAMED_HOME_REWARDED,

        DEFAULT_HOME_SET,
        DEFAULT_HOME_SET_WITH_PRICE,

        NAMED_HOME_SET,
        NAMED_HOME_SET_WITH_PRICE,
        NAMED_HOME_DELETED,

        PLAYER_INVITED,
        PUBLIC_INVITED,

        PLAYER_DID_RECEIVE_DEFAULT_INVITE,
        PLAYER_DID_RECEIVE_NAMED_INVITE,
        PLAYER_DID_RECEIVE_DEFAULT_INVITE_WITH_PRICE,
        PLAYER_DID_RECEIVE_NAMED_INVITE_WITH_PRICE,

        LIST_HOMES,
        LIST_HOMES_SEPARATOR,
        LIST_HOMES_DEFAULT_ENTRY,
        LIST_HOMES_DEFAULT_ENTRY_WITH_PRICE,
        LIST_HOMES_NAMED_ENTRY,
        LIST_HOMES_NAMED_ENTRY_WITH_PRICE,

        LIST_INVITERS_HEADER,
        LIST_INVITERS_SEPARATOR,
        LIST_INVITERS_ENTRY,
        LIST_INVITERS_DELETE,

        LIST_INVITES,
        LIST_INVITES_SEPARATOR,
        LIST_INVITES_DEFAULT_ENTRY,
        LIST_INVITES_DEFAULT_ENTRY_WITH_PRICE,
        LIST_INVITES_NAMED_ENTRY,
        LIST_INVITES_NAMED_ENTRY_WITH_PRICE,
        LIST_INVITES_DELETE_DEFAULT,
        LIST_INVITES_DELETE_NAMED,

        PLAYER_DID_DELETE_DEFAULT_INVITE,
        PLAYER_DID_DELETE_NAMED_INVITE,
        PLAYER_DID_DELETE_ALL_INVITES_OF_PLAYER,

        BUY_HOME_MENU,
        PLAYER_DID_BUY_HOME,
        ;
        public final String key;
        Key() {
            this.key = Strings.camelCase(name());
        }
    }

    private static Map<String, Object> store;
    private Object root;
    private final Map<String, Object> replacements = new HashMap<>();
    private final UUID recipient;

    private Message(Object root, UUID recipient) {
        this.root = root;
        this.recipient = recipient;
    }

    private static void loadStore() {
        @SuppressWarnings("unchecked")
            Map<String, Object> tmp = (Map<String, Object>)new Yaml().load(Homes.getInstance().getResource("messages.yml"));
        store = tmp;
        for (Key key : Key.values()) {
            if (!store.containsKey(key.key)) {
                System.err.println("Missing message with key " + key.key);
            }
        }
    }

    public static void clearStore() {
        store = null;
    }

    public static Object rawMessage(Key key) {
        if (store == null) loadStore();
        Object result = store.get(key.key);
        if (result == null) {
            System.err.println("[Home] Message for key not found: " + key + ", " + key.key);
            return null;
        }
        return result;
    }

    public static Message forKeyAndRecipient(Key key, UUID recipient) {
        Object raw = rawMessage(key);
        Message result = new Message(Conf.deepCopy(raw), recipient);
        return result;
    }

    public static Message forListAndRecipient(List<Message> list, UUID recipient) {
        List<Object> root = new ArrayList<>(list.size());
        for (Message message : list) root.add(message.replaceAll());
        return new Message(root, recipient);
    }

    public Message replace(String from, Object to) {
        if (to == null) to = "";
        replacements.put(from, to);
        return this;
    }

    public Message replacePrice(double price) {
        replace("%price%", Homes.getInstance().formatMoney(price));
        replace("%rawprice%", String.format("%.02f", price));
        return this;
    }

    public Message replaceList(String key, List<Message> messages) {
        List<Object> replacement = new ArrayList<>(messages.size());
        for (Message message : messages) replacement.add(message.replaceAll());
        Conf.replaceList(root, key, replacement);
        return this;
    }

    // public Message fill(PlayerRow playerRow, List<HomeRow> homes, Rank rank) {
    //     final UUID uuid = row.getUuid();
    //     replace("%playeruuid%", uuid);
    //     replace("%player%", Homes.getInstance().getPlayerName(uuid));
    //     replace("%homecount%", homes.size());
    //     replace("%maxhomes%", rank.getMaxHomes() + playerRow.getExtraHomes());
    //     return this;
    // }

    private Object replaceAll() {
        return Conf.replace(root, replacements);
    }

    public boolean send() {
        if (root == null) return false;
        String playerName = Homes.getInstance().getPlayerName(recipient);
        if (playerName != null) replace("%sendername%", playerName);
        replace("%senderuuid%", recipient);
        Object raw = replaceAll();
        String json = JSONValue.toJSONString(raw);
        return Homes.getInstance().sendJSONMessage(recipient, json);
    }

    public String parse() {
        if (root == null) return "";
        return JSONValue.toJSONString(root);
    }
}