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
    }

    public ServiceRecord(UserRecord parent, TCN data) {
        this.parent = parent;
        this.data = data;
    }

    public RawKey login() {
        RawKey key = new RawKey();
        data.set("messageToken", Base64.getEncoder().encodeToString(Database.key.encryptString(key.toString()).getBytes()));
        // Clear messages so that there wont be any unobtainable ones
        data.getArray("messages").clear();
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

    public void message(TCN msg) {
        data.getArray("messages").add(Base64.getEncoder().encodeToString(decryptKey().encryptString(JSON.write(msg)).getBytes()));
    }
}
