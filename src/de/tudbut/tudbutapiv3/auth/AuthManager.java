package de.tudbut.tudbutapiv3.auth;

import java.util.HashMap;
import java.util.UUID;

import de.tudbut.tools.Tools;

public class AuthManager {

    public static HashMap<UUID, AuthManager> managers = new HashMap<>();

    public final String token;
    public final UUID uuid;

    private AuthManager(String token, UUID uuid) {
        this.token = token;
        this.uuid = uuid;
    }
    
    public static AuthManager create(UUID uuid) {
        AuthManager manager = new AuthManager(Tools.randomAlphanumericString(50), uuid);
        managers.put(uuid, manager);
        return manager;
    }

    public static AuthManager get(UUID uuid) { return managers.get(uuid); }

    public void delete() {
        managers.remove(uuid);
    }
}
