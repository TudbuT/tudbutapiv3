package de.tudbut.tudbutapiv3.data;

import java.util.UUID;

import de.tudbut.async.Task;
import tudbut.parsing.TCN;
import tudbut.parsing.TCNArray;

import static de.tudbut.async.Async.*;

public class UserRecord {

    public TCN data;
    public UUID uuid;

    public UserRecord(UUID uuid) {
        this.uuid = uuid;
        data = new TCN();
        data.set("services", new TCN());
        data.set("lastNameFetch", 0L);
        NameFetcher.fetch(uuid).then(x -> {
            setName(x);
            data.set("lastNameFetch", System.currentTimeMillis());
        }).err(e -> data.set("name", "FETCH_ERROR_" + uuid)).ok().await();
        data.set("onlineTime", 0L);
        data.set("lastOnline", 0L);
        data.set("passwordHash", "");
    }

    public UserRecord(UUID uuid, TCN data) {
        this.uuid = uuid;
        this.data = data;
        getName().ok();
    }

    private void setName(String name) {
        data.set("name", name);
        Database.data.getSub("nameToUUID").set(name, uuid.toString());
    }

    public Task<String> getName() {
        return t((res, rej) -> {
            if(data.getLong("lastNameFetch") < System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000) {
                NameFetcher.fetch(uuid).then(x -> {
                    setName(x);
                    data.set("lastNameFetch", System.currentTimeMillis());
                }).err(e -> setName("FETCH_ERROR_" + uuid)).ok().await();
            }
            res.call(data.getString("name"));
        });
    }

    public Task<ServiceRecord[]> getServices() {
        return t((res, rej) -> {
            TCNArray services = TCNArray.fromTCN(data.getSub("services"));
            ServiceRecord[] records = new ServiceRecord[services.size()];
            for(int i = 0; i < records.length; i++) {
                records[i] = new ServiceRecord(this, services.getSub(i));
                unblockQueue();
            }
            res.call(records);
        });
    }

    public Task<ServiceRecord> service(ServiceData service) {
        return t((res, rej) -> {
            if(data.getSub("services").getSub(service.name) != null) {
                TCN record = data.getSub("services").getSub(service.name);
                res.call(new ServiceRecord(this, record));
            }
            else {
                ServiceRecord record = new ServiceRecord(this, service);
                data.getSub("services").set(service.name, record.data);
                res.call(record);
            }
        });
    }

    public void online() {
        if(data.getLong("lastOnline") > System.currentTimeMillis() - 1500) {
            data.set("onlineTime", data.getLong("onlineTime") + (System.currentTimeMillis() - data.getLong("lastOnline")));
        }
        data.set("lastOnline", System.currentTimeMillis());
    }
}
