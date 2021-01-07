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

    private LauncherWebsocket websocket = new LauncherWebsocket();

    public static void main(String[] args) {
        if(System.getenv("SECRET") != null)
            clientSecret = System.getenv("SECRET");
        if(System.getenv("ID") != null)
            clientID = System.getenv("ID");

        if(clientID != null || clientSecret != null){
            new Server();
            return;
        }
        try (InputStream input = new FileInputStream("./client")) {

            Properties prop = new Properties();
            prop.load(input);

            clientID = prop.getProperty("id");
            clientSecret = prop.getProperty("secret");

            if(System.getenv("SECRET") != null)
                clientSecret = System.getenv("SECRET");
            if(System.getenv("ID") != null)
                clientID = System.getenv("ID");

            if(clientID == null || clientSecret == null){

                System.out.println("Keine Clientinfos gefunden");
                return;
            }
            new Server();

        } catch (IOException io) {
            System.out.println("Datei 'client' konnte nicht geladen werden!");
            io.printStackTrace();
            return;
        }

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

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 80; //return default port if heroku-port isn't set (i.e. on localhost)
    }

}
