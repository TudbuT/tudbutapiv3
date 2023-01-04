package de.tudbut.tudbutapiv3.auth;

import java.util.HashMap;
import java.util.UUID;

import de.tudbut.tools.Tools;
import tudbut.net.http.HTTPRequest;
import tudbut.net.http.HTTPRequestType;
import tudbut.net.http.ParsedHTTPValue;
import tudbut.parsing.JSON;
import tudbut.parsing.TCN;

public class GameAuthentication {

    UUID user;
    String username;
    String currentServerHash;

    static HashMap<UUID, GameAuthentication> map = new HashMap<>();

    private GameAuthentication(UUID user, String username) {
        this.user = user;
        this.username = username;
        currentServerHash = Tools.randomString(20, "0123456789abcdef"); // This is an invalid format, so we don't conflict with real servers!
    }

    public static GameAuthentication get(UUID uuid, String username) {
        if(map.containsKey(uuid) || username == null)
            return map.get(uuid);
        GameAuthentication auth = new GameAuthentication(uuid, username);
        map.put(uuid, auth);
        return auth;
    }

    public String getServerToJoin() {
        return currentServerHash;
    }

    public boolean isAuthenticated() {
        try {
            HTTPRequest request = new HTTPRequest(HTTPRequestType.GET, "https://sessionserver.mojang.com", 443, "/session/minecraft/hasJoined?username=" + username + "&serverId=" + currentServerHash);
            ParsedHTTPValue value = request.send().parse();
            if(value.getStatusCode() == 200) {
                TCN json = JSON.read(value.getBody());
                if(json.getString("id").equals(user.toString().replace("-", "")) && json.getString("name").equals(username)) {
                    map.remove(user);
                    return true;
                }
            }
        } catch(Exception e) {}
        return false;
    }
}
