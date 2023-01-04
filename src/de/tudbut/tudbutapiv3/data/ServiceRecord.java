package de.tudbut.tudbutapiv3.data;

import java.util.Base64;

import tudbut.parsing.JSON;
import tudbut.parsing.TCN;
import tudbut.parsing.TCNArray;
import tudbut.tools.encryption.RawKey;

public class ServiceRecord {

    public UserRecord parent;
    public TCN data;

    public ServiceRecord(UserRecord parent, ServiceData serviceData) {
        this.parent = parent;
        this.data = new TCN();
        serviceData.data.getArray("users").add(parent.uuid.toString());
        data.set("service", serviceData.name);
        data.set("useTime", 0L);
        data.set("lastUse", System.currentTimeMillis());
        data.set("messageToken", Base64.getEncoder().encodeToString(Database.key.encryptString(new RawKey().toString()).getBytes()));
        data.set("messages", new TCNArray());
        data.set("dataMessages", new TCNArray());
        data.set("version", "v0.0.0a");
        data.set("premiumStatus", 0);
        data.set("lastMessageSent", System.currentTimeMillis());
        data.set("data", new TCN());
    }

    public ServiceRecord(UserRecord parent, TCN data) {
        this.parent = parent;
        this.data = data;
    }

    public RawKey login(String version) {
        RawKey key = new RawKey();
        data.set("version", version);
        data.set("messageToken", Base64.getEncoder().encodeToString(Database.key.encryptString(key.toString()).getBytes()));
        // Clear messages so that there wont be any unobtainable ones
        data.getArray("messages").clear();
        data.getArray("dataMessages").clear();
        return key;
    }

    public void use() {
        parent.online();
        if(data.getLong("lastUse") > System.currentTimeMillis() - 1500) {
            long l = System.currentTimeMillis() - data.getLong("lastUse");
            data.set("useTime", data.getLong("useTime") + l);
            Database.service(data.getString("service")).use(l);
        }
        data.set("lastUse", System.currentTimeMillis());
    }

    public RawKey decryptKey() {
        return new RawKey(Database.key.decryptString(new String(Base64.getDecoder().decode(data.getString("messageToken")))));
    }

    public RawKey clientSideKey() {
        return new RawKey(decryptKey().toHashString());
    }

    public void message(TCN msg) {
        while(data.getArray("messages").size() > 10)
            data.getArray("messages").remove(0);
        data.getArray("messages").add(Base64.getEncoder().encodeToString(clientSideKey().encryptString(JSON.write(msg)).getBytes()));
    }
    public void dataMessage(TCN msg) {
        while(data.getArray("dataMessages").size() > 10)
            data.getArray("dataMessages").remove(0);
        data.getArray("dataMessages").add(Base64.getEncoder().encodeToString(clientSideKey().encryptString(JSON.write(msg)).getBytes()));
    }
}
