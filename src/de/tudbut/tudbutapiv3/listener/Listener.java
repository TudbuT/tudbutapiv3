package de.tudbut.tudbutapiv3.listener;

import java.util.HashMap;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.tudbut.async.Callback;
import de.tudbut.tools.Hasher;
import de.tudbut.tryumph.events.GET;
import de.tudbut.tryumph.events.PBody;
import de.tudbut.tryumph.events.POST;
import de.tudbut.tryumph.events.PPathFragment;
import de.tudbut.tryumph.events.Path;
import de.tudbut.tryumph.events.RequestHandler;
import de.tudbut.tryumph.server.HTMLParsing;
import de.tudbut.tryumph.server.Header;
import de.tudbut.tryumph.server.Request;
import de.tudbut.tryumph.server.Response;
import de.tudbut.tudbutapiv3.data.Database;
import de.tudbut.tudbutapiv3.data.ServiceData;
import de.tudbut.tudbutapiv3.data.ServiceRecord;
import de.tudbut.tudbutapiv3.data.UserRecord;
import tudbut.parsing.JSON;
import tudbut.parsing.TCN;
import tudbut.tools.encryption.RawKey;

public class Listener implements RequestHandler.Listener {

    @GET
    @Path("/style.css")
    public void style(Request request, Callback<Response> res, Callback<Throwable> rej) {
        res.call(new Response(request, request.context.file("style.css"), 200, "OK", "text/css"));
    }

    @GET
    @Path("/")
    public void onIndex(Request request, Callback<Response> res, Callback<Throwable> rej) {
        Response r = new Response(request, request.context.file("index.html"), 200, "OK");
        Document html = r.getHTML();
        Node node = html.createTextNode(request.fingerPrint());
        HTMLParsing.getElementById(html, "fingerprint").appendChild(node);
        r.updateHTMLData();
        res.call(r);
    }

    Response redirect(Request req, String path) {
        Response r = new Response(req, "Redirecting...", 302, "Moved Temporarily");
        r.headers.put("Location", new Header("Location", path, new HashMap<>()));
        return r;
    }

    @POST
    @Path("/api/service/[a-z]+/use")
    public Response useService(
            Request request, 
            @PPathFragment(3) String service, 
            @PBody("uuid") String uuid, 
            @PBody("name") String name
    ) {
        TCN tcn = new TCN();
        tcn.set("found", false);
        tcn.set("updated", false);
        if(Database.serviceExists(service)) {
            UserRecord user = Database.getUser(uuid, name);
            tcn.set("found", true);
            if(user != null) {
                ServiceRecord record = user.service(Database.service(service)).ok().await();
                record.use();
                tcn.set("service", record.data);
                tcn.set("user", record.parent.data);
                tcn.set("updated", true);
            }
        }
        return new Response(request, JSON.write(tcn), 200, "OK", "application/json");
    }

    @POST
    @Path("/api/service/[a-z]+/create")
    public Response createService(
            Request request,
            @PPathFragment(3) String service,
            @PBody("pass") String password,
            @PBody("servicePass") String servicePassword
    ) {
        TCN tcn = new TCN();
        tcn.set("passwordMatches", false);
        tcn.set("created", false);
        if(Database.data.getString("password").equals(Hasher.sha512hex(Hasher.sha512hex(password)))) { 
            tcn.set("passwordMatches", true);
            if(!Database.serviceExists(service)) {
                tcn.set("created", true);
                Database.makeService(service, servicePassword);
            }
        }
        return new Response(request, JSON.write(tcn), 200, "OK", "application/json");
    }

    @GET
    @Path("/api/service/[a-z]+")
    public Response getService(
            Request request,
            @PPathFragment(3) String service
    ) {
        TCN tcn = new TCN();
        tcn.set("found", false);
        if(Database.serviceExists(service)) {
            tcn.set("found", true);
            tcn.set("service", Database.service(service).data);
        }
        return new Response(request, JSON.write(tcn), 200, "OK", "application/json");
    }

    @Path("/api/user/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    public Response getUser(
            Request request,
            @PPathFragment(3) String user
    ) {
        TCN tcn = new TCN();
        tcn.set("found", false);
        UserRecord record = Database.getUser(UUID.fromString(user), request.method.equals("POST"));
        if(record != null) {
            tcn.set("found", true);
            tcn.set("user", record.data);
            tcn.set("uuid", record.uuid.toString());
        }
        return new Response(request, JSON.write(tcn), 200, "OK", "application/json");
    }

    @Path("/api/user/[a-zA-Z_0-9]+")
    public Response getUserByName(
            Request request,
            @PPathFragment(3) String user
    ) {
        TCN tcn = new TCN();
        tcn.set("found", false);
        UserRecord record = Database.getUser(null, user, request.method.equals("POST"));
        if(record != null) {
            tcn.set("found", true);
            tcn.set("user", record.data);
            tcn.set("uuid", record.uuid.toString());
        }
        return new Response(request, JSON.write(tcn), 200, "OK", "application/json");
    }

