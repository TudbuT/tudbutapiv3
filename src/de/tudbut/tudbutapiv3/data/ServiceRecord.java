package de.tudbut.tudbutapiv3.data;

import tudbut.parsing.TCN;

public class ServiceRecord {

    public UserRecord parent;
    public TCN data;

    public ServiceRecord(UserRecord parent, ServiceData serviceData) {
        this.parent = parent;
        this.data = new TCN();
        data.set("service", serviceData.name);
        data.set("useTime", 0L);
    }

    public ServiceRecord(UserRecord parent, TCN data) {
        this.data = data;
    }
}
