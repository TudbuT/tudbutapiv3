package de.tudbut.tudbutapiv3.data;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import de.tudbut.async.Async;
import de.tudbut.async.Task;
import de.tudbut.io.StreamReader;
import tudbut.parsing.AsyncJSON;
import tudbut.parsing.TCN;
import tudbut.parsing.TCNArray;

public class NameFetcher {

    public static Task<String> fetch(UUID uuid) {
        return Async.
            <TCN>t((res, rej) -> {
                try {
                    URL url = new URL("https://api.mojang.com/user/profiles/" + uuid + "/names");
                    StreamReader reader = new StreamReader(url.openStream());
                    AsyncJSON.read(reader.readAllAsString()).err(e -> rej.call(e)).then(res).ok();
                } catch (IOException e) {
                    rej.call(e);
                }
            })
            .compose((resp, res, rej) -> {
                TCNArray arr = TCNArray.fromTCN(resp);
                if(arr.size() == 0)
                    rej.call(new NameNotFound());
                res.call(arr.getSub(arr.size() - 1).getString("name"));
            });
    }

    public static class NameNotFound extends Exception { }
}
