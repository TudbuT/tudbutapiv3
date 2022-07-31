package de.tudbut.tudbutapiv3.listener;

import java.util.UUID;

import de.tudbut.async.Callback;
import de.tudbut.tryumph.events.PBody;
import de.tudbut.tryumph.events.POST;
import de.tudbut.tryumph.events.Path;
import de.tudbut.tryumph.events.RequestHandler;
import de.tudbut.tryumph.server.Request;
import de.tudbut.tryumph.server.Response;
import de.tudbut.tudbutapiv3.auth.AuthManager;
import de.tudbut.tudbutapiv3.auth.GameAuthentication;
import de.tudbut.tudbutapiv3.data.Database;
import de.tudbut.tudbutapiv3.data.UserRecord;
import tudbut.parsing.JSON;
import tudbut.parsing.TCN;

public class GameAuthenticationListener implements RequestHandler.Listener {

    @POST
    @Path("/api/auth/game/start")
    public Response startGameAuth(
            Request request,
            @PBody("uuid") String uuid,
            @PBody("name") String name
    ) {
        TCN tcn = new TCN();
        tcn.set("found", false);
        UserRecord user = Database.getUser(uuid, name);
        if(user != null) {
            GameAuthentication auth = GameAuthentication.get(UUID.fromString(uuid), name);
            tcn.set("found", true);
            tcn.set("serverToJoin", auth.getServerToJoin());
        }
        return new Response(request, JSON.write(tcn), 200, "OK", "application/json");
    }

    @POST
    @Path("/api/auth/game/check")
    public Response checkGameAuth(
            Request request,
            @PBody("uuid") String uuid
    ) {
        TCN tcn = new TCN();
        tcn.set("found", false);
        tcn.set("success", false);
        UserRecord user = Database.getUser(UUID.fromString(uuid));
        if(user != null) {
            GameAuthentication auth = GameAuthentication.get(UUID.fromString(uuid), null);
            if(auth != null) {
                tcn.set("found", true);
                boolean b = auth.isAuthenticated();
                tcn.set("success", b);
                if(b) {
                    tcn.set("token", AuthManager.create(UUID.fromString(uuid)).token);
                }
            }
        }
        return new Response(request, JSON.write(tcn), 200, "OK", "application/json");
    }

    @Override
    public void handleError(Request request, Throwable err, Callback<Response> res, Callback<Throwable> rej) {
        TCN error = new TCN();
        err.printStackTrace();
        error.set("errorType", err.getClass().getName());
        res.call(new Response(request, JSON.write(error), 500, "Internal Server Error", "application/json"));
    }

    
}
