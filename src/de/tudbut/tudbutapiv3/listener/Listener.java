package de.tudbut.tudbutapiv3.listener;

import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.tudbut.async.Callback;
import de.tudbut.tryumph.events.GET;
import de.tudbut.tryumph.events.POST;
import de.tudbut.tryumph.events.Path;
import de.tudbut.tryumph.server.HTMLParsing;
import de.tudbut.tryumph.server.Header;
import de.tudbut.tryumph.server.Request;
import de.tudbut.tryumph.server.Response;
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

}
