package de.mcnexus.MicrosoftAuth;

import de.mcnexus.MicrosoftAuth.websocket.LauncherWebsocket;

import static spark.Spark.*;

public class Server {
    public static final String clientID = "3530b541-1564-4c3d-bb2f-407c1b0e0e5d";
    public static final String clientSecret = ".5lHApsUF9A3omc~8eFAD.6Kqu5cE.1h-d";

    private LauncherWebsocket websocket = new LauncherWebsocket();

    public static void main(String[] args){
        new Server(80);
    }

    public Server(int port){
        port(port);
        registerRoutes();

    }

    private void registerRoutes() {

        webSocket("/flow", LauncherWebsocket.class);
        get("/redirect", (req, res) -> {
            if(req.queryParams("state") == null || req.queryParams("code") == null) {
                res.redirect("https://mc-nexus.de");
                return null;
            }
            websocket.authReceived(req.queryParams("state"), req.queryParams("code"));
            res.redirect("https://mc-nexus.de");
            return null;
            //https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id=3530b541-1564-4c3d-bb2f-407c1b0e0e5d&response_type=code&redirect_uri=http%3A%2F%2Flocalhost%2Fredirect&scope=XboxLive.signin%20offline_access&state=test
        });

    }

}
