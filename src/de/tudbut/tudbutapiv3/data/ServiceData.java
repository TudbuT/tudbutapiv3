package de.tudbut.tudbutapiv3.data;

import java.util.Base64;
import java.util.UUID;

import de.tudbut.tools.Hasher;
import tudbut.parsing.TCN;
import tudbut.parsing.TCNArray;

public class ServiceData {

    public TCN data;
    public String name;

    public ServiceData(String name, String servicePassword) {
        this.name = name;
        data = new TCN();
        data.set("users", new TCNArray());
        data.set("useTime", 0L);
        data.set("password", Base64.getEncoder().encodeToString(Database.key.encryptString(Hasher.sha512hex(Hasher.sha512hex(servicePassword))).getBytes()));
        data.set("allowChat", false);
        data.set("data", new TCN());
    }

    public String getServicePassHash() {
        return Database.key.decryptString(new String(Base64.getDecoder().decode(data.getString("password"))));
    }

    public ServiceData(String name, TCN data) {
        this.name = name;
        this.data = data;
    }

    public ServiceRecord[] getUsers() {
        TCNArray users = data.getArray("users");
        ServiceRecord[] records = new ServiceRecord[users.size()];
        // records.length is faster than users.size()
        for(int i = 0; i < records.length; i++) {
            UserRecord user = Database.getUser(UUID.fromString(users.getString(i)));
            records[i] = user.service(this).ok().await();
        }
        return records;
    }

    public void use(long l) {
        data.set("useTime", data.getLong("useTime") + l);
    }
}
