package de.tudbut.tudbutapiv3.listener;

import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.tudbut.async.Callback;
import de.tudbut.tools.Hasher;
import de.tudbut.tryumph.events.GET;
import de.tudbut.tryumph.events.PBody;
import de.tudbut.tryumph.events.POST;
import de.tudbut.tryumph.events.PPathFragment;
import de.tudbut.tryumph.events.PQuery;
import de.tudbut.tryumph.events.Path;
import de.tudbut.tryumph.server.HTMLParsing;
import de.tudbut.tryumph.server.Header;
import de.tudbut.tryumph.server.Request;
import de.tudbut.tryumph.server.Response;
import de.tudbut.tudbutapiv3.data.Database;
import de.tudbut.tudbutapiv3.data.ServiceRecord;
import de.tudbut.tudbutapiv3.data.UserRecord;
import tudbut.parsing.JSON;
import tudbut.parsing.TCN;

public class Listener {

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

    private Response redirect(Request req, String path) {
        Response r = new Response(req, "Redirecting...", 302, "Moved Temporarily");
        r.headers.put("Location", new Header("Location", path, new HashMap<>()));
        return r;
    }

    @POST
    @Path("/")
    public void onSubmit(Request req, Callback<Response> res, Callback<Throwable> rej) {
        TCN body = req.bodyURLEncoded();
        req.context.data.set(body.getString("name"), body.getString("value"));
        req.context.save();
        res.call(redirect(req, "/"));
    }

    @POST
    @Path("/api/service/[a-z]+/use")
    public Response onUse(
            Request request, 
            @PPathFragment(3) String service, 
            @PBody("uuid") String uuid, 
            @PBody("name") String name
    ) {
        TCN tcn = new TCN();
        tcn.set("foundService", false);
        tcn.set("updated", false);
        if(Database.serviceExists(service)) {
            UserRecord user = Database.getUser(uuid, name);
            tcn.set("foundService", true);
            if(user != null) {
                ServiceRecord record = user.service(Database.service(service)).ok().await();
                record.use();
                tcn.set("serviceRecord", record.data);
                tcn.set("updated", true);
            }
        }
        return new Response(request, JSON.write(tcn), 200, "OK", "application/json");
    }

    @POST
    @Path("/api/service/[a-z]+/create")
    public Response onCreate(
            Request request,
            @PPathFragment(3) String service,
            @PBody("pass") String password
    ) {
        TCN tcn = new TCN();
        tcn.set("passwordMatches", false);
        tcn.set("created", false);
        if(Database.data.getString("password").equals(Hasher.sha512hex(Hasher.sha512hex(password)))) { 
            tcn.set("passwordMatches", true);
            if(!Database.serviceExists(service)) {
                tcn.set("created", true);
                Database.makeService(service);
            }
        }
        return new Response(request, JSON.write(tcn), 200, "OK", "application/json");
    }

}