    @POST
    @Path("/api/service/[a-z]+/login")
    public Response login(
            Request request,
            @PPathFragment(3) String service,
            @PBody("uuid") String uuid,
            @PBody("name") String name
    ) {
        TCN tcn = new TCN();
        tcn.set("found", false);
        tcn.set("foundService", false);
        if(Database.serviceExists(service)) {
            tcn.set("foundService", true);
            UserRecord user = Database.getUser(uuid, name);
            if(user != null) {
                tcn.set("found", true);
                RawKey key = user.service(Database.service(service)).ok().await().login();
                tcn.set("key", key.toString());
                tcn.set("token", key.toHashString());
                tcn.set("user", user.data);
                
            }
        }
        return new Response(request, JSON.write(tcn), 200, "OK", "application/json");
    }

    // To decrypt a message: parseJSON(key.decryptString(decodeB64(messageString))))
    @POST
    @Path("/api/service/[a-z]+/message")
    public Response message(
            Request request,
            @PPathFragment(3) String service,
            @PBody("uuidOther") String ouuid,
            @PBody("nameOther") String oname,
            @PBody("uuid") String uuid,
            @PBody("name") String name,
            @PBody("token") String keyHash,
            @PBody("message") String message
    ) {
        TCN tcn = new TCN();
        tcn.set("found", false);
        tcn.set("accessGranted", false);
        tcn.set("foundYou", false);
        tcn.set("foundService", false);
        if(Database.serviceExists(service)) {
            tcn.set("foundService", true);
            UserRecord user = Database.getUser(uuid, name);
            if(user != null) {
                tcn.set("foundYou", true);
                ServiceRecord record = user.service(Database.service(service)).ok().await();
                if(record.decryptKey().toHashString().equals(keyHash)) {
                    tcn.set("accessGranted", true);
                    UserRecord otherUser = Database.getUser(ouuid, oname, false);
                    if(otherUser != null) {
                        tcn.set("found", true);
                        ServiceRecord otherRecord = otherUser.service(Database.service(service)).ok().await();
                        TCN msg = new TCN();
                        msg.set("fromUUID", user.uuid.toString());
                        msg.set("from", user.data);
                        msg.set("content", message);
                        otherRecord.message(msg);
                    }
                }
            }
        }
        return new Response(request, JSON.write(tcn), 200, "OK", "application/json");
    }

    @POST
    @Path("/api/service/[a-z]+/message/read")
    public Response messageRead(
            Request request,
            @PPathFragment(3) String service,
            @PBody("uuid") String uuid,
            @PBody("name") String name,
            @PBody("token") String keyHash
    ) {
        TCN tcn = new TCN();
        tcn.set("found", false);
        tcn.set("accessGranted", false);
        tcn.set("foundService", false);
        if(Database.serviceExists(service)) {
            tcn.set("foundService", true);
            UserRecord user = Database.getUser(uuid, name);
            if(user != null) {
                tcn.set("found", true);
                ServiceRecord record = user.service(Database.service(service)).ok().await();
                if(record.decryptKey().toHashString().equals(keyHash)) {
                    tcn.set("accessGranted", true);
                    tcn.set("messages", record.data.getArray("messages").clone());
                    record.data.getArray("messages").clear();
                }
            }
        }
        return new Response(request, JSON.write(tcn), 200, "OK", "application/json");
    }

    @GET
    @Path("/api/service/[a-z]+/usetime")
    public Response getUsetime(
            Request request,
            @PPathFragment(3) String service
    ) {
        ServiceData data = Database.service(service);
        if(data == null) {
            return new Response(request, "Service not found", 400, "Service does not exist", "text/txt");
        }
        return new Response(request, String.valueOf(data.data.getLong("useTime")), 200, "OK", "text/txt");
    }

    @GET
    @Path("/api/service/[a-z]+/usetime/seconds")
    public Response getUsetimeSecs(
            Request request,
            @PPathFragment(3) String service
    ) {
        ServiceData data = Database.service(service);
        if(data == null) {
            return new Response(request, "Service not found", 400, "Service does not exist", "text/txt");
        }
        return new Response(request, String.valueOf(data.data.getLong("useTime") / 1000), 200, "OK", "text/txt");
    }

	@Override
	public void handleError(Request request, Throwable err, Callback<Response> res, Callback<Throwable> rej) {
        TCN error = new TCN();
        err.printStackTrace();
        error.set("errorType", err.getClass().getName());
        res.call(new Response(request, JSON.write(error), 500, "Internal Server Error", "application/json"));
	}

}
