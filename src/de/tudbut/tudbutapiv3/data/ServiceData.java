package de.tudbut.tudbutapiv3.data;

import java.util.UUID;

import tudbut.parsing.TCN;
import tudbut.parsing.TCNArray;

public class ServiceData {

    public TCN data;
    public String name;

    public ServiceData(String name) {
        this.name = name;
        data = new TCN();
        data.set("users", new TCNArray());
        data.set("useTime", 0L);
    }

    public ServiceData(String name, TCN data) {
        this.name = name;
        this.data = data;
    }

    public ServiceRecord[] getUsers() {
        TCNArray users = TCNArray.fromTCN(data.getSub("users"));
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
