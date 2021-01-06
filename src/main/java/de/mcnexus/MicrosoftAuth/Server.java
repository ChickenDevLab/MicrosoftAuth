package de.mcnexus.MicrosoftAuth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.mcnexus.MicrosoftAuth.websocket.LauncherWebsocket;
import de.mcnexus.MicrosoftAuth.websocket.MessageBuilder;
import kong.unirest.Unirest;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static spark.Spark.*;

public class Server {
    public static final String clientID = "3530b541-1564-4c3d-bb2f-407c1b0e0e5d";
    public static final String clientSecret = ".5lHApsUF9A3omc~8eFAD.6Kqu5cE.1h-d";

    private LauncherWebsocket websocket = new LauncherWebsocket();

    public static void main(String[] args) {
        new Server();
    }

    public Server() {
        port(getHerokuAssignedPort());
        registerRoutes();

    }

    private void registerRoutes() {

        webSocket("/", LauncherWebsocket.class);

        init();
        get("/redirect", (req, res) -> {
            if (req.queryParams("state") == null || req.queryParams("code") == null) {
                return "Error";
            }
            websocket.authReceived(req.queryParams("state"), req.queryParams("code"));
            return "Success";
            //https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id=3530b541-1564-4c3d-bb2f-407c1b0e0e5d&response_type=code&redirect_uri=http%3A%2F%2Flocalhost%2Fredirect&scope=XboxLive.signin%20offline_access&state=test
        });

        post("/refresh", "application/json", (req, res) -> {
            JsonObject object = (JsonObject) JsonParser.parseString(req.body());
            if (object.has("refresh_token")) {
                return Unirest.post("https://login.live.com/oauth20_token.srf")
                        .field("client_id", Server.clientID)
                        .field("client_secret", "refresh_code")
                        .field("redirect_uri", "http://localhost/redirect")
                        .field("grant_type", "refresh_token")
                        .field("refresh_token", object.get("refresh_token").getAsString())
                        .contentType("application/x-www-form-urlencoded")
                        .asJson()
                        .getBody()
                        .toString();


            }
            return null;
        });

    }

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 80; //return default port if heroku-port isn't set (i.e. on localhost)
    }

}
