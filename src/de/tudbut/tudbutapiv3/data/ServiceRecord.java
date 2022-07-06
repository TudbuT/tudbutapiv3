package de.tudbut.tudbutapiv3.data;

import tudbut.parsing.TCN;

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
    }

    public ServiceRecord(UserRecord parent, TCN data) {
        this.data = data;
    }

    public void use() {
        if(data.getLong("lastUse") > System.currentTimeMillis() - 1500) {
            data.set("useTime", data.getLong("useTime") + (System.currentTimeMillis() - data.getLong("lastUse")));
        }
        data.set("lastUse", System.currentTimeMillis());
    }
}
