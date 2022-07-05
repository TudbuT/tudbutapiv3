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
        data.set("services", new TCNArray());
        data.set("lastNameFetch", 0L);
        NameFetcher.fetch(uuid).then(x -> {
            data.set("name", x);
            data.set("lastNameFetch", System.currentTimeMillis());
        }).err(e -> data.set("name", "FETCH_ERROR_" + uuid)).ok();
        data.set("onlineTime", 0L);
    }

    public UserRecord(UUID uuid, TCN data) {
        this.uuid = uuid;
        this.data = data;
        getName().ok();
    }

	public Task<String> getName() {
        return t((res, rej) -> {
            if(data.getLong("lastNameFetch") < System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000) {
                NameFetcher.fetch(uuid).then(x -> {
                    data.set("name", x);
                    data.set("lastNameFetch", System.currentTimeMillis());
                }).err(e -> data.set("name", "FETCH_ERROR_" + uuid)).ok().await();
            }
            res.call(data.getString("name"));
        });
	}

    public Task<ServiceRecord> getServices() {
        return t((res, rej) -> {
            
        });
    }
}
