package de.tudbut.tudbutapiv3.data;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import de.tudbut.async.Async;
import de.tudbut.async.Task;
import de.tudbut.io.StreamReader;
import tudbut.parsing.AsyncJSON;
import tudbut.parsing.TCN;

public class NameFetcher {

    public static Task<String> fetch(UUID uuid) {
        return Async.
            <TCN>t((res, rej) -> {
                try {
                    URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
                    StreamReader reader = new StreamReader(url.openStream());
                    AsyncJSON.read(reader.readAllAsString()).err(e -> rej.call(e)).then(res).ok();
                } catch (IOException e) {
                    rej.call(e);
                }
            })
            .compose((resp, res, rej) -> {
                res.call(resp.getString("name"));
            });
    }

    public static class NameNotFound extends Exception { }
}
