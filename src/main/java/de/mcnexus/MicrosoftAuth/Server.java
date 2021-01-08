package de.mcnexus.MicrosoftAuth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.mcnexus.MicrosoftAuth.websocket.LauncherWebsocket;
import kong.unirest.Unirest;

import java.io.*;
import java.util.Properties;

import static spark.Spark.*;

public class Server {

    public static String clientID;
    public static String clientSecret;

    public static int port = 80;

    private final LauncherWebsocket websocket = new LauncherWebsocket();

    public static void main(String[] args) {
        try (InputStream input = new FileInputStream("./client")) {

            Properties prop = new Properties();
            prop.load(input);

            clientID = prop.getProperty("id");
            clientSecret = prop.getProperty("secret");
            port = prop.getProperty("port") != null ? Integer.parseInt(prop.getProperty("port")) : 80;

            if(clientID == null || clientSecret == null){

                System.out.println("Keine Clientinfos gefunden");
                return;
            }
            new Server();

        } catch (IOException io) {
            System.out.println("Datei 'client' konnte nicht geladen werden!");
            io.printStackTrace();
        }

    }

    public Server() {
        port(port);
        registerRoutes();

    }

    private void registerRoutes() {

        webSocket("/", LauncherWebsocket.class);
        staticFiles.location("static");

        init();
        get("/redirect", (req, res) -> {
            if (req.queryParams("state") == null || req.queryParams("code") == null || req.queryParams("error") != null) {
                if(req.queryParams("state") != null){
                    LauncherWebsocket.denyReceived(req.queryParams("state"));
                }
                res.redirect("/waiting/index.html");
                return "Error";
            }
            LauncherWebsocket.authReceived(req.queryParams("state"), req.queryParams("code"));
            res.redirect("/waiting/index.html");
            return "Success";
            //https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id=3530b541-1564-4c3d-bb2f-407c1b0e0e5d&response_type=code&redirect_uri=http%3A%2F%2Flocalhost%2Fredirect&scope=XboxLive.signin%20offline_access&state=test
        });

        post("/refresh", "application/json", (req, res) -> {
            JsonObject object = (JsonObject) JsonParser.parseString(req.body());
            if (object.has("refresh_token")) {
                return Unirest.post("https://login.live.com/oauth20_token.srf")
                        .field("client_id", Server.clientID)
                        .field("client_secret", Server.clientSecret)
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
}
