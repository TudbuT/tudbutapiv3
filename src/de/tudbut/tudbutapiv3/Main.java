package de.tudbut.tudbutapiv3;

import java.net.Socket;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.tudbut.async.ComposeCallback;
import de.tudbut.async.TaskCallable;
import de.tudbut.tryumph.config.IRequestCatcher;
import de.tudbut.tryumph.events.EventListener;
import de.tudbut.tryumph.server.Request;
import de.tudbut.tryumph.server.Response;
import de.tudbut.tudbutapiv3.data.Database;
import de.tudbut.tudbutapiv3.listener.Listener;
import tudbut.logger.Logger;

public class Main implements IRequestCatcher {

    EventListener listener = new EventListener(new Listener());
	public static Logger logger = new Logger("TudbuT API v3");
    
    public Main() {
        System.setOut(logger.infoAsStream());
        System.setErr(logger.warnAsStream());
        Database.initiailize();
    }

    @Override
    public TaskCallable<ComposeCallback<Request, Response>> onConnect(Socket arg0) {
        return (tres, trej) -> tres.call((req, res, rej) -> {
            listener.handle(req, r -> {
                if(r.isHTML) {
                    Document html = r.getHTML();
                    Element element;
                    element = html.createElement("meta");
                    element.setAttribute("name", "viewport");
                    element.setAttribute("content", "width=device-width height=device-height");
                    html.getElementsByTagName("head").item(0).appendChild(element);
                    element = html.createElement("link");
                    element.setAttribute("rel", "stylesheet");
                    element.setAttribute("href", "/style.css");
                    html.getElementsByTagName("head").item(0).appendChild(element);
                    r.updateHTMLData();
                }
                res.call(r);
            }, rej);
            if(!req.hasResponse()) {
                res.call(new Response(req, "<h1>Error: 404 Not found " + req.realPath + "</h1>", 404, "Not Found"));
            }

        });
    }

}
